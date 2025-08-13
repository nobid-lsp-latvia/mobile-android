// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.di

import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.storagelogic.config.StorageConfig
import lv.lvrtc.storagelogic.config.StorageConfigImpl
import lv.lvrtc.storagelogic.controller.BookmarkStorageController
import lv.lvrtc.storagelogic.controller.BookmarkStorageControllerImpl
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import lv.lvrtc.storagelogic.controller.TransactionStorageControllerImpl
import lv.lvrtc.storagelogic.service.RealmService
import lv.lvrtc.storagelogic.service.RealmServiceImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.storagelogic")
class LogicStorageModule

@Single
fun provideStorageConfig(prefKeys: PrefKeys): StorageConfig =
    StorageConfigImpl(prefKeys)

@Single
fun provideRealmService(storageConfig: StorageConfig): RealmService =
    RealmServiceImpl(storageConfig)

@Factory
fun provideTransactionStorageController(realmService: RealmService): TransactionStorageController =
    TransactionStorageControllerImpl(realmService)

@Factory
fun provideBookmarkStorageController(realmService: RealmService): BookmarkStorageController =
    BookmarkStorageControllerImpl(realmService)