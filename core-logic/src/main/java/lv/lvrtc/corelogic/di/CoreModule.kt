// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.di

import android.content.Context
import com.android.identity.android.securearea.AndroidKeystoreSecureArea
import com.android.identity.android.storage.AndroidStorageEngine
import com.android.identity.securearea.SecureArea
import eu.europa.ec.eudi.wallet.EudiWallet
import kotlinx.io.files.Path
import lv.lvrtc.businesslogic.config.EnvironmentConfig
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.businesslogic.controller.crypto.SecureAreaRepository
import lv.lvrtc.businesslogic.controller.log.LogController
import lv.lvrtc.corelogic.config.WalletConfig
import lv.lvrtc.corelogic.config.WalletCoreConfigImpl
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsControllerImpl
import lv.lvrtc.corelogic.controller.WalletCoreLogController
import lv.lvrtc.corelogic.controller.WalletCoreLogControllerImpl
import lv.lvrtc.corelogic.security.SecureAreaController
import lv.lvrtc.corelogic.security.SecureAreaControllerImpl
import lv.lvrtc.corelogic.security.SecureAreaRepositoryImpl
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.BookmarkStorageController
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Single
import org.koin.core.annotation.Singleton
import org.koin.mp.KoinPlatform

const val PRESENTATION_SCOPE_ID = "presentation_scope_id"

@Module
@ComponentScan("lv.lvrtc.corelogic")
class CoreModule

@Single
fun provideEudiWallet(
    context: Context,
    walletCoreConfig: WalletConfig,
    walletCoreLogController: WalletCoreLogController
): EudiWallet = EudiWallet(context, walletCoreConfig.config) {
    withLogger(walletCoreLogController)
}
@Single
fun provideWalletCoreConfig(
    context: Context,
    environmentConfig: EnvironmentConfig
): WalletConfig = WalletCoreConfigImpl(context, environmentConfig)

@Single
fun provideConfigWalletCore(context: Context, environmentConfig: EnvironmentConfig): WalletConfig = WalletCoreConfigImpl(context, environmentConfig)

@Single
fun provideWalletCoreLogController(logController: LogController): WalletCoreLogController =
    WalletCoreLogControllerImpl(logController)

@Singleton
fun provideSecureArea(context: Context): SecureArea {
    val storageFile = Path(context.filesDir.absolutePath + "/secure_area_storage")

    val storageEngine = AndroidStorageEngine.Builder(context, storageFile)
        .setUseEncryption(true)
        .build()

    return AndroidKeystoreSecureArea(context, storageEngine)
}

@Single
fun provideSecureAreaManager(
    secureArea: SecureArea,
    prefKeys: PrefKeys
): SecureAreaController = SecureAreaControllerImpl(
    secureArea,
    prefKeys
)

@Singleton
fun provideSecureAreaRepository(
    secureAreaController: SecureAreaController
): SecureAreaRepository = SecureAreaRepositoryImpl(secureAreaController)

@Factory
fun provideWalletCoreDocumentsController(
    resourceProvider: ResourceProvider,
    eudiWallet: EudiWallet,
    transactionStorageController: TransactionStorageController,
    bookmarkStorageController: BookmarkStorageController
): WalletCoreDocumentsController =
    WalletCoreDocumentsControllerImpl(
        resourceProvider,
        eudiWallet,
        transactionStorageController,
        bookmarkStorageController
    )

@Scope
class WalletPresentationScope

fun getOrCreatePresentationScope(): org.koin.core.scope.Scope =
    KoinPlatform.getKoin().getOrCreateScope<WalletPresentationScope>(PRESENTATION_SCOPE_ID)