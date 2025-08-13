// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.dashboard.ui

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.commonfeature.config.PresentationMode
import lv.lvrtc.commonfeature.config.RequestUriConfig
import lv.lvrtc.commonfeature.features.PresentationConfigStore
import lv.lvrtc.commonfeature.features.document_details.model.DocumentDetailsUi
import lv.lvrtc.commonfeature.features.issuance.OfferConfigRepository
import lv.lvrtc.commonfeature.features.offer.OfferUiConfig
import lv.lvrtc.corelogic.di.getOrCreatePresentationScope
import lv.lvrtc.dashboard.interactor.DashboardInteractor
import lv.lvrtc.dashboard.interactor.DashboardInteractorGetDocumentsPartialState
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractorDeleteBookmarkPartialState
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractorDeleteDocumentPartialState
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractorPartialState
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractorStoreBookmarkPartialState
import lv.lvrtc.issuancefeature.ui.document.offer.DocumentOfferInteractor
import lv.lvrtc.resourceslogic.bridge.DASHBOARD
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.navigation.DashboardScreens
import lv.lvrtc.uilogic.navigation.DeepLinkType
import lv.lvrtc.uilogic.navigation.NavigationCommand
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.uilogic.navigation.hasDeepLink
import lv.lvrtc.uilogic.serializer.UiSerializer
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.BridgeRequest
import lv.lvrtc.webbridge.core.BridgeResponse

