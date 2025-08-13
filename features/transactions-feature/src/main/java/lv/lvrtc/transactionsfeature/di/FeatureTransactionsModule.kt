// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.transactionsfeature.di

import lv.lvrtc.businesslogic.controller.log.LogController
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import lv.lvrtc.transactionsfeature.ui.TransactionsInteractor
import lv.lvrtc.transactionsfeature.ui.TransactionsInteractorImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.lvrtc.transactionsfeature")
class FeatureTransactionsModule

@Factory
fun provideTransactionsInteractor(
    resourceProvider: ResourceProvider,
    transactionStorageController: TransactionStorageController,
    logController: LogController
): TransactionsInteractor =
    TransactionsInteractorImpl(
        resourceProvider,
        transactionStorageController,
        logController,
    )