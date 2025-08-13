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

package lv.lvrtc.corelogic.controller

import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import com.android.identity.crypto.Algorithm
import com.google.common.reflect.TypeToken
import com.nimbusds.jose.shaded.gson.Gson
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.toKotlinResult
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.model.BiometricCrypto
import lv.lvrtc.businesslogic.extensions.addOrReplace
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.corelogic.di.WalletPresentationScope
import lv.lvrtc.corelogic.model.AuthenticationData
import lv.lvrtc.corelogic.model.toDocumentIdentifier
import lv.lvrtc.corelogic.util.EudiWalletListenerWrapper
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import lv.lvrtc.storagelogic.model.Transaction
import lv.lvrtc.storagelogic.model.TransactionType
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import java.net.URI

sealed class PresentationControllerConfig(val initiatorRoute: String) {
    data class OpenId4VP(val uri: String, val initiator: String) :
        PresentationControllerConfig(initiator)

    data class Ble(val initiator: String) : PresentationControllerConfig(initiator)
}

sealed class TransferEventPartialState {
    data object Connected : TransferEventPartialState()
    data object Connecting : TransferEventPartialState()
    data object Disconnected : TransferEventPartialState()
    data class Error(val error: String) : TransferEventPartialState()
    data class QrEngagementReady(val qrCode: String) : TransferEventPartialState()
    data class RequestReceived(
        val requestData: List<RequestedDocument>,
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val paymentDetails: PaymentDetails? = null
    ) : TransferEventPartialState()

    data object ResponseSent : TransferEventPartialState()
    data class Redirect(val uri: URI) : TransferEventPartialState()
}

data class PaymentDetails(
    val paymentId: String,
    val amount: String?,
    val currency: String?,
    val creditor: String?,
    val purpose: String?
)

data class PaymentRequestData(
    val type: String,
    val payment_id: String,
    val instructed_amount: String?,
    val currency: String?,
    val creditor: String?,
    val purpose: String?
)

sealed class CheckKeyUnlockPartialState {
    data class Failure(val error: String) : CheckKeyUnlockPartialState()
    data class UserAuthenticationRequired(
        val authenticationData: List<AuthenticationData>,
    ) : CheckKeyUnlockPartialState()

    data object RequestIsReadyToBeSent : CheckKeyUnlockPartialState()
}

sealed class SendRequestedDocumentsPartialState {
    data class Failure(val error: String) : SendRequestedDocumentsPartialState()
    data object RequestSent : SendRequestedDocumentsPartialState()
}

sealed class ResponseReceivedPartialState {
    data object Success : ResponseReceivedPartialState()
    data class Redirect(val uri: URI) : ResponseReceivedPartialState()
    data class Failure(val error: String) : ResponseReceivedPartialState()
}

sealed class WalletCorePartialState {
    data class UserAuthenticationRequired(
        val authenticationData: List<AuthenticationData>,
    ) : WalletCorePartialState()

    data class Failure(val error: String) : WalletCorePartialState()
    data object Success : WalletCorePartialState()
    data class Redirect(val uri: URI) : WalletCorePartialState()
    data object RequestIsReadyToBeSent : WalletCorePartialState()
}

/**
 * Common scoped interactor that has all the complexity and required interaction with the EudiWallet Core.
 * */
interface WalletCorePresentationController {
    /**
     * On initialization it adds the core listener and remove it when scope is canceled.
     * When the scope is canceled so does the presentation
     *
     * @return Hot Flow that emits the Core's status callback.
     * */
    val events: SharedFlow<TransferEventPartialState>

    /**
     * User selection data for request step
     * */
    val disclosedDocuments: MutableList<DisclosedDocument>?

    /**
     * Verifier name so it can be retrieve across screens
     * */
    val verifierName: String?

    /**
     * Who started the presentation
     * */
    val initiatorRoute: String

    /**
     * Set [PresentationControllerConfig]
     * */
    fun setConfig(config: PresentationControllerConfig)

    /**
     * Terminates the presentation and kills the coroutine scope that [events] live in
     * */
    fun stopPresentation()

