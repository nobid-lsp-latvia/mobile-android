// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.transactionsfeature

import kotlinx.coroutines.launch
import lv.lvrtc.resourceslogic.bridge.TRANSACTIONS
import lv.lvrtc.transactionsfeature.ui.TransactionsInteractor
import lv.lvrtc.transactionsfeature.ui.TransactionsInteractorGetTransactionsPartialState
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.BridgeRequest
import lv.lvrtc.webbridge.core.BridgeResponse

class TransactionsBridge(
    private val transactionsInteractor: TransactionsInteractor
) : BaseBridge() {

    override fun getName() = TRANSACTIONS.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            TRANSACTIONS.GET_TRANSACTIONS -> handleGetTransactions(request)
            else -> BridgeResponse(
                id = request.id,
                status = BridgeResponse.Status.ERROR,
                error = "Unknown function"
            )
        }
    }

    private fun handleGetTransactions(request: BridgeRequest): BridgeResponse {
        val documentId = (request.data as? Map<*, *>)?.get("documentId") as? String

        coroutineScope.launch {
            transactionsInteractor.getTransactions(documentId).collect { result ->
                when (result) {
                    is TransactionsInteractorGetTransactionsPartialState.Success -> {
                        val transactions = result.transactionList
                            .sortedByDescending { it.timestamp }
                            .map { transaction ->
                                val nameSpace = if (transaction.nameSpace == "eu.europa.ec.eudi.iban") "IBAN" else transaction.nameSpace

                                mapOf(
                                "id" to transaction.id,
                                "documentId" to transaction.documentId,
                                "docType" to transaction.docType,
                                "nameSpace" to nameSpace,
                                "timestamp" to transaction.timestamp,
                                "eventType" to transaction.eventType,
                                "status" to transaction.status,
                                "authority" to transaction.authority
                            )
                        }
                        emitEvent(createSuccessResponse(request, mapOf(
                            "transactions" to transactions
                        )))
                    }
                    is TransactionsInteractorGetTransactionsPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, result.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }
}