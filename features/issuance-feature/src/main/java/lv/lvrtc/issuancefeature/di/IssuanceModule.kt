// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.issuancefeature.di

import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.issuancefeature.ui.IssuanceBridge
import lv.lvrtc.issuancefeature.ui.document.add.AddDocumentInteractor
import lv.lvrtc.issuancefeature.ui.document.add.AddDocumentInteractorImpl
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.lvrtc.issuancefeature.ui.document.details.DocumentDetailsInteractorImpl
import lv.lvrtc.issuancefeature.ui.document.offer.DocumentOfferInteractor
import lv.lvrtc.issuancefeature.ui.document.offer.DocumentOfferInteractorImpl
import lv.lvrtc.issuancefeature.ui.success.SuccessInteractor
import lv.lvrtc.issuancefeature.ui.success.SuccessInteractorImpl
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.BookmarkStorageController
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.uilogic.serializer.UiSerializer
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.lvrtc.issuancefeature")
class IssuanceModule

@Factory
fun provideAddDocumentInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer,
    walletApiClient: WalletApiClient,
    authService: AuthService
): AddDocumentInteractor =
    AddDocumentInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer,
        walletApiClient,
        authService
    )

@Factory
fun provideSuccessInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController
): SuccessInteractor = SuccessInteractorImpl(resourceProvider, walletCoreDocumentsController)

@Factory
fun provideDocumentOfferInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer,
    authService: AuthService,
    walletApiClient: WalletApiClient
): DocumentOfferInteractor =
    DocumentOfferInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer,
        authService,
        walletApiClient
    )

@Factory
fun provideDocumentDetailsInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    bookmarkStorageController: BookmarkStorageController
): DocumentDetailsInteractor =
    DocumentDetailsInteractorImpl(walletCoreDocumentsController, bookmarkStorageController, resourceProvider)