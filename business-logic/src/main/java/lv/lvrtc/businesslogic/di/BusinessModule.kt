// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.businesslogic.di

import android.content.Context
import lv.lvrtc.analyticslogic.provider.CrashlyticsProvider
import lv.lvrtc.businesslogic.config.ConfigLogic
import lv.lvrtc.businesslogic.config.ConfigLogicImpl
import lv.lvrtc.businesslogic.config.EnvironmentConfig
import lv.lvrtc.businesslogic.controller.NetworkStatusController
import lv.lvrtc.businesslogic.controller.NetworkStatusControllerImpl
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.businesslogic.controller.PrefKeysImpl
import lv.lvrtc.businesslogic.controller.PrefsController
import lv.lvrtc.businesslogic.controller.PrefsControllerImpl
import lv.lvrtc.businesslogic.controller.crypto.CryptoController
import lv.lvrtc.businesslogic.controller.crypto.CryptoControllerImpl
import lv.lvrtc.businesslogic.controller.crypto.KeystoreController
import lv.lvrtc.businesslogic.controller.crypto.KeystoreControllerImpl
import lv.lvrtc.businesslogic.controller.log.LogController
import lv.lvrtc.businesslogic.controller.log.LogControllerImpl
import lv.lvrtc.businesslogic.validator.FormValidator
import lv.lvrtc.businesslogic.validator.FormValidatorImpl
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.businesslogic")
class BusinessModule

@Single
fun providePrefsController(context: Context): PrefsController =
    PrefsControllerImpl(context)

@Single
fun providePrefKeys(prefsController: PrefsController): PrefKeys =
    PrefKeysImpl(prefsController)

@Single
fun provideKeystoreController(
    prefKeys: PrefKeys,
): KeystoreController =
    KeystoreControllerImpl(prefKeys)

@Factory
fun provideCryptoController(keystoreController: KeystoreController): CryptoController =
    CryptoControllerImpl(keystoreController)

@Single
fun provideConfigLogic(): ConfigLogic = ConfigLogicImpl()

@Single
fun provideLogController(context: Context, configLogic: ConfigLogic, crashlyticsProvider: CrashlyticsProvider): LogController =
    LogControllerImpl(context, configLogic, crashlyticsProvider)

@Single
fun provideNetworkStatusController(): NetworkStatusController {
    return NetworkStatusControllerImpl()
}

@Factory
fun provideFormValidator(logController: LogController): FormValidator =
    FormValidatorImpl(logController)

@Single
fun provideEnvironmentConfig(configLogic: ConfigLogic): EnvironmentConfig =
    configLogic.environmentConfig