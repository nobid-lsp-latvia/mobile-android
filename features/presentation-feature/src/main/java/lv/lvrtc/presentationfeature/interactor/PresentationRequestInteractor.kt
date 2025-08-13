// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.presentationfeature.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.commonfeature.config.RequestUriConfig
import lv.lvrtc.commonfeature.config.toDomainConfig
import lv.lvrtc.commonfeature.features.PresentationConfigStore
import lv.lvrtc.commonfeature.features.request.transformer.PresentationDocument
import lv.lvrtc.commonfeature.features.request.transformer.RequestTransformer
import lv.lvrtc.corelogic.controller.PaymentDetails
import lv.lvrtc.corelogic.controller.TransferEventPartialState
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.controller.WalletCorePresentationController
import lv.lvrtc.resourceslogic.provider.ResourceProvider

sealed class PresentationRequestInteractorPartialState {
    data class Success(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val requestDocuments: List<PresentationDocument>,
        val paymentDetails: PaymentDetails? = null
    ) : PresentationRequestInteractorPartialState()

    data class NoData(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val paymentDetails: PaymentDetails? = null
    ) : PresentationRequestInteractorPartialState()

    data class Failure(val error: String) : PresentationRequestInteractorPartialState()
    data object Disconnect : PresentationRequestInteractorPartialState()
}

interface PresentationRequestInteractor {
    fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(items: List<PresentationDocument>)
    fun setConfig(config: RequestUriConfig)
}

class PresentationRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : PresentationRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun setConfig(config: RequestUriConfig) {
        walletCorePresentationController.setConfig(config.toDomainConfig())
    }

    override fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState> =
        walletCorePresentationController.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    if (response.requestData.all { it.requestedItems.isEmpty() }) {
                        PresentationRequestInteractorPartialState.NoData(
                            verifierName = response.verifierName,
                            verifierIsTrusted = response.verifierIsTrusted,
                            paymentDetails = response.paymentDetails
                        )
                    } else {
                        val documentsDomain = RequestTransformer.transformToDomainItems(
                            storageDocuments = walletCoreDocumentsController.getAllIssuedDocuments(),
                            requestDocuments = response.requestData,
                            resourceProvider = resourceProvider
                        ).getOrThrow()

                        if (documentsDomain.isNotEmpty()) {
                            PresentationRequestInteractorPartialState.Success(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                                requestDocuments = RequestTransformer.transformToPresentationItems(
                                    documentsDomain = documentsDomain,
                                    resourceProvider = resourceProvider,
                                ),
                                paymentDetails = response.paymentDetails
                            )
                        } else {
                            PresentationRequestInteractorPartialState.NoData(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                                paymentDetails = response.paymentDetails
                            )
                        }
                    }
                }

                is TransferEventPartialState.Error -> {
                    PresentationRequestInteractorPartialState.Failure(error = response.error)
                }

                is TransferEventPartialState.Disconnected -> {
                    PresentationRequestInteractorPartialState.Disconnect
                }

                else -> null
            }
        }.safeAsync {
            PresentationRequestInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun stopPresentation() {
        PresentationConfigStore.clear()
        walletCorePresentationController.stopPresentation()
    }

    override fun updateRequestedDocuments(items: List<PresentationDocument>) {
        val disclosedDocuments = RequestTransformer.createDisclosedDocuments(items)
        walletCorePresentationController.updateRequestedDocuments(disclosedDocuments.toMutableList())
    }
}