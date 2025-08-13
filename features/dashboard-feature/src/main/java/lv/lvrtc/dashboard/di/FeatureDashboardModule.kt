// SPDX-License-Identifier: EUPL-1.2

/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package lv.lvrtc.dashboard.di

import lv.lvrtc.dashboard.interactor.DashboardInteractor
import lv.lvrtc.dashboard.interactor.DashboardInteractorImpl
import lv.lvrtc.businesslogic.config.ConfigLogic
import lv.lvrtc.businesslogic.controller.log.LogController
import lv.lvrtc.corelogic.config.WalletConfig
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.BookmarkStorageController
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.lvrtc.dashboard")
class FeatureDashboardModule

@Factory
fun provideDashboardInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    walletCoreConfig: WalletConfig,
    configLogic: ConfigLogic,
    logController: LogController,
    bookmarkStorageController: BookmarkStorageController
): DashboardInteractor =
    DashboardInteractorImpl(
        resourceProvider,
        walletCoreDocumentsController,
        walletCoreConfig,
        configLogic,
        logController,
        bookmarkStorageController
    )