    /**
     * Starts QR engagement. This will trigger [events] emission.
     *
     * [TransferEventPartialState.QrEngagementReady] -> QR String to show QR
     *
     * [TransferEventPartialState.Connecting] -> Connecting
     *
     * [TransferEventPartialState.Connected] -> Connected. We can proceed to the next screen
     * */
    fun startQrEngagement()

    /**
     * Enable/Disable NFC service
     * */
    fun toggleNfcEngagement(componentActivity: ComponentActivity, toggle: Boolean)

    /**
     * Transform UI models to Domain and create -> sent the request.
     *
     * @return Flow that emits the creation state. On Success send the request.
     * The response of that request is emitted through [events]
     *  */
    fun checkForKeyUnlock(): Flow<CheckKeyUnlockPartialState>

    fun sendRequestedDocuments(): SendRequestedDocumentsPartialState

    /**
     * Updates the UI model
     * @param disclosedDocuments User updated data through UI Events
     * */
    fun updateRequestedDocuments(disclosedDocuments: MutableList<DisclosedDocument>?)

    /**
     * @return flow that maps the state from [events] emission to what we consider as success state
     * */
    fun mappedCallbackStateFlow(): Flow<ResponseReceivedPartialState>

    /**
     * The main observation point for collecting state for the Request flow.
     * Exposes a single flow for two operations([checkForKeyUnlock] - [mappedCallbackStateFlow])
     * and a single state
     * @return flow that emits the create, sent, receive states
     * */
    fun observeSentDocumentsRequest(): Flow<WalletCorePartialState>
}