class DashboardBridge(
    private val dashboardInteractor: DashboardInteractor,
    private val documentDetailsInteractor: DocumentDetailsInteractor,
    private val navigationService: WebNavigationService,
    private val uiSerializer: UiSerializer,
    private val documentOfferInteractor: DocumentOfferInteractor,
    private val prefKeys: PrefKeys
) : BaseBridge() {

    override fun getName() = DASHBOARD.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        prefKeys.setAppActivated(true)

        return when(request.function) {
            DASHBOARD.GET_DOCUMENTS -> handleGetDocuments(request)
            DASHBOARD.GET_DOCUMENT_DETAILS -> handleGetDocumentDetails(request)
            DASHBOARD.DELETE_DOCUMENT -> handleDeleteDocument(request)
            DASHBOARD.SET_DOCUMENT_FAVORITE -> handleSetDocumentFavorite(request)
            else -> BridgeResponse(
                id = request.id,
                status = BridgeResponse.Status.ERROR,
                error = "Unknown function"
            )
        }
    }

    fun handlePendingDeepLink(uri: Uri) {
        hasDeepLink(uri)?.let { deepLink ->
            when (deepLink.type) {
                DeepLinkType.SIGN_DOCUMENT -> {
                    coroutineScope.launch {
                        val state = dashboardInteractor.getDocuments().first()
                        when (state) {
                            is DashboardInteractorGetDocumentsPartialState.Success -> {
                                if (state.documentsUi.isNotEmpty()) {
                                    val filePath = uri.getQueryParameter("filePath") ?: uri.toString()
                                    val encodedPath = Uri.encode(filePath)
                                    navigationService.navigate(
                                        NavigationCommand.ToWeb("sign/$encodedPath/null")
                                    )
                                } else {
                                    navigationService.navigate(
                                        NavigationCommand.ToWeb("onboarding")
                                    )
                                }
                            }
                            else -> {
                                Log.d("DashboardBridge", "Failed to get documents for deep link handling")
                            }
                        }
                    }
                }
                DeepLinkType.CREDENTIAL_OFFER -> {
                    val offerConfig = OfferUiConfig(
                        offerURI = uri.toString(),
                        onSuccessNavigation = ConfigNavigation(
                            navigationType = NavigationType.PopTo(
                                screen = DashboardScreens.Dashboard
                            )
                        ),
                        onCancelNavigation = ConfigNavigation(
                            navigationType = NavigationType.Pop
                        )
                    )

                    OfferConfigRepository.setOfferConfig(offerConfig)

                    coroutineScope.launch {
                        navigationService.navigate(
                            NavigationCommand.ToWeb("document-offer")
                        )
                    }
                }
                DeepLinkType.OPENID4VP -> {
                    try {
                        getOrCreatePresentationScope().close()
                    } catch (_: Exception) {}

                    PresentationConfigStore.clear()
                    getOrCreatePresentationScope()
                    PresentationConfigStore.setConfig(
                        RequestUriConfig(
                            PresentationMode.OpenId4Vp(
                                uri = uri.toString(),
                                initiatorRoute = DashboardScreens.Dashboard.screenRoute
                            )
                        )
                    )

                    coroutineScope.launch {
                        navigationService.navigate(
                            NavigationCommand.ToWeb("document-presentation")
                        )
                    }
                }
                else -> {
                    Log.d("DashboardBridge", "Unsupported deep link type: ${deepLink.type}")
                }
            }
        }
    }

    private fun handleGetDocuments(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            val result = dashboardInteractor.getDocuments().first()
            val response = when (result) {
                is DashboardInteractorGetDocumentsPartialState.Success -> {
                    val documents = result.documentsUi.map { doc ->
                        mapOf(
                            "meta" to mapOf(
                                "id" to doc.documentId,
                                "issuanceState" to doc.documentIssuanceState.name,
                                "type" to doc.documentName,
                                "hasExpired" to doc.documentHasExpired,
                                "isFavorite" to doc.documentIsBookmarked,
                                "expirationDate" to doc.documentExpirationDate,
                                "issuingAuthority" to doc.issuingAuthority,
                                "issuerCountry" to doc.issuerCountry,
                                "issuanceDate" to doc.issuanceDate,
                                "displayNumber" to doc.displayNumber,
                                "description" to doc.description,
                                "additionalInfo" to doc.additionalInfo
                            ),
                        )
                    }

                    BridgeResponse(
                        id = request.id,
                        status = BridgeResponse.Status.SUCCESS,
                        data = mapOf("documents" to documents)
                    )
                }
                is DashboardInteractorGetDocumentsPartialState.Failure -> BridgeResponse(
                    id = request.id,
                    status = BridgeResponse.Status.ERROR,
                    error = result.error
                )
            }
            emitEvent(response)
        }

        return BridgeResponse(
            id = request.id,
            status = BridgeResponse.Status.SUCCESS
        )
    }

    private fun handleGetDocumentDetails(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        coroutineScope.launch {
            documentDetailsInteractor.getDocumentDetails(documentId).collect { state ->
                when (state) {
                    is DocumentDetailsInteractorPartialState.Success -> {
                        val document = state.documentUi

                        fun convertUrlSafeToStandard(base64: String): String {
                            return base64.replace('-', '+').replace('_', '/')
                        }

                        val image = convertUrlSafeToStandard(document.documentImage)

                        val responseData = mapOf(
                            "meta" to mapOf(
                                "id" to document.documentId,
                                "type" to document.documentName, // docType
                                "issuanceState" to document.documentIssuanceState.name,
                                "hasExpired" to document.documentHasExpired,
                                "isFavorite" to state.isBookmarked,
                                "expirationDate" to document.documentExpirationDate,
                                "issuingAuthority" to document.issuingAuthority, // issuerId
                                "issuingCountry" to document.issuerCountry,
                                "issuanceDate" to document.issuanceDate,
                                "displayNumber" to document.displayNumber,
                                "description" to document.description,
                                "additionalInfo" to document.additionalInfo,
                            ),
                            "documentImage" to image,
                            "documentDetails" to document.documentDetails.mapNotNull { detail ->
                                when (detail) {
                                    is DocumentDetailsUi.DefaultItem -> mapOf(
                                        "type" to "default",
                                        "identifier" to detail.itemData.identifier,
                                        "label" to detail.itemData.title,
                                        "value" to detail.itemData.infoValues?.firstOrNull()
                                    )

                                    is DocumentDetailsUi.SignatureItem -> mapOf(
                                        "type" to "signature",
                                        "identifier" to detail.itemData.identifier,
                                        "label" to detail.itemData.title,
                                        "image" to detail.itemData.base64Image
                                    )

                                    else -> null
                                }
                            }
                        )

                        emitEvent(createSuccessResponse(request,responseData))
                    }
                    is DocumentDetailsInteractorPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleDeleteDocument(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        coroutineScope.launch {
            documentDetailsInteractor.deleteDocument(documentId).collect { state ->
                when (state) {
                    is DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted -> {
                        emitEvent(createSuccessResponse(request, mapOf("status" to "deleted")))
                    }
                    is DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted -> {
                        emitEvent(createSuccessResponse(request, mapOf("status" to "all_deleted")))
                    }
                    is DocumentDetailsInteractorDeleteDocumentPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.errorMessage))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleSetDocumentFavorite(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        val isFavorite = data["isFavorite"] as? Boolean
            ?: return createErrorResponse(request, "Missing isFavorite")

        coroutineScope.launch {
            if (isFavorite) {
                documentDetailsInteractor.storeBookmark(documentId).collect { result ->
                    when (result) {
                        is DocumentDetailsInteractorStoreBookmarkPartialState.Success -> {
                            emitEvent(createSuccessResponse(request, null))
                        }
                        is DocumentDetailsInteractorStoreBookmarkPartialState.Failure -> {
                            emitEvent(createErrorResponse(request, "Failed to store bookmark"))
                        }
                    }
                }
            } else {
                documentDetailsInteractor.deleteBookmark(documentId).collect { result ->
                    when (result) {
                        is DocumentDetailsInteractorDeleteBookmarkPartialState.Success -> {
                            emitEvent(createSuccessResponse(request, null))
                        }
                        is DocumentDetailsInteractorDeleteBookmarkPartialState.Failure -> {
                            emitEvent(createErrorResponse(request, "Failed to delete bookmark"))
                        }
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }
}