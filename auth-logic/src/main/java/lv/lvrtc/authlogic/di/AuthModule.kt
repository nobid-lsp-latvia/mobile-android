// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.di

import lv.lvrtc.authlogic.config.StorageConfig
import lv.lvrtc.authlogic.config.StorageConfigImpl
import lv.lvrtc.authlogic.controller.auth.BiometricAuthController
import lv.lvrtc.authlogic.controller.auth.BiometricAuthControllerImpl
import lv.lvrtc.authlogic.controller.auth.DeviceAuthController
import lv.lvrtc.authlogic.controller.auth.DeviceAuthControllerImpl
import lv.lvrtc.authlogic.controller.auth.OnboardingStorageController
import lv.lvrtc.authlogic.controller.auth.OnboardingStorageControllerImpl
import lv.lvrtc.authlogic.controller.storage.BiometryStorageController
import lv.lvrtc.authlogic.controller.storage.BiometryStorageControllerImpl
import lv.lvrtc.authlogic.controller.storage.PinStorageController
import lv.lvrtc.authlogic.controller.storage.PinStorageControllerImpl
import lv.lvrtc.authlogic.storage.PrefsBiometryStorageProvider
import lv.lvrtc.authlogic.storage.PrefsOnboardingStorageProvider
import lv.lvrtc.authlogic.storage.PrefsPinStorageProvider
import lv.lvrtc.businesslogic.controller.PrefsController
import lv.lvrtc.businesslogic.controller.crypto.CryptoController
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.authlogic")
class AuthModule

@Single
fun provideStorageConfig(
    prefsController: PrefsController
): StorageConfig = StorageConfigImpl(
    pinImpl = PrefsPinStorageProvider(prefsController),
    biometryImpl = PrefsBiometryStorageProvider(prefsController),
    onboardingImpl = PrefsOnboardingStorageProvider(prefsController)
)

@Factory
fun provideBiometricAuthController(
    cryptoController: CryptoController,
    biometryStorageController: BiometryStorageController,
    resourceProvider: ResourceProvider
): BiometricAuthController =
    BiometricAuthControllerImpl(
        resourceProvider,
        cryptoController,
        biometryStorageController
    )

@Factory
fun provideDeviceAuthController(
    resourceProvider: ResourceProvider,
    biometricAuthController: BiometricAuthController
): DeviceAuthController =
    DeviceAuthControllerImpl(
        resourceProvider,
        biometricAuthController
    )

@Factory
fun providePinStorageController(
    storageConfig: StorageConfig
): PinStorageController = PinStorageControllerImpl(storageConfig)

@Factory
fun provideBiometryStorageController(
    storageConfig: StorageConfig
): BiometryStorageController = BiometryStorageControllerImpl(storageConfig)

@Factory
fun provideOnboardingStorageController(
    storageConfig: StorageConfig
) : OnboardingStorageController = OnboardingStorageControllerImpl(storageConfig)