@Scope(WalletPresentationScope::class)
@Scoped
class WalletCorePresentationControllerImpl(
    private val eudiWallet: EudiWallet,
    private val resourceProvider: ResourceProvider,
    private val transactionStorageController: TransactionStorageController,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WalletCorePresentationController {

    private val genericErrorMessage = resourceProvider.genericErrorMessage()

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private lateinit var _config: PresentationControllerConfig

    override var disclosedDocuments: MutableList<DisclosedDocument>? = null

    private var processedRequest: RequestProcessor.ProcessedRequest.Success? = null

    override var verifierName: String? = null

    override val initiatorRoute: String
        get() {
            val config = requireInit { _config }
            return config.initiatorRoute
        }

    override fun setConfig(config: PresentationControllerConfig) {
        _config = config
    }

    override val events: SharedFlow<TransferEventPartialState> = callbackFlow {
        val eventListenerWrapper = EudiWalletListenerWrapper(
            onQrEngagementReady = { qrCode ->
                trySendBlocking(
                    TransferEventPartialState.QrEngagementReady(qrCode = qrCode)
                )
            },
            onConnected = {
                trySendBlocking(
                    TransferEventPartialState.Connected
                )
            },
            onConnecting = {
                trySendBlocking(
                    TransferEventPartialState.Connecting
                )
            },
            onDisconnected = {
                trySendBlocking(
                    TransferEventPartialState.Disconnected
                )
            },
            onError = { errorMessage ->
                trySendBlocking(
                    TransferEventPartialState.Error(
                        error = errorMessage.ifEmpty { genericErrorMessage }
                    )
                )
            },
            onRequestReceived = { requestedDocumentData ->
                trySendBlocking(
                    requestedDocumentData.getOrNull()?.let { requestedDocuments ->

                        processedRequest = requestedDocuments

                        val readerNameOrTxData = requestedDocuments.requestedDocuments
                            .firstOrNull()?.readerAuth?.readerCommonName

                        var actualReaderName: String? = null
                        var paymentDetails: PaymentDetails? = null

                        if (readerNameOrTxData?.startsWith("TXDATA:") == true) {
                            val transactionDataJson = readerNameOrTxData.removePrefix("TXDATA:")
                            try {
                                val paymentDataList = Gson().fromJson<List<PaymentRequestData>>(
                                    transactionDataJson,
                                    object : TypeToken<List<PaymentRequestData>>() {}.type
                                )
                                val paymentRequestData = paymentDataList.firstOrNull { it.type == "PaymentRequest" }

                                if (paymentRequestData != null) {
                                    paymentDetails = PaymentDetails(
                                        paymentId = paymentRequestData.payment_id,
                                        amount = paymentRequestData.instructed_amount?.toString(),
                                        currency = paymentRequestData.currency,
                                        creditor = paymentRequestData.creditor,
                                        purpose = paymentRequestData.purpose
                                    )
                                    actualReaderName = paymentDetails.creditor
                                } else {
                                    actualReaderName = "N/A"
                                }

                            } catch (e: Exception) {
                                actualReaderName = "N/A"
                            }
                        } else {
                            actualReaderName = readerNameOrTxData
                        }


                        verifierName = actualReaderName

                        val verifierIsTrusted = requestedDocuments.requestedDocuments
                            .firstOrNull()?.readerAuth?.isVerified == true

                        TransferEventPartialState.RequestReceived(
                            requestData = requestedDocuments.requestedDocuments,
                            verifierName = actualReaderName,
                            verifierIsTrusted = verifierIsTrusted,
                            paymentDetails = paymentDetails
                        )
                    } ?: TransferEventPartialState.Error(error = genericErrorMessage)
                )
            },
            onResponseSent = {
                trySendBlocking(
                    TransferEventPartialState.ResponseSent
                )
            },
            onRedirect = { uri ->
                trySendBlocking(
                    TransferEventPartialState.Redirect(
                        uri = uri
                    )
                )
            }
        )

        addListener(eventListenerWrapper)
        awaitClose {
            removeListener(eventListenerWrapper)
            eudiWallet.stopProximityPresentation()
        }
    }.safeAsync {
        TransferEventPartialState.Error(
            error = it.localizedMessage ?: resourceProvider.genericErrorMessage()
        )
    }.shareIn(coroutineScope, SharingStarted.Lazily, 2)

    override fun startQrEngagement() {
        eudiWallet.startProximityPresentation()
    }

    override fun toggleNfcEngagement(componentActivity: ComponentActivity, toggle: Boolean) {
        try {
            if (toggle) {
                eudiWallet.enableNFCEngagement(componentActivity)
            } else {
                eudiWallet.disableNFCEngagement(componentActivity)
            }
        } catch (_: Exception) {
        }
    }

    override fun checkForKeyUnlock() = flow {
        disclosedDocuments?.let { documents ->

            val authenticationData = mutableListOf<AuthenticationData>()

            if (eudiWallet.config.userAuthenticationRequired) {

                val keyUnlockDataMap = documents.associateWith {
                    eudiWallet.getDefaultKeyUnlockData(it.documentId)
                }

                for ((doc, kud) in keyUnlockDataMap) {
                    authenticationData.add(
                        AuthenticationData(
                            BiometricCrypto(kud?.getCryptoObjectForSigning(Algorithm.ES256)),
                            onAuthenticationSuccess = {
                                disclosedDocuments?.addOrReplace(doc.copy(keyUnlockData = kud)) {
                                    it.documentId == doc.documentId
                                }
                            }
                        )
                    )
                }

                emit(
                    CheckKeyUnlockPartialState.UserAuthenticationRequired(
                        authenticationData
                    )
                )

            } else {
                emit(
                    CheckKeyUnlockPartialState.RequestIsReadyToBeSent
                )
            }
        }
    }.safeAsync {
        CheckKeyUnlockPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMessage
        )
    }

    private suspend fun logPresentationTransaction(documents: List<DisclosedDocument>) {
        documents.forEach { doc ->
            eudiWallet.getDocumentById(doc.documentId)?.let { fullDoc ->
                transactionStorageController.store(
                    Transaction(
                        documentId = fullDoc.id,
                        docType = fullDoc.toDocumentIdentifier().formatType,
                        nameSpace = fullDoc.name,
                        eventType = TransactionType.DOCUMENT_PRESENTED.name,
                        authority = verifierName,
                        status = "SUCCESS",
                    )
                )
            }
        }
    }

    override fun sendRequestedDocuments(): SendRequestedDocumentsPartialState {
        return disclosedDocuments?.let { safeDisclosedDocuments ->

            var result: SendRequestedDocumentsPartialState =
                SendRequestedDocumentsPartialState.RequestSent

            processedRequest?.generateResponse(DisclosedDocuments(safeDisclosedDocuments.toList()))
                ?.toKotlinResult()
                ?.onFailure {
                    val errorMessage = it.localizedMessage ?: genericErrorMessage
                    result = SendRequestedDocumentsPartialState.Failure(
                        error = errorMessage
                    )
                }
                ?.onSuccess {
                    eudiWallet.sendResponse(it.response)
                    coroutineScope.launch {
                        val isPayment = processedRequest?.requestedDocuments?.firstOrNull()?.readerAuth?.readerCommonName?.startsWith("TXDATA:")
                        isPayment?.let { it1 -> if (!it1) logPresentationTransaction(safeDisclosedDocuments.toList()) }
                    }
                    result = SendRequestedDocumentsPartialState.RequestSent
                }
            result
        } ?: SendRequestedDocumentsPartialState.Failure(
            error = genericErrorMessage
        )
    }

    override fun mappedCallbackStateFlow(): Flow<ResponseReceivedPartialState> {
        return events.mapNotNull { response ->
            when (response) {

                // Fix: Wallet core should return Success state here
                is TransferEventPartialState.Error -> {
                    if (response.error == "Peer disconnected without proper session termination") {
                        ResponseReceivedPartialState.Success
                    } else {
                        ResponseReceivedPartialState.Failure(error = response.error)
                    }
                }

                is TransferEventPartialState.Redirect -> {
                    ResponseReceivedPartialState.Redirect(uri = response.uri)
                }

                is TransferEventPartialState.Disconnected -> {
                    when {
                        events.replayCache.firstOrNull() is TransferEventPartialState.Redirect -> null
                        else -> ResponseReceivedPartialState.Success
                    }
                }

                is TransferEventPartialState.ResponseSent -> ResponseReceivedPartialState.Success

                else -> null
            }
        }.safeAsync {
            ResponseReceivedPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }
    }

    override fun observeSentDocumentsRequest(): Flow<WalletCorePartialState> =
        merge(checkForKeyUnlock(), mappedCallbackStateFlow()).mapNotNull {
            when (it) {
                is CheckKeyUnlockPartialState.Failure -> {
                    WalletCorePartialState.Failure(it.error)
                }

                is CheckKeyUnlockPartialState.UserAuthenticationRequired -> {
                    WalletCorePartialState.UserAuthenticationRequired(it.authenticationData)
                }

                is ResponseReceivedPartialState.Failure -> {
                    WalletCorePartialState.Failure(it.error)
                }

                is ResponseReceivedPartialState.Redirect -> {
                    WalletCorePartialState.Redirect(
                        uri = it.uri
                    )
                }

                is CheckKeyUnlockPartialState.RequestIsReadyToBeSent -> {
                    WalletCorePartialState.RequestIsReadyToBeSent
                }

                else -> {
                    WalletCorePartialState.Success
                }
            }
        }.safeAsync {
            WalletCorePartialState.Failure(
                error = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun updateRequestedDocuments(disclosedDocuments: MutableList<DisclosedDocument>?) {
        this.disclosedDocuments = disclosedDocuments
    }

    override fun stopPresentation() {
        eudiWallet.stopProximityPresentation()
        coroutineScope.cancel()
        (events as? MutableSharedFlow<TransferEventPartialState>)?.resetReplayCache()
    }

    private fun addListener(listener: EudiWalletListenerWrapper) {
        val config = requireInit { _config }
        eudiWallet.addTransferEventListener(listener)
        if (config is PresentationControllerConfig.OpenId4VP) {
            eudiWallet.startRemotePresentation(Uri.parse(config.uri))
        }
    }

    private fun removeListener(listener: EudiWalletListenerWrapper) {
        requireInit { _config }
        eudiWallet.removeTransferEventListener(listener)
    }

    private fun <T> requireInit(block: () -> T): T {
        if (!::_config.isInitialized) {
            throw IllegalStateException("setConfig() must be called before using the WalletCorePresentationController")
        }
        return block()
    }
}