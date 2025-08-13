// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.transactionsfeature.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.lvrtc.businesslogic.controller.log.LogController
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import lv.lvrtc.storagelogic.model.Transaction

sealed class TransactionsInteractorGetTransactionsPartialState {
    data class Success(
        val transactionList: List<Transaction>,
    ) : TransactionsInteractorGetTransactionsPartialState()

    data class Failure(val error: String) : TransactionsInteractorGetTransactionsPartialState()
}

interface TransactionsInteractor {
    fun getTransactions(documentId: String?): Flow<TransactionsInteractorGetTransactionsPartialState>
}

class TransactionsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val transactionStorageController: TransactionStorageController,
    private val logController: LogController
) : TransactionsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()


    override fun getTransactions(documentId: String?): Flow<TransactionsInteractorGetTransactionsPartialState> = flow {
        val transactions = transactionStorageController.retrieveAll(documentId)

        emit(
            TransactionsInteractorGetTransactionsPartialState.Success(
                transactionList = transactions,
            )
        )
    }.safeAsync {
        TransactionsInteractorGetTransactionsPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}