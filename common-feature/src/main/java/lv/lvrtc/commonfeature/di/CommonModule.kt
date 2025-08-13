// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.di

import lv.lvrtc.authlogic.controller.auth.BiometricAuthController
import lv.lvrtc.authlogic.controller.auth.DeviceAuthController
import lv.lvrtc.authlogic.controller.storage.BiometryStorageController
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.businesslogic.controller.PrefsController
import lv.lvrtc.businesslogic.controller.crypto.SecureAreaRepository
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractorImpl
import lv.lvrtc.commonfeature.features.biometric.BiometricInteractor
import lv.lvrtc.commonfeature.features.biometric.BiometricInteractorImpl
import lv.lvrtc.commonfeature.features.qr_scan.QrScanInteractor
import lv.lvrtc.commonfeature.features.qr_scan.QrScanInteractorImpl
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingBridge
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingInteractor
import lv.lvrtc.commonfeature.features.wallet.WalletInteractor
import lv.lvrtc.commonfeature.features.wallet.WalletInteractorImpl
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.security.SecurityInteractor
import lv.lvrtc.corelogic.security.SecurityInteractorImpl
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.config.StorageConfig
import lv.lvrtc.storagelogic.service.RealmService
import lv.lvrtc.uilogic.navigation.WebNavigationService
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.lvrtc.commonfeature")
class CommonModule

@Factory
fun provideBiometricInteractor(
    biometryStorageController: BiometryStorageController,
    biometricAuthenticationController: BiometricAuthController,
): BiometricInteractor {
    return BiometricInteractorImpl(
        biometryStorageController,
        biometricAuthenticationController
    )
}

@Factory
fun provideWalletInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    prefsController: PrefsController,
    storageConfig: StorageConfig,
    realmService: RealmService,
    secureAreaRepository: SecureAreaRepository
) : WalletInteractor {
    return WalletInteractorImpl(walletCoreDocumentsController, prefsController, storageConfig, realmService, secureAreaRepository)
}

@Factory
fun provideDeviceAuthenticationInteractor(
    deviceAuthenticationController: DeviceAuthController,
    resourceProvider: ResourceProvider,
    walletApiClient: WalletApiClient,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    authService: AuthService
): DeviceAuthenticationInteractor {
    return DeviceAuthenticationInteractorImpl(deviceAuthenticationController, walletApiClient, walletCoreDocumentsController, authService, resourceProvider)
}

@Factory
fun provideQrScanInteractor(): QrScanInteractor {
    return QrScanInteractorImpl()
}

@Factory
fun provideOnboardingBridge(
    onboardingInteractor: OnboardingInteractor,
    navigationService: WebNavigationService,
    authService: AuthService,
    onboardingCoordinator: OnboardingCoordinator,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    prefKeys: PrefKeys
): OnboardingBridge {
    return OnboardingBridge(
        onboardingInteractor,
        navigationService,
        authService,
        onboardingCoordinator,
        deviceAuthenticationInteractor,
        prefKeys
    )
}

@Factory
fun provideSecurityInteractor(
    resourceProvider: ResourceProvider
): SecurityInteractor = SecurityInteractorImpl(
    resourceProvider
)