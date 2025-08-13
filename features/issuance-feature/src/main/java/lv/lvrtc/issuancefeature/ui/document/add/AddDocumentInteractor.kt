// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.issuancefeature.ui.document.add

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import eu.europa.ec.eudi.wallet.document.Document
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.controller.auth.BiometricsAvailability
import lv.lvrtc.authlogic.controller.auth.DeviceAuthenticationResult
import lv.lvrtc.authlogic.model.BiometricCrypto
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.commonfeature.features.success.SuccessUIConfig
import lv.lvrtc.corelogic.controller.IssuanceMethod
import lv.lvrtc.corelogic.controller.IssueDocumentPartialState
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.lvrtc.corelogic.controller.AddSampleDataPartialState
import lv.lvrtc.corelogic.controller.FetchScopedDocumentsPartialState
import lv.lvrtc.corelogic.controller.IssueDocumentPartialState.*
import lv.lvrtc.corelogic.controller.IssueDocumentsPartialState
import lv.lvrtc.corelogic.model.DocumentIdentifier
import lv.lvrtc.corelogic.model.toDocumentIdentifier
import lv.lvrtc.issuancefeature.ui.document.add.model.DocumentOptionItemUi
import lv.lvrtc.networklogic.api.wallet.DocumentType
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.networklogic.error.ApiError.UnauthorizedException
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.uilogic.components.AppIcons
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.navigation.CommonScreens
import lv.lvrtc.uilogic.navigation.generateComposableArguments
import lv.lvrtc.uilogic.navigation.generateComposableNavigationLink
import lv.lvrtc.uilogic.serializer.UiSerializer
import lv.lvrtc.resourceslogic.R
import lv.lvrtc.uilogic.navigation.DashboardScreens
import lv.lvrtc.uilogic.navigation.IssuanceScreens
import lv.lvrtc.uilogic.navigation.WebScreens

sealed class AddDocumentInteractorPartialState {
    data class Success(val options: List<DocumentOptionItemUi>) :
        AddDocumentInteractorPartialState()

    data class Failure(val error: String) : AddDocumentInteractorPartialState()
}

interface AddDocumentInteractor {
    fun getAddDocumentOption(flowType: IssuanceFlowUiConfig): Flow<AddDocumentInteractorPartialState>

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String
    ): Flow<IssueDocumentPartialState>

    fun handleUserAuth(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun buildGenericSuccessRouteForDeferred(flowType: IssuanceFlowUiConfig): String

    fun resumeOpenId4VciWithAuthorization(uri: String)

    fun addSampleData(): Flow<AddSampleDataPartialState>
}

class AddDocumentInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val walletApiClient: WalletApiClient,
    private val authService: AuthService,
    ) : AddDocumentInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getAddDocumentOption(flowType: IssuanceFlowUiConfig): Flow<AddDocumentInteractorPartialState> =
        flow {
            when (val state = walletCoreDocumentsController.getScopedDocuments(resourceProvider.getLocale())) {
                is FetchScopedDocumentsPartialState.Failure -> emit(
                    AddDocumentInteractorPartialState.Failure(
                        error = state.errorMessage
                    )
                )

                is FetchScopedDocumentsPartialState.Success -> {
                    val existingDocs = walletCoreDocumentsController.getAllIssuedDocuments()

                    val options = mutableListOf<DocumentOptionItemUi>()

                    options.addAll(state.documents.mapNotNull { scopedDoc ->
                        if (
                            (flowType != IssuanceFlowUiConfig.NO_DOCUMENT || scopedDoc.isPid)
                            && !scopedDoc.configurationId.contains("jwt", ignoreCase = true)
                            && scopedDoc.configurationId != "eu.europa.ec.eudi.iban_mdoc"
                        ) {
                            DocumentOptionItemUi(
                                text = scopedDoc.name,
                                icon = AppIcons.Id,
                                configId = scopedDoc.configurationId,
                                available = true,
                                alreadyHave = existingDocs.any { doc ->
                                    doc.name == scopedDoc.name
                                }
                            )
                        } else null
                    })

                    options.add(DocumentOptionItemUi(
                        text = "eSign",
                        icon = AppIcons.Id,
                        configId = DocumentIdentifier.MdocESign.formatType,
                        available = true,
                        alreadyHave = existingDocs.any { doc ->
                            doc.toDocumentIdentifier() == DocumentIdentifier.MdocESign
                        }
                    ))

                    options.add(DocumentOptionItemUi(
                        text = "eSeal",
                        icon = AppIcons.Id,
                        configId = DocumentIdentifier.MdocESeal.formatType,
                        available = true,
                        // Always false because user can have multiple eSeals
                        alreadyHave = false
                    ))

                    options.add(DocumentOptionItemUi(
                        text = "IBAN",
                        icon = AppIcons.Id,
                        configId = DocumentIdentifier.SdJwtA2Pay.formatType,
                        available = true,
                        alreadyHave = existingDocs.any { doc ->
                            doc.toDocumentIdentifier() == DocumentIdentifier.SdJwtA2Pay
                        }
                    ))

                    emit(AddDocumentInteractorPartialState.Success(options = options))
                }
            }
        }.safeAsync {
            AddDocumentInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun addSampleData(): Flow<AddSampleDataPartialState> =
        walletCoreDocumentsController.addSampleData()

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String
    ): Flow<IssueDocumentPartialState> =
        walletCoreDocumentsController.issueDocument(
            issuanceMethod = issuanceMethod,
            configId = configId
        )

    override fun handleUserAuth(
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

    override fun buildGenericSuccessRouteForDeferred(flowType: IssuanceFlowUiConfig): String {
        val navigation = when (flowType) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> ConfigNavigation(
                navigationType = NavigationType.PushRoute(route = WebScreens.Main.screenRoute),
            )

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> ConfigNavigation(
                navigationType = NavigationType.PopTo(
                    screen = WebScreens.Main
                )
            )
        }
        val successScreenArguments = getSuccessScreenArgumentsForDeferred(navigation)
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = successScreenArguments
        )
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }

    private fun getSuccessScreenArgumentsForDeferred(
        navigation: ConfigNavigation
    ): String {
        val (headerConfig, imageConfig, buttonText) = Triple(
            first = SuccessUIConfig.HeaderConfig(
                title = resourceProvider.getString(R.string.issuance_add_document_deferred_success_title),
            ),
            second = SuccessUIConfig.ImageConfig(
                type = SuccessUIConfig.ImageConfig.Type.DRAWABLE,
                drawableRes = AppIcons.ClockTimer.resourceId,
                contentDescription = resourceProvider.getString(AppIcons.ClockTimer.contentDescriptionId)
            ),
            third = resourceProvider.getString(R.string.issuance_add_document_deferred_success_primary_button)
        )

        return generateComposableArguments(
            mapOf(
                SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                    SuccessUIConfig(
                        headerConfig = headerConfig,
                        content = resourceProvider.getString(R.string.issuance_add_document_deferred_success_subtitle),
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