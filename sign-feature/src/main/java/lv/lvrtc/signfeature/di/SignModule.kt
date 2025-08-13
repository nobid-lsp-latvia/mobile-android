// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.signfeature.di

import android.content.Context
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.signfeature.interactor.EParakstIdentitiesInteractor
import lv.lvrtc.signfeature.interactor.EParakstIdentitiesInteractorImpl
import lv.lvrtc.signfeature.interactor.SignDocumentInteractor
import lv.lvrtc.signfeature.interactor.SignDocumentInteractorImpl
import lv.lvrtc.signfeature.util.FilePickerHelper
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.signfeature")
class SignModule

@Single
fun provideFilePickerHelper(context: Context): FilePickerHelper = FilePickerHelper(context)

@Factory
fun provideSignDocumentInteractor(
    walletApiClient: WalletApiClient,
    resourceProvider: ResourceProvider,
    authService: AuthService,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    transactionStorageController: TransactionStorageController
): SignDocumentInteractor = SignDocumentInteractorImpl(
    walletApiClient,
    resourceProvider,
    authService,
    walletCoreDocumentsController,
    transactionStorageController
)

@Factory
fun provideEParakstIdentitiesInteractor(
    walletApiClient: WalletApiClient,
    walletCoreDocumentsController: WalletCoreDocumentsController
): EParakstIdentitiesInteractor = EParakstIdentitiesInteractorImpl(
    walletApiClient,
    walletCoreDocumentsController
)