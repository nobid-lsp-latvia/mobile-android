// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.startupfeature.di

import lv.lvrtc.authlogic.controller.auth.OnboardingStorageController
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.security.SecurityInteractor
import lv.lvrtc.networklogic.session.SessionManager
import lv.lvrtc.networklogic.session.TokenStorage
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.startupfeature.interactor.SplashInteractor
import lv.lvrtc.startupfeature.interactor.SplashInteractorImpl
import lv.lvrtc.uilogic.serializer.UiSerializer
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.startupfeature")
class StartupModule

@Factory
fun provideSplashInteractor(
    uiSerializer: UiSerializer,
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    onboardingCoordinator: OnboardingCoordinator,
    securityInteractor: SecurityInteractor,
    prefKeys: PrefKeys
): SplashInteractor = SplashInteractorImpl(
    uiSerializer,
    resourceProvider,
    walletCoreDocumentsController,
    onboardingCoordinator,
    securityInteractor,
    prefKeys
)

@Single
fun provideOnboardingCoordinator(
    onboardingStorage: OnboardingStorageController,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    tokenStorage: TokenStorage,
    sessionManager: SessionManager,
    prefKeys: PrefKeys
): OnboardingCoordinator = OnboardingCoordinator(
    walletCoreDocumentsController,
    onboardingStorage,
    tokenStorage,
    sessionManager,
    prefKeys
)