// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.signfeature.interactor

import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.commonfeature.features.document_details.transformer.DocumentDetailsTransformer
import lv.lvrtc.commonfeature.util.extractValueFromDocumentOrEmpty
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.model.DocumentIdentifier
import lv.lvrtc.corelogic.model.toDocumentIdentifier
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.networklogic.model.wallet.SignDocumentRequest
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.signfeature.BuildConfig
import lv.lvrtc.signfeature.util.isPDF
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import lv.lvrtc.storagelogic.model.Transaction
import lv.lvrtc.storagelogic.model.TransactionType
import java.io.File
import java.util.UUID

data class SignerInfo(
    val name: String,
    val signedAt: String,
    val type: String
)

data class ContainerFile(
    val name: String,
    val size: Long? = null
)

sealed class SignDocumentInteractorPartialState {
    data class Success(val redirectUrl: String, val requestId: String, val isESeal: Boolean) : SignDocumentInteractorPartialState()
    data class Failure(val error: String) : SignDocumentInteractorPartialState()
}

sealed class SignDownloadInteractorPartialState {
    data class Success(
        val file: File,
        val contentType: String
    ) : SignDownloadInteractorPartialState()
    data class Failure(val error: String) : SignDownloadInteractorPartialState()
}

sealed class ValidateContainerPartialState {
    data class Success(
        val signers: List<SignerInfo>,
        val files: List<ContainerFile>
    ) : ValidateContainerPartialState()
    data class Failure(val error: String) : ValidateContainerPartialState()
}

interface SignDocumentInteractor {
    fun signDocument(file: File, documentId: String, code: String? = null): Flow<SignDocumentInteractorPartialState>
    fun downloadSignedDocument(requestId: String): Flow<SignDownloadInteractorPartialState>
    fun closeSession(requestId: String, isESeal: Boolean): Flow<Unit>
    fun getSigningMethods(): Flow<List<Map<String, Any>>>
    fun validateContainer(file: File): Flow<ValidateContainerPartialState>
    suspend fun logSigningTransaction(documentId: String, isSuccess: Boolean, isESeal: Boolean)
}

class SignDocumentInteractorImpl(
    private val walletApiClient: WalletApiClient,
    private val resourceProvider: ResourceProvider,
    private val authService: AuthService,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val transactionStorageController: TransactionStorageController
) : SignDocumentInteractor {

    private val genericErrorMsg get() = resourceProvider.genericErrorMessage()

    override fun signDocument(file: File, documentId: String, code: String?): Flow<SignDocumentInteractorPartialState> = flow {
        val requestId = UUID.randomUUID().toString()
        val deepLinkScheme = BuildConfig.EPARAKSTS_REDIRECT_URI

        // If documentId is "eparaksts", it means we're doing direct signing without a stored document
        val directSigning = documentId == "eparaksts"

        val (userId, sid, isESeal) = if (directSigning) {
            // For direct signing:
            // - code parameter contains the userId
            // - no sid needed (direct signing is always eSign)
            // - isESeal is false (direct signing is always eSign)
            Triple(code, null, false)
        } else {
            val doc = walletCoreDocumentsController.getDocumentById(documentId) as? IssuedDocument
            if (doc == null) {
                emit(SignDocumentInteractorPartialState.Failure("Document not found"))
                return@flow
            }

            val docType = doc.toDocumentIdentifier()
            Triple(
                extractValueFromDocumentOrEmpty(doc, "userId"),
                if (docType == DocumentIdentifier.MdocESeal) extractValueFromDocumentOrEmpty(doc, "sid") else null,
                docType == DocumentIdentifier.MdocESeal
            )
        }

        val asice = !isPDF(file.name)

        val request = SignDocumentRequest(
            requestId = requestId,
            asice = asice,
            fileName = file.name,
//            userId = userId,
            redirectUrl = "$deepLinkScheme://eseal-success",
            redirectError = "$deepLinkScheme://eseal-error",
            esealSid = if (isESeal) sid else null
        )

        val result = if (isESeal) {
            walletApiClient.sealDocument(request, file)
        } else {
            walletApiClient.signDocument(request, file)
        }

        result.fold(
            onSuccess = { response ->
                emit(SignDocumentInteractorPartialState.Success(
                    redirectUrl = response.redirectUrl,
                    requestId = requestId,
                    isESeal = isESeal
                ))
            },
            onFailure = { error ->
                logSigningTransaction(documentId, false, isESeal)

                emit(SignDocumentInteractorPartialState.Failure(
                    error = error.localizedMessage ?: genericErrorMsg
                ))
            }
        )
    }

    override fun validateContainer(file: File): Flow<ValidateContainerPartialState> = flow {
        walletApiClient.validateContainer(file).fold(
            onSuccess = { response ->
                val signers = response.validationResponses.firstOrNull()?.data?.signaturesExt?.map { signature ->
                    val signatureType = when (signature.signatureLevel) {
                        "QESIG", "ADESIG_QC", "ADESIG" -> "eSign"
                        "QESEAL", "ADESEAL_QC", "ADESEAL" -> "eSeal"
                        else -> "eSign"
                    }
                    SignerInfo(
                        name = signature.signedBy,
                        signedAt = signature.info.bestSignatureTime,
                        type = signatureType
                    )
                } ?: emptyList()

                val files = response.validationResponses.firstOrNull()?.data?.includedFiles?.map { file ->
                    ContainerFile(name = file.filename)
                } ?: emptyList()

                emit(ValidateContainerPartialState.Success(signers, files))
            },
            onFailure = { error ->
                emit(ValidateContainerPartialState.Failure(
                    error = error.localizedMessage ?: genericErrorMsg
                ))
            }
        )
    }.safeAsync {
        ValidateContainerPartialState.Failure(it.localizedMessage ?: genericErrorMsg)
    }

    override fun downloadSignedDocument(requestId: String): Flow<SignDownloadInteractorPartialState> = flow {
        walletApiClient.downloadDocument(requestId).fold(
            onSuccess = { response ->
                val context = resourceProvider.provideContext()

                val contentDisposition = response.headers()["content-disposition"]

                var originalFilename = if (contentDisposition != null) {
                    val pattern = """filename=([^;]+)""".toRegex()
                    pattern.find(contentDisposition)?.groupValues?.get(1)?.trim('"', ' ')
                } else {
                    null
                } ?: "signed_document.edoc"

                val uuidPattern = """[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}_(.+)""".toRegex()
                uuidPattern.find(originalFilename)?.let {
                    originalFilename = it.groupValues[1]
                }

                val cacheDir = File(context.cacheDir, "sign_temp")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val tempFile = File(cacheDir, originalFilename)

                response.body()?.byteStream()?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val contentType = response.body()?.contentType()?.toString() ?: "application/edoc"

                emit(SignDownloadInteractorPartialState.Success(tempFile, contentType))
            },
            onFailure = { error ->
                emit(SignDownloadInteractorPartialState.Failure(
                    error = error.localizedMessage ?: genericErrorMsg
                ))
            }
        )
    }.safeAsync {
        SignDownloadInteractorPartialState.Failure(it.localizedMessage ?: genericErrorMsg)
    }

    override fun getSigningMethods(): Flow<List<Map<String, Any>>> = flow {
        val signingDocs = walletCoreDocumentsController.getAllDocumentsByType(
            listOf(
                DocumentIdentifier.MdocESeal,
                DocumentIdentifier.MdocESign
            )
        )

        val methods = signingDocs.mapNotNull { doc ->
            val documentUi = DocumentDetailsTransformer.transformToUiItem(
                document = doc,
                resourceProvider = resourceProvider
            ) ?: return@mapNotNull null

            mapOf(
                "meta" to mapOf(
                    "id" to documentUi.documentId,
                    "issuanceState" to documentUi.documentIssuanceState.name,
                    "type" to documentUi.documentName,
                    "hasExpired" to documentUi.documentHasExpired,
                    "isFavorite" to documentUi.documentIsBookmarked,
                    "expirationDate" to documentUi.documentExpirationDate,
                    "issuingAuthority" to documentUi.issuingAuthority,
                    "issuerCountry" to documentUi.issuerCountry,
                    "issuanceDate" to documentUi.issuanceDate,
                    "displayNumber" to documentUi.displayNumber,
                    "description" to documentUi.description,
                    "additionalInfo" to documentUi.additionalInfo
                )
            )
        }

        emit(methods)
    }

    override fun closeSession(requestId: String, isESeal: Boolean): Flow<Unit> = flow {
        val type = if (isESeal) "eseal" else "sign"

        walletApiClient.closeSigningSession(type, requestId)

        emit(Unit)
    }.safeAsync {
    // { No-op }
    }

    override suspend fun logSigningTransaction(
        documentId: String,
        isSuccess: Boolean,
        isESeal: Boolean
    ) {
        val docType = if (isESeal) "MdocESeal" else "MdocESign"
        val nameSpace = if (isESeal) "eSeal" else "eSign"
        val status = if (isSuccess) "SUCCESS" else "ERROR"

        transactionStorageController.store(
            Transaction(
                documentId = documentId,
                docType = docType,
                nameSpace = nameSpace,
                status = status,
                eventType = TransactionType.DOCUMENT_SIGNED.name,
            )
        )
    }
}