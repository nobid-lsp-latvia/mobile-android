// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.webfeature.di

import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.commonfeature.features.biometric.BiometricInteractor
import lv.lvrtc.commonfeature.features.settings.SettingsBridge
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingBridge
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingInteractor
import lv.lvrtc.commonfeature.features.user.state.AppStateBridge
import lv.lvrtc.commonfeature.features.wallet.WalletInteractor
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.dashboard.interactor.DashboardInteractor
import lv.lvrtc.dashboard.ui.DashboardBridge
import lv.lvrtc.issuancefeature.ui.IssuanceBridge
import lv.lvrtc.issuancefeature.ui.document.add.AddDocumentInteractor
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.lvrtc.issuancefeature.ui.document.offer.DocumentOfferInteractor
import lv.lvrtc.issuancefeature.ui.success.SuccessInteractor
import lv.lvrtc.presentationfeature.bridge.PresentationBridge
import lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractor
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.signfeature.bridge.SignBridge
import lv.lvrtc.signfeature.interactor.EParakstIdentitiesInteractor
import lv.lvrtc.signfeature.interactor.SignDocumentInteractor
import lv.lvrtc.signfeature.util.FilePickerHelper
import lv.lvrtc.transactionsfeature.TransactionsBridge
import lv.lvrtc.transactionsfeature.ui.TransactionsInteractor
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.uilogic.serializer.UiSerializer
import lv.lvrtc.webbridge.WebBridge
import lv.lvrtc.webbridge.config.WebViewConfig
import lv.lvrtc.webbridge.core.WebBridgeInterface
import lv.lvrtc.webbridge.registry.BridgeProvider
import lv.lvrtc.webbridge.registry.BridgeRegistry
import lv.lvrtc.webfeature.ui.WebViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.webfeature")
class WebFeatureModule {

    @Single
    fun provideBridgeProvider(
        navigationService: WebNavigationService,
        successInteractor: SuccessInteractor,
        resourceProvider: ResourceProvider,
        onboardingInteractor: OnboardingInteractor,
        dashboardInteractor: DashboardInteractor,
        documentDetailsInteractor: DocumentDetailsInteractor,
        addDocumentInteractor: AddDocumentInteractor,
        uiSerializer: UiSerializer,
        biometricInteractor: BiometricInteractor,
        documentOfferInteractor: DocumentOfferInteractor,
        walletCoreDocumentsController: WalletCoreDocumentsController,
        deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
        transactionsInteractor: TransactionsInteractor,
        authService: AuthService,
        onboardingCoordinator: OnboardingCoordinator,
        walletInteractor: WalletInteractor,
        prefKeys: PrefKeys,
        signDocumentInteractor: SignDocumentInteractor,
        filePickerHelper: FilePickerHelper,
        eParakstIdentitiesInteractor: EParakstIdentitiesInteractor,
        paymentPresentationInteractor: PaymentPresentationInteractor
    ): BridgeProvider = object : BridgeProvider {
        override fun provideBridges(): List<WebBridgeInterface> {
            return listOf(
                AppStateBridge(
                    walletCoreDocumentsController,
                ),
                OnboardingBridge(
                    onboardingInteractor,
                    navigationService,
                    authService,
                    onboardingCoordinator,
                    deviceAuthenticationInteractor,
                    prefKeys
                ),
                DashboardBridge(
                    dashboardInteractor,
                    documentDetailsInteractor,
                    navigationService,
                    uiSerializer,
                    documentOfferInteractor,
                    prefKeys
                ),
                IssuanceBridge(
                    addDocumentInteractor,
                    documentOfferInteractor,
                    navigationService,
                    resourceProvider,
                    successInteractor,
                    uiSerializer,
                    eParakstIdentitiesInteractor,
                    prefKeys
                ),
                SignBridge(
                    signDocumentInteractor,
                    filePickerHelper,
                    navigationService,
                    resourceProvider
                ),
                SettingsBridge(biometricInteractor, resourceProvider, navigationService, deviceAuthenticationInteractor, walletInteractor, prefKeys),
                PresentationBridge(
                    navigationService,
                    resourceProvider,
                    uiSerializer,
                    deviceAuthenticationInteractor,
                    walletCoreDocumentsController,
                    documentDetailsInteractor,
                    paymentPresentationInteractor,
                ),
                TransactionsBridge(transactionsInteractor)
            )
        }
    }

    @Single
    fun provideWebViewModel(
        bridge: WebBridge,
        onboardingBridge: OnboardingBridge,
        bridgeRegistry: BridgeRegistry,
        webNavigationService: WebNavigationService,
        webViewConfig: WebViewConfig,
    ) = WebViewModel(bridge, onboardingBridge, bridgeRegistry, webNavigationService, webViewConfig)
}