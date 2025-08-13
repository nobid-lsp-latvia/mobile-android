// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.signfeature.bridge

import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.webkit.WebResourceRequest
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import lv.lvrtc.resourceslogic.bridge.SIGN
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.signfeature.BuildConfig
import lv.lvrtc.signfeature.interactor.SignDocumentInteractor
import lv.lvrtc.signfeature.interactor.SignDocumentInteractorPartialState
import lv.lvrtc.signfeature.interactor.SignDownloadInteractorPartialState
import lv.lvrtc.signfeature.interactor.ValidateContainerPartialState
import lv.lvrtc.signfeature.util.FilePickerHelper
import lv.lvrtc.signfeature.util.getMimeTypeFromExtension
import lv.lvrtc.uilogic.navigation.NavigationCommand.ToWeb
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.webbridge.UrlHandler
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.BridgeRequest
import lv.lvrtc.webbridge.core.BridgeResponse
import java.io.File

class SignBridge(
    private val signDocumentInteractor: SignDocumentInteractor,
    private val filePickerHelper: FilePickerHelper,
    private val navigationService: WebNavigationService,
    private val resourceProvider: ResourceProvider
) : BaseBridge(), UrlHandler {

    private var downloadedFile: File? = null
    private var contentType: String? = null
    private var currentSigningRequestId: String? = null
    private var pendingDocumentId: String? = null
    private var pendingIsESeal: Boolean = false

    override fun getName() = SIGN.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            SIGN.PICK_FILES -> handlePickFiles(request)
            SIGN.GET_SIGNING_METHODS -> handleGetSigningMethods(request)
            SIGN.SIGN_DOCUMENT -> handleSignDocument(request)
            SIGN.DOWNLOAD_DOCUMENT -> handleDownloadSignedFile(request)
            SIGN.SHARE_DOCUMENT -> handleShareSignedFile(request)
            SIGN.CLOSE_SESSION -> handleCloseSession(request)
            SIGN.GET_SHARED_FILE -> handleGetSharedFile(request)
            SIGN.OPEN_FILE -> handlePreviewContainerFile(request)
            else -> createErrorResponse(request, "Unknown function")
        }
    }

    private fun handlePickFiles(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            try {
                val files = filePickerHelper.pickFiles()
                processAndValidateFiles(files, request)
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, e.message))
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetSigningMethods(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            signDocumentInteractor.getSigningMethods().collect { methods ->
                emitEvent(createSuccessResponse(request, mapOf("methods" to methods)))
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetSharedFile(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val contentUri = data["filePath"] as? String
            ?: return createErrorResponse(request, "Missing filePath")

        coroutineScope.launch {
            try {
                val fileInfo = filePickerHelper.processFileUri(Uri.parse(contentUri))
                processAndValidateFiles(listOf(fileInfo), request)
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, e.message))
            }
        }
        return createSuccessResponse(request, null)
    }

    private suspend fun processAndValidateFiles(files: List<FilePickerHelper.FileInfo>, request: BridgeRequest) {
        val allowedExtensions = setOf("pdf", "asice", "edoc", "sce")
        val potentialContainerFile = files.firstOrNull { file ->
            file.isValid && allowedExtensions.contains(file.name.substringAfterLast('.').lowercase())
        }

        if (potentialContainerFile != null) {
            signDocumentInteractor.validateContainer(File(potentialContainerFile.path)).collect { state ->
                when (state) {
                    is ValidateContainerPartialState.Success -> {
                        val validFiles = state.files.filter { containerFile ->
                            containerFile.name.contains(".") && containerFile.name.substringAfterLast('.').isNotEmpty()
                        }

                        val isRealContainer = state.signers.isNotEmpty()

                        emitEvent(createSuccessResponse(request, mapOf(
                            "files" to files.map { file ->
                                mapOf(
                                    "path" to file.path,
                                    "name" to file.name,
                                    "size" to file.size,
                                    "type" to file.mimeType,
                                    "isContainer" to (file == potentialContainerFile && isRealContainer),
                                    "isValid" to file.isValid,
                                    "containerInfo" to if (file == potentialContainerFile && isRealContainer) {
                                        mapOf(
                                            "signers" to state.signers.map { signer ->
                                                mapOf(
                                                    "name" to signer.name,
                                                    "signedAt" to signer.signedAt,
                                                    "type" to signer.type
                                                )
                                            },
                                            "files" to validFiles.map { containerFile ->
                                                mapOf(
                                                    "name" to containerFile.name,
                                                    "size" to containerFile.size
                                                )
                                            }
                                        )
                                    } else null
                                )
                            }
                        )))
                    }
                    is ValidateContainerPartialState.Failure -> {
                        emitEvent(createSuccessResponse(request, mapOf(
                            "files" to files.map { file ->
                                mapOf(
                                    "path" to file.path,
                                    "name" to file.name,
                                    "size" to file.size,
                                    "type" to file.mimeType,
                                    "isContainer" to file.isContainer,
                                    "isValid" to file.isValid,
                                    "containerInfo" to null
                                )
                            },
                            "validationError" to state.error
                        )))
                    }
                }
            }
        } else {
            emitEvent(createSuccessResponse(request, mapOf(
                "files" to files.map { file ->
                    mapOf(
                        "path" to file.path,
                        "name" to file.name,
                        "size" to file.size,
                        "type" to file.mimeType,
                        "isContainer" to file.isContainer,
                        "isValid" to file.isValid,
                        "containerInfo" to null
                    )
                }
            )))
        }
    }

    private fun handlePreviewContainerFile(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val containerPath = data["containerPath"] as? String
            ?: return createErrorResponse(request, "Missing containerPath")

        val fileName = data["fileName"] as? String

        coroutineScope.launch {
            try {
                val context = resourceProvider.provideContext()
                val fileToOpen: File

                if (fileName != null) {
                    fileToOpen = filePickerHelper.extractFileFromContainer(containerPath, fileName)
                } else {
                    fileToOpen = File(containerPath)
                    if (!fileToOpen.exists()) {
                        throw IllegalStateException("File not found: $containerPath")
                    }
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    fileToOpen
                )

                val fileExtension = if (fileName != null) {
                    fileName.substringAfterLast('.')
                } else {
                    fileToOpen.name.substringAfterLast('.')
                }

                val mimeType = getMimeTypeFromExtension(fileExtension)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(Intent.createChooser(intent, "Open with")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                emitEvent(createSuccessResponse(request, null))
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, "Failed to open file: ${e.message}"))
            }
        }

        return createSuccessResponse(request, null)
    }

    override fun handleUrl(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        if (url.startsWith("eparakstsid")) {
            val uri = request.url
            val successUrl = uri.getQueryParameter("successurl")
            val failureUrl = uri.getQueryParameter("failureurl")

            val newBuilder = Uri.Builder()
                .scheme(uri.scheme)
                .authority(uri.authority)
                .path(uri.path)

            for (paramName in uri.queryParameterNames) {
                if (paramName != "successurl" && paramName != "failureurl") {
                    for (value in uri.getQueryParameters(paramName)) {
                        newBuilder.appendQueryParameter(paramName, value)
                    }
                }
            }

            newBuilder.appendQueryParameter(
                "successurl",
                "${BuildConfig.DEEPLINK}resume_sign?url=${Uri.encode(successUrl ?: "")}"
            )
            newBuilder.appendQueryParameter(
                "failureurl",
                "${BuildConfig.DEEPLINK}resume_sign?url=${Uri.encode(failureUrl ?: "")}"
            )

            val modifiedUri = newBuilder.build()

            val intent = Intent(Intent.ACTION_VIEW, modifiedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                webView?.context?.startActivity(intent)
            } catch (_: Exception) {}
            return true
        }

        if (url.contains("resume_sign")) {
            val originalUrl = request.url.getQueryParameter("url")
            if (originalUrl != null) {
                webView?.post {
                    webView?.loadUrl(originalUrl)
                }
            }
            return true
        }

        if (url.startsWith(BuildConfig.DEEPLINK)) {
            val type = if (pendingIsESeal) "seal" else "esign"
            if (url.contains("eseal-success")) {
                coroutineScope.launch {
                    pendingDocumentId?.let { docId ->
                        signDocumentInteractor.logSigningTransaction(
                            documentId = docId,
                            isSuccess = true,
                            isESeal = pendingIsESeal
                        )
                        pendingDocumentId = null
                    }
                    navigationService.navigate(ToWeb("sign-done/loading"))

                    currentSigningRequestId?.let { requestId ->
                        signDocumentInteractor.downloadSignedDocument(requestId).collect { state ->
                            when (state) {
                                is SignDownloadInteractorPartialState.Success -> {
                                    downloadedFile = state.file
                                    contentType = state.contentType
                                    navigationService.navigate(
                                        ToWeb("sign-done/success/${type}")
                                    )
                                }
                                is SignDownloadInteractorPartialState.Failure -> {
                                    navigationService.navigate(ToWeb("sign-done/error/${type}"))
                                }
                            }
                        }
                    }
                }
                return true
            } else if (url.contains("eseal-error")) {
                coroutineScope.launch {
                    pendingDocumentId?.let { docId ->
                        signDocumentInteractor.logSigningTransaction(
                            documentId = docId,
                            isSuccess = false,
                            isESeal = pendingIsESeal
                        )
                        pendingDocumentId = null
                    }
                    navigationService.navigate(ToWeb("sign-done/error/${type}"))
                }
                return true
            }
            return true
        }

        return false
    }

    private fun handleSignDocument(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val filePath = data["filePath"] as? String
            ?: return createErrorResponse(request, "Missing filePath")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        val code = data["code"] as? String

        val file = File(filePath)
        if (!file.exists()) {
            return createErrorResponse(request, null)
        }

        coroutineScope.launch {
            navigationService.navigate(ToWeb("sign-done/loading"))

            signDocumentInteractor.signDocument(file, documentId, code).collect { state ->
                when (state) {
                    is SignDocumentInteractorPartialState.Success -> {
                        currentSigningRequestId = state.requestId
                        pendingDocumentId = documentId
                        pendingIsESeal = state.isESeal

                        webView?.post {
                            webView?.loadUrl(state.redirectUrl)
                        }
                    }
                    is SignDocumentInteractorPartialState.Failure -> {
                        navigationService.navigate(ToWeb("sign-done/error"))
                        emitEvent(createErrorResponse(request, state.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleCloseSession(request: BridgeRequest): BridgeResponse {
        return createErrorResponse(request, "Not yet implemented")
    }

    private fun handleDownloadSignedFile(request: BridgeRequest): BridgeResponse {
        downloadedFile?.let { tempFile ->
            val context = resourceProvider.provideContext()
            try {
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "NOBID_EUDIW"
                )
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val destinationFile = File(downloadDir, tempFile.name)

                var uniqueFile = destinationFile
                var counter = 1
                while (uniqueFile.exists()) {
                    val name = tempFile.nameWithoutExtension
                    val ext = tempFile.extension
                    uniqueFile = File(downloadDir, "${name}_${counter}.${ext}")
                    counter++
                }

                tempFile.copyTo(uniqueFile)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(uniqueFile.absolutePath),
                    null,
                    null
                )

                emitEvent(createSuccessResponse(request, null))
                return createSuccessResponse(request, null)
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, null))
                return createErrorResponse(request, e.message)
            }
        }
        return createErrorResponse(request, null)
    }

    private fun handleShareSignedFile(request: BridgeRequest): BridgeResponse {
        downloadedFile?.let { tempFile ->
            val context = resourceProvider.provideContext()
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = contentType ?: "application/edoc"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                emitEvent(createSuccessResponse(request, null))
                return createSuccessResponse(request, null)
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, null))
                return createErrorResponse(request, e.message)
            }
        }
        return createErrorResponse(request, null)
    }
}