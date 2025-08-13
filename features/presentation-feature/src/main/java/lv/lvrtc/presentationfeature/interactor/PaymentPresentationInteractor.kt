// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.presentationfeature.interactor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.lvrtc.networklogic.api.payment.PaymentApiClient

sealed class PaymentStatusState {
    object Loading : PaymentStatusState()
    data class Status(val status: String) : PaymentStatusState()
    data class Error(val message: String) : PaymentStatusState()
    data class Timeout(val message: String) : PaymentStatusState()
    object Completed : PaymentStatusState()
}

interface PaymentPresentationInteractor {
    fun pollPaymentStatus(
        paymentStatusUri: String,
        maxAttempts: Int = MAX_POLLING_ATTEMPTS,
        pollingInterval: Long = POLLING_INTERVAL
    ): Flow<PaymentStatusState>

    companion object {
        const val MAX_POLLING_ATTEMPTS = 30
        const val POLLING_INTERVAL = 1000L
    }
}

class PaymentPresentationInteractorImpl(
    private val paymentApiClient: PaymentApiClient
) : PaymentPresentationInteractor {

    override fun pollPaymentStatus(
        paymentStatusUri: String,
        maxAttempts: Int,
        pollingInterval: Long
    ): Flow<PaymentStatusState> = flow {
        var attemptCount = 0
        var isFinalStatus = false

        emit(PaymentStatusState.Loading)

        while (!isFinalStatus && attemptCount < maxAttempts) {
            paymentApiClient.getPaymentStatus(paymentStatusUri).fold(
                onSuccess = { response ->
                    val status = response.payment_status
                    emit(PaymentStatusState.Status(status))
                    isFinalStatus = status in listOf("ACSC", "NAUT", "RJCT", "CANC")
                    if (isFinalStatus) emit(PaymentStatusState.Completed)
                },
                onFailure = { error ->
                    emit(PaymentStatusState.Error(error.message ?: "Unknown error"))
                    isFinalStatus = true
                }
            )
            if (!isFinalStatus) {
                delay(pollingInterval)
                attemptCount++
            }
        }
        if (!isFinalStatus) emit(PaymentStatusState.Timeout("Payment status check timed out"))
    }
}