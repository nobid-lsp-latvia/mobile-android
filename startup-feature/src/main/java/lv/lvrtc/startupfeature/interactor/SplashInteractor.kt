// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.startupfeature.interactor

import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.commonfeature.features.biometric.BiometricUiConfig
import lv.lvrtc.commonfeature.features.biometric.OnBackNavigationConfig
import lv.lvrtc.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.security.SecurityInteractor
import lv.lvrtc.corelogic.security.SecurityValidation
import lv.lvrtc.resourceslogic.R
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.navigation.CommonScreens
import lv.lvrtc.uilogic.navigation.WebScreens
import lv.lvrtc.uilogic.navigation.generateComposableArguments
import lv.lvrtc.uilogic.navigation.generateComposableNavigationLink
import lv.lvrtc.uilogic.serializer.UiSerializer

interface SplashInteractor {
    suspend fun getAfterSplashRoute(): String
}

class SplashInteractorImpl(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val onboardingCoordinator: OnboardingCoordinator,
    private val securityInteractor: SecurityInteractor,
    private val prefKeys: PrefKeys
) : SplashInteractor {

    private val hasDocuments: Boolean
        get() = walletCoreDocumentsController.getAllDocuments().isNotEmpty()

    override suspend fun getAfterSplashRoute(): String {
        when (val validation = securityInteractor.validateDeviceSecurity()) {
            is SecurityValidation.Invalid -> {
                return generateComposableNavigationLink(
                    screen = CommonScreens.SecurityError,
                    arguments = generateComposableArguments(
                        mapOf("reason" to validation.reason.name)
                    )
                )
            }

            SecurityValidation.Valid -> {
                return when {
                    prefKeys.getAppActivated() -> getBiometricsConfig()
                    else -> onboardingCoordinator.getCurrentScreen().screenRoute
                }
            }
        }
    }

    private fun getBiometricsConfig(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments = generateComposableArguments(
                mapOf(
                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                        BiometricUiConfig(
                            title = resourceProvider.getString(R.string.biometric_login_prompt_title),
                            subTitle = resourceProvider.getString(R.string.biometric_login_prompt_subtitle),
                            quickPinOnlySubTitle = resourceProvider.getString(R.string.biometric_login_prompt_quickPinOnlySubTitle),
                            isPreAuthorization = true,
                            shouldInitializeBiometricAuthOnCreate = true,
                            onSuccessNavigation = ConfigNavigation(
                                navigationType = NavigationType.PushScreen(
                                    screen =
                                    if (hasDocuments) {
                                        WebScreens.Main
                                    } else {
                                        WebScreens.AddPid
                                    },
                                    arguments = if (!hasDocuments) {
                                        mapOf("flowType" to IssuanceFlowUiConfig.NO_DOCUMENT.name)
                                    } else {
                                        emptyMap()
                                    }
                                )
                            ),
                            onBackNavigationConfig = OnBackNavigationConfig(
                                onBackNavigation = ConfigNavigation(
                                    navigationType = NavigationType.Finish
                                ),
                                hasToolbarCancelIcon = false
                            )
                        ),
                        BiometricUiConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }
}