// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.presentationfeature.di

import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.controller.WalletCorePresentationController
import lv.lvrtc.corelogic.di.PRESENTATION_SCOPE_ID
import lv.lvrtc.networklogic.api.payment.PaymentApiClient
import lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractor
import lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractorImpl
import lv.lvrtc.presentationfeature.interactor.PresentationLoadingInteractor
import lv.lvrtc.presentationfeature.interactor.PresentationLoadingInteractorImpl
import lv.lvrtc.presentationfeature.interactor.PresentationRequestInteractor
import lv.lvrtc.presentationfeature.interactor.PresentationRequestInteractorImpl
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.ScopeId

@Module
@ComponentScan("lv.lvrtc.presentationfeature")
class FeaturePresentationModule

@Factory
fun providePresentationRequestInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController
): PresentationRequestInteractor {
    return PresentationRequestInteractorImpl(
        resourceProvider,
        walletCorePresentationController,
        walletCoreDocumentsController
    )
}

@Factory
fun providePresentationLoadingInteractor(
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor
): PresentationLoadingInteractor {
    return PresentationLoadingInteractorImpl(
        walletCorePresentationController,
        deviceAuthenticationInteractor
    )
}

@Factory
fun providePaymentPresentationInteractor(
    paymentApiClient: PaymentApiClient
): PaymentPresentationInteractor {
    return PaymentPresentationInteractorImpl(paymentApiClient)
}