// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.issuancefeature.ui.document.offer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.controller.auth.BiometricsAvailability
import lv.lvrtc.authlogic.controller.auth.DeviceAuthenticationResult
import lv.lvrtc.authlogic.model.BiometricCrypto
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.businesslogic.util.safeLet
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.commonfeature.features.success.SuccessUIConfig
import lv.lvrtc.corelogic.controller.IssueDocumentsPartialState
import lv.lvrtc.corelogic.controller.ResolveDocumentOfferPartialState
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.extension.documentIdentifier
import lv.lvrtc.corelogic.extension.getIssuerName
import lv.lvrtc.corelogic.extension.getName
import lv.lvrtc.corelogic.model.DocumentIdentifier
import lv.lvrtc.commonfeature.features.request.model.DocumentItemUi
import lv.lvrtc.corelogic.controller.IssueDocumentPartialState
import lv.lvrtc.networklogic.api.wallet.DocumentType
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.networklogic.error.ApiError.UnauthorizedException
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.uilogic.components.AppIcons
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.navigation.CommonScreens
import lv.lvrtc.uilogic.navigation.generateComposableArguments
import lv.lvrtc.uilogic.navigation.generateComposableNavigationLink
import lv.lvrtc.uilogic.serializer.UiSerializer
import lv.lvrtc.resourceslogic.R
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.navigation.WebScreens

sealed class ResolveDocumentOfferInteractorPartialState {
    data class Success(
        val documents: List<DocumentItemUi>,
        val issuerName: String,
        val txCodeLength: Int?
    ) : ResolveDocumentOfferInteractorPartialState()

    data class NoDocument(val issuerName: String) : ResolveDocumentOfferInteractorPartialState()
    data class Failure(val errorMessage: String) : ResolveDocumentOfferInteractorPartialState()
}

sealed class IssueDocumentsInteractorPartialState {
    data class Success(
        val successRoute: String,
    ) : IssueDocumentsInteractorPartialState()

    data class DeferredSuccess(
        val successRoute: String,
    ) : IssueDocumentsInteractorPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentsInteractorPartialState()

    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult
    ) : IssueDocumentsInteractorPartialState()
}

interface DocumentOfferInteractor {
    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState>

    fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String? = null
    ): Flow<IssueDocumentsInteractorPartialState>

    fun issueDocumentByType(documentType: DocumentType): Flow<IssueDocumentsInteractorPartialState>

    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun resumeOpenId4VciWithAuthorization(uri: String)
}

class DocumentOfferInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val authService: AuthService,
    private val walletApiClient: WalletApiClient
) : DocumentOfferInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState> =
        flow {
            walletCoreDocumentsController.resolveDocumentOffer(
                offerUri = offerUri
            ).map { response ->
                when (response) {
                    is ResolveDocumentOfferPartialState.Failure -> {
                        ResolveDocumentOfferInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is ResolveDocumentOfferPartialState.Success -> {
                        val offerHasNoDocuments = response.offer.offeredDocuments.isEmpty()
                        if (offerHasNoDocuments) {
                            ResolveDocumentOfferInteractorPartialState.NoDocument(
                                issuerName = response.offer.getIssuerName(
                                    resourceProvider.getLocale()
                                )
                            )
                        } else {

                            val codeMinLength = 4
                            val codeMaxLength = 6

                            safeLet(
                                response.offer.txCodeSpec?.inputMode,
                                response.offer.txCodeSpec?.length
                            ) { inputMode, length ->

                                if ((length !in codeMinLength..codeMaxLength) || inputMode == TxCodeInputMode.TEXT) {
                                    return@map ResolveDocumentOfferInteractorPartialState.Failure(
                                        errorMessage = resourceProvider.getString(
                                            R.string.issuance_document_offer_error_invalid_txcode_format,
                                            codeMinLength,
                                            codeMaxLength
                                        )
                                    )
                                }
                            }

                            val hasMainPid =
                                walletCoreDocumentsController.getMainPidDocument() != null

                            val hasPidInOffer =
                                response.offer.offeredDocuments.any { offeredDocument ->
                                    val id = offeredDocument.documentIdentifier
                                    // TODO: Re-activate once SD-JWT PID Rule book is in place in ARF.
                                    // id == DocumentIdentifier.MdocPid || id == DocumentIdentifier.SdJwtPid
                                    id == DocumentIdentifier.MdocPid
                                }

                            if (hasMainPid || hasPidInOffer) {

                                ResolveDocumentOfferInteractorPartialState.Success(
                                    documents = response.offer.offeredDocuments.map { offeredDocument ->
                                        DocumentItemUi(
                                            title = offeredDocument.getName(
                                                resourceProvider.getLocale()
                                            ).orEmpty()
                                        )
                                    },
                                    issuerName = response.offer.getIssuerName(resourceProvider.getLocale()),
                                    txCodeLength = response.offer.txCodeSpec?.length
                                )
                            } else {
                                ResolveDocumentOfferInteractorPartialState.Failure(
                                    errorMessage = resourceProvider.getString(
                                        R.string.issuance_document_offer_error_missing_pid_text
                                    )
                                )
                            }
                        }
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            ResolveDocumentOfferInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String?
    ): Flow<IssueDocumentsInteractorPartialState> =
        flow {
            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offerUri,
                txCode = txCode
            ).map { response ->
                when (response) {
                    is IssueDocumentsPartialState.Failure -> {
                        IssueDocumentsInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is IssueDocumentsPartialState.PartialSuccess -> {

                        val nonIssuedDocsNames: String =
                            response.nonIssuedDocuments.entries.map { it.value }.joinToString(
                                separator = ", ",
                                transform = {
                                    it
                                }
                            )

                        IssueDocumentsInteractorPartialState.Success(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFAULT,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_partial_success_subtitle,
                                    issuerName,
                                    nonIssuedDocsNames
                                ),
                                navigation = navigation
                            )
                        )
                    }

                    is IssueDocumentsPartialState.Success -> {
                        IssueDocumentsInteractorPartialState.Success(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFAULT,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_success_subtitle,
                                    issuerName
                                ),
                                navigation = navigation
                            )
                        )
                    }

                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        IssueDocumentsInteractorPartialState.UserAuthRequired(
                            crypto = response.crypto,
                            resultHandler = response.resultHandler
                        )
                    }

                    is IssueDocumentsPartialState.DeferredSuccess -> {
                        IssueDocumentsInteractorPartialState.DeferredSuccess(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFERRED,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_deferred_success_subtitle,
                                    issuerName
                                ),
                                navigation = navigation
                            )
                        )
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            IssueDocumentsInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun issueDocumentByType(documentType: DocumentType): Flow<IssueDocumentsInteractorPartialState> = flow {

        val issuerName = "PMLP"
        val onSuccessNavigation = ConfigNavigation(
            navigationType = NavigationType.PushRoute(
                route = WebScreens.Main.screenRoute
            )
        )

        try {
            val offer = walletApiClient.getDocumentOffer(documentType).fold(
                onSuccess = { it },
                onFailure = { error ->
                    if (error is UnauthorizedException) {
                        coroutineScope {
                            launch {
                                val url = authService.buildEParakstsAuthUrl()
                                // Create intent with NEW_TASK flag since we're outside Activity context
                                val customTabsIntent = CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                resourceProvider.provideContext()?.let { context ->
                                    customTabsIntent.launchUrl(context, Uri.parse(url))
                                }
                            }
                        }
                        return@flow
                    }
                    throw error
                }
            )

            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offer.offerUrl,
                txCode = null
            ).map { response ->
                when (response) {
                    is IssueDocumentsPartialState.Failure -> {
                        IssueDocumentsInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is IssueDocumentsPartialState.PartialSuccess -> {

                        val nonIssuedDocsNames: String =
                            response.nonIssuedDocuments.entries.map { it.value }.joinToString(
                                separator = ", ",
                                transform = {
                                    it
                                }
                            )

                        IssueDocumentsInteractorPartialState.Success(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFAULT,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_partial_success_subtitle,
                                    issuerName,
                                    nonIssuedDocsNames
                                ),
                                navigation = onSuccessNavigation
                            )
                        )
                    }

                    is IssueDocumentsPartialState.Success -> {
                        IssueDocumentsInteractorPartialState.Success(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFAULT,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_success_subtitle,
                                    issuerName
                                ),
                                navigation = onSuccessNavigation
                            )
                        )
                    }

                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        IssueDocumentsInteractorPartialState.UserAuthRequired(
                            crypto = response.crypto,
                            resultHandler = response.resultHandler
                        )
                    }

                    is IssueDocumentsPartialState.DeferredSuccess -> {
                        IssueDocumentsInteractorPartialState.DeferredSuccess(
                            successRoute = buildGenericSuccessRoute(
                                type = IssuanceSuccessType.DEFERRED,
                                subtitle = resourceProvider.getString(
                                    R.string.issuance_document_offer_deferred_success_subtitle,
                                    issuerName
                                ),
                                navigation = onSuccessNavigation
                            )
                        )
                    }
                }
            }.collect {
                emit(it)
            }


        } catch (e: Exception) {
             emit(IssueDocumentsInteractorPartialState.Failure(e.message ?: genericErrorMsg))
        }
    }.safeAsync {
        IssueDocumentsInteractorPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }

    private enum class IssuanceSuccessType {
        DEFAULT, DEFERRED
    }

    private fun buildGenericSuccessRoute(
        type: IssuanceSuccessType,
        subtitle: String,
        navigation: ConfigNavigation
    ): String {
        val successScreenArguments = getSuccessScreenArguments(type, subtitle, navigation)
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = successScreenArguments
        )
    }

    private fun getSuccessScreenArguments(
        type: IssuanceSuccessType,
        subtitle: String,
        navigation: ConfigNavigation
    ): String {
        val (headerConfig, imageConfig, buttonText) = when (type) {
            IssuanceSuccessType.DEFAULT -> Triple(
                first = SuccessUIConfig.HeaderConfig(
                    title = resourceProvider.getString(R.string.issuance_document_offer_success_title),
                ),
                second = SuccessUIConfig.ImageConfig(
                    type = SuccessUIConfig.ImageConfig.Type.DEFAULT,
                    drawableRes = null,
                    contentDescription = resourceProvider.getString(R.string.common_success)
                ),
                third = resourceProvider.getString(R.string.issuance_document_offer_success_primary_button_text)
            )

            IssuanceSuccessType.DEFERRED -> Triple(
                first = SuccessUIConfig.HeaderConfig(
                    title = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_title),
                ),
                second = SuccessUIConfig.ImageConfig(
                    type = SuccessUIConfig.ImageConfig.Type.DRAWABLE,
                    drawableRes = AppIcons.ClockTimer.resourceId,
                    contentDescription = resourceProvider.getString(AppIcons.ClockTimer.contentDescriptionId)
                ),
                third = resourceProvider.getString(R.string.issuance_document_offer_deferred_success_primary_button_text)
            )
        }

        return generateComposableArguments(
            mapOf(
                SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                    SuccessUIConfig(
                        headerConfig = headerConfig,
                        content = subtitle,
                        imageConfig = imageConfig,
                        buttonConfig = listOf(
                            SuccessUIConfig.ButtonConfig(
                                text = buttonText,
                                style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                                navigation = navigation
                            )
                        ),
                        onBackScreenToNavigate = navigation,
                    ),
                    SuccessUIConfig.Parser
                ).orEmpty()
            )
        )
    }
}