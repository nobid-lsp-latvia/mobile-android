// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.presentationfeature.bridge

import android.annotation.SuppressLint
import androidx.core.app.ComponentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.controller.auth.DeviceAuthenticationResult
import lv.lvrtc.commonfeature.features.PresentationConfigStore
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.commonfeature.features.qr_scan.QrScanFlow
import lv.lvrtc.commonfeature.features.qr_scan.QrScanUiConfig
import lv.lvrtc.commonfeature.features.request.transformer.PresentationDocument
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.controller.WalletCorePresentationController
import lv.lvrtc.corelogic.di.getOrCreatePresentationScope
import lv.lvrtc.corelogic.model.AuthenticationData
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractorPartialState
import lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractor
import lv.lvrtc.presentationfeature.interactor.PaymentStatusState
import lv.lvrtc.presentationfeature.interactor.PresentationLoadingInteractor
import lv.lvrtc.presentationfeature.interactor.PresentationLoadingObserveResponsePartialState
import lv.lvrtc.presentationfeature.interactor.PresentationLoadingSendRequestedDocumentPartialState
import lv.lvrtc.presentationfeature.interactor.PresentationRequestInteractor
import lv.lvrtc.presentationfeature.interactor.PresentationRequestInteractorPartialState
import lv.lvrtc.resourceslogic.bridge.PRESENTATION
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.uilogic.navigation.CommonScreens
import lv.lvrtc.uilogic.navigation.NavigationCommand.ToNative
import lv.lvrtc.uilogic.navigation.NavigationCommand.ToWeb
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.uilogic.navigation.generateComposableArguments
import lv.lvrtc.uilogic.navigation.generateComposableNavigationLink
import lv.lvrtc.uilogic.serializer.UiSerializer
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.BridgeRequest
import lv.lvrtc.webbridge.core.BridgeResponse
import lv.lvrtc.resourceslogic.R
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import lv.lvrtc.storagelogic.model.Transaction
import lv.lvrtc.storagelogic.model.TransactionType
import lv.lvrtc.uilogic.navigation.NavigationCommand.ToExternal

class PresentationBridge(
    private val navigationService: WebNavigationService,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val addDocumentInteractor: DocumentDetailsInteractor,
    private val paymentInteractor: PaymentPresentationInteractor
) : BaseBridge() {


    private val presentationScope get() = getOrCreatePresentationScope()
    private val presentationRequestInteractor get() = presentationScope.get<PresentationRequestInteractor>()
    private val presentationLoadingInteractor get() = presentationScope.get<PresentationLoadingInteractor>()

    private var currentDocuments: List<PresentationDocument> = emptyList()

    override fun getName() = PRESENTATION.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when(request.function) {
            PRESENTATION.GET_REQUEST_DOCUMENTS -> {
                getOrCreatePresentationScope()
                val config = PresentationConfigStore.getConfig()
                if (config == null) {
                    emitEvent(createErrorResponse(request, "No presentation config found"))
                    return createErrorResponse(request, "No presentation config found")
                }
                presentationRequestInteractor.setConfig(config)
                handleGetRequestDocuments(request)
            }
            PRESENTATION.CANCEL_REQUEST -> {
                cleanupPresentation()
                emitEvent(createSuccessResponse(request, null))
                createSuccessResponse(request, null)
            }
            PRESENTATION.CONFIRM_REQUEST -> handleConfirmRequest(request)
            PRESENTATION.START_PRESENTATION -> handleStartPresentation(request)
            else -> createErrorResponse(request, "Unknown function")
        }
    }

    private fun updateCurrentDocuments(documents: List<PresentationDocument>) {
        currentDocuments = documents
    }

    private fun handleStartPresentation(request: BridgeRequest): BridgeResponse {
        emitEvent(createSuccessResponse(request, null))
        coroutineScope.launch {
            navigationService.navigate(
                ToNative(
                    generateComposableNavigationLink(
                        CommonScreens.QrScan,
                        generateComposableArguments(
                            mapOf(
                                QrScanUiConfig.serializedKeyName to uiSerializer.toBase64(
                                    QrScanUiConfig(
                                        title = "Present Documents",
                                        subTitle = "Scan verifier's QR code",
                                        qrScanFlow = QrScanFlow.Presentation
                                    ),
                                    QrScanUiConfig.Parser
                                )
                            )
                        )
                    )
                )
            )
        }
        return createSuccessResponse(request, null)
    }

    private fun handleConfirmRequest(request: BridgeRequest): BridgeResponse {

        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid data")

        val fields = data["fields"] as? Map<*, *>
            ?: return createErrorResponse(request, "Missing fields data")

        val updatedItems = currentDocuments.map { doc ->
            doc.copy(
                claims = doc.claims.map { claim ->
                    val checkedStatusFromRequest = fields[claim.id] as? Boolean

                    // Only update 'isChecked' if the field was present in the request
                    // AND the claim is actually toggleable by the user (isEnabled).
                    // Required fields (!isEnabled) should retain their original state (presumably checked=true).
                    if (checkedStatusFromRequest != null && claim.isEnabled) {
                        claim.copy(isChecked = checkedStatusFromRequest)
                    } else {
                        // Keep the original claim state if it wasn't in the request
                        // or if it's a required field that shouldn't be changed.
                        claim
                    }
                }
            )
        }

        updateCurrentDocuments(updatedItems)

        coroutineScope.launch {
            presentationRequestInteractor.updateRequestedDocuments(currentDocuments)

            presentationLoadingInteractor.observeResponse().collect { state ->
                when (state) {
                    is PresentationLoadingObserveResponsePartialState.UserAuthenticationRequired -> {
                        handleUserAuthentication(request, state.authenticationData, true)
                    }

                    is PresentationLoadingObserveResponsePartialState.RequestReadyToBeSent -> {
                        navigationService.navigate(ToWeb(PRESENTATION.SCREENS.PRESENTATION_LOADING))

                        when (val result = presentationLoadingInteractor.sendRequestedDocuments()) {
                            is PresentationLoadingSendRequestedDocumentPartialState.Success -> {
                                // Continue with flow
                            }
                            is PresentationLoadingSendRequestedDocumentPartialState.Failure -> {
                                emitEvent(createErrorResponse(request, result.error))
                                cleanupPresentation()
                            }
                        }
                    }

                    is PresentationLoadingObserveResponsePartialState.Failure -> {
                        cleanupPresentation()
                        emitEvent(createErrorResponse(request, state.error))
                    }
                    is PresentationLoadingObserveResponsePartialState.Success -> {
                        cleanupPresentation()
                        emitEvent(createSuccessResponse(request, null))
                    }
                    is PresentationLoadingObserveResponsePartialState.Redirect -> {
                        val redirectUri = state.uri.toString()
                        val paymentStatusUri = redirectUri.replace("/complete/", "/status/")

                        navigationService.navigate(ToWeb("payment-done/loading"))
                        handlePaymentFlow(paymentStatusUri, redirectUri)
                    }
                }
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetRequestDocuments(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            try {
                presentationRequestInteractor.getRequestDocuments().collect { response ->
                    when (response) {
                        is PresentationRequestInteractorPartialState.Success -> {
                            updateCurrentDocuments(response.requestDocuments)

                            val documentsForJson = response.requestDocuments.map { doc ->

                                val fieldsForJson = doc.claims.map { claim ->
                                    mapOf(
                                        "id" to claim.id,
                                        "readableName" to claim.displayTitle,
                                        "value" to claim.value,
                                        "checked" to claim.isChecked,
                                        "enabled" to claim.isEnabled,
                                        "elementIdentifier" to claim.elementIdentifier,
                                        "isRequired" to claim.intentToRetain,
//                                        "intentToRetain" to claim.intentToRetain
                                    )
                                }

                                var meta: Map<String, Any> = emptyMap()
                                val storedDocument =
                                    walletCoreDocumentsController.getAllIssuedDocuments()
                                        .firstOrNull { it.name == doc.docName }

                                addDocumentInteractor.getDocumentDetails(storedDocument!!.id)
                                    .collect { state ->
                                        when (state) {
                                            is DocumentDetailsInteractorPartialState.Success -> {
                                                val document = state.documentUi
                                                meta = mapOf(
                                                    "id" to document.documentId,
                                                    "type" to document.documentName,
                                                    "issuanceState" to document.documentIssuanceState.name,
                                                    "hasExpired" to document.documentHasExpired,
                                                    "isFavorite" to state.isBookmarked,
                                                    "expirationDate" to document.documentExpirationDate,
                                                    "issuingAuthority" to document.issuingAuthority,
                                                    "issuingCountry" to document.issuerCountry,
                                                    "issuanceDate" to document.issuanceDate,
                                                    "displayNumber" to document.displayNumber,
                                                    "description" to document.description,
                                                    "additionalInfo" to document.additionalInfo
                                                ) as Map<String, Any>
                                            }

                                            else -> {}
                                        }
                                    }

                                mapOf(
                                    "title" to doc.docName,
                                    "meta" to meta,
                                    "fields" to fieldsForJson
                                )
                            }

                            val responseData = mapOf(
                                "verifierName" to response.verifierName,
                                "verifierIsTrusted" to response.verifierIsTrusted,
                                "documents" to documentsForJson,
                                "transactionData" to response.paymentDetails?.let { paymentDetails ->
                                    mapOf(
                                        "paymentId" to paymentDetails.paymentId,
                                        "amount" to paymentDetails.amount,
                                        "currency" to paymentDetails.currency,
                                        "creditor" to paymentDetails.creditor,
                                        "purpose" to paymentDetails.purpose
                                    )
                                }
                            )

                            emitEvent(createSuccessResponse(request, responseData))
                        }

                        is PresentationRequestInteractorPartialState.NoData -> {
                            emitEvent(
                                createSuccessResponse(
                                    request, mapOf(
                                        "verifierName" to response.verifierName,
                                        "verifierIsTrusted" to response.verifierIsTrusted,
                                        "documents" to emptyList<Map<String, Any>>()
                                    )
                                )
                            )
                        }

                        is PresentationRequestInteractorPartialState.Disconnect -> {
                            cleanupPresentation()
                            emitEvent(createErrorResponse(request, "disconnected"))
                        }

                        is PresentationRequestInteractorPartialState.Failure -> {
                            cleanupPresentation()
                            emitEvent(createErrorResponse(request, response.error))
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is org.koin.core.error.ScopeNotCreatedException &&
                    e !is org.koin.core.error.InstanceCreationException) {
                    try {
                        cleanupPresentation()
                    } catch (_: Exception) {}
                }

                emitEvent(createErrorResponse(request, e.message ?: "Unknown error"))
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handlePaymentFlow(paymentStatusUri: String, redirectUri: String) {
        coroutineScope.launch {
            var finalStatusReached = false

            paymentInteractor.pollPaymentStatus(paymentStatusUri).collect { state ->
                when (state) {
                    is PaymentStatusState.Loading -> emitPaymentStatusToWeb("LOADING")
                    is PaymentStatusState.Status -> {
                        emitPaymentStatusToWeb(state.status)
                        if (state.status == "ACSC") {
                            finalStatusReached = true
                            delay(2000)
                            logPaymentTransaction(true)
                            cleanupPresentation()
                            navigationService.navigate(ToExternal(redirectUri))
                        } else if (state.status in listOf("NAUT", "RJCT", "CANC")) {
                            finalStatusReached = true
                            delay(2000)
                            logPaymentTransaction(false)
                            cleanupPresentation()
                            navigationService.navigate(ToExternal(redirectUri))
                        }
                    }
                    is PaymentStatusState.Error -> {
                        emitPaymentStatusToWeb("ERROR")
                        finalStatusReached = true
                        logPaymentTransaction(false)
                    }
                    is PaymentStatusState.Completed -> finalStatusReached = true
                    is PaymentStatusState.Timeout -> {
                        emitPaymentStatusToWeb("TIMEOUT")
                        finalStatusReached = true
                        logPaymentTransaction(false)
                        cleanupPresentation()
                        navigationService.navigate(ToExternal(redirectUri))
                    }
                }
            }
            if (!finalStatusReached) {
                cleanupPresentation()
                navigationService.navigate(ToExternal(redirectUri))
            }
        }
    }

    private suspend fun logPaymentTransaction(isSuccess: Boolean) {
        val scope = getOrCreatePresentationScope()
        val walletCorePresentationController = scope.get<WalletCorePresentationController>()
        val transactionStorageController = scope.get<TransactionStorageController>()
        val status = if (isSuccess) "SUCCESS" else "ERROR"
        val eventType = TransactionType.DOCUMENT_PAYMENT.name

        walletCorePresentationController.disclosedDocuments?.forEach { document ->
            transactionStorageController.store(
                Transaction(
                    documentId = document.documentId,
                    docType = "IBAN",
                    nameSpace = "IBAN",
                    status = status,
                    eventType = eventType,
                    authority = walletCorePresentationController.verifierName
                )
            )
        }
    }

    private fun emitPaymentStatusToWeb(status: String) {
        webView?.post {
            webView?.evaluateJavascript(
                """
            window.dispatchEvent(new CustomEvent('lx-payment-status', {
                detail: { status: "$status" }
            }));
            """.trimIndent(),
                null
            )
        }
    }

    private fun cleanupPresentation() {
        try {
            val scope = getOrCreatePresentationScope()
            if (!scope.closed) {
                val interactor = scope.getOrNull<PresentationRequestInteractor>()
                interactor?.stopPresentation()
            }
        } catch (_: org.koin.core.error.ClosedScopeException) {} catch (_: Exception) {}

        PresentationConfigStore.clear()

        try {
            getOrCreatePresentationScope().close()
        } catch (_: Exception) {}

        currentDocuments = emptyList()
    }

    @SuppressLint("RestrictedApi")
    private fun handleUserAuthentication(
        request: BridgeRequest,
        authDataList: List<AuthenticationData>,
        notifyOnAuthFailure: Boolean,
        index: Int = 0
    ) {
        if (index >= authDataList.size) {
            presentationLoadingInteractor.sendRequestedDocuments()
            emitEvent(createSuccessResponse(request, null))
            return
        }

        val authData = authDataList[index]
        val isFinalAuthentication = index == authDataList.lastIndex
        val activity = webView?.context as? ComponentActivity

        activity?.let {
            deviceAuthenticationInteractor.authenticateWithBiometrics(
                context = it,
                crypto = authData.crypto,
                notifyOnAuthenticationFailure = notifyOnAuthFailure,
                resultHandler = DeviceAuthenticationResult(
                    onAuthenticationSuccess = {
                        authData.onAuthenticationSuccess()
                        if (isFinalAuthentication) {
                            presentationLoadingInteractor.sendRequestedDocuments()
                            emitEvent(createSuccessResponse(request, null))
                        } else {
                            // Add small delay before next auth prompt
                            coroutineScope.launch {
                                delay(500)
                                handleUserAuthentication(
                                    request,
                                    authDataList,
                                    notifyOnAuthFailure,
                                    index + 1
                                )
                            }
                        }
                    },
                    onAuthenticationError = {
                        emitEvent(createErrorResponse(request, "authentication_error"))
                    },
                    onAuthenticationFailure = {
                        if (notifyOnAuthFailure) {
                            emitEvent(createErrorResponse(
                                request,
                                resourceProvider.getString(R.string.biometric_failed_warning)
                            ))
                        }
                    }
                )
            )
        }
    }
}