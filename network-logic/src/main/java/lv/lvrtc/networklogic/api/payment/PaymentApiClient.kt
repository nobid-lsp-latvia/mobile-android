// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.payment

import lv.lvrtc.networklogic.api.base.BaseApiClient
import lv.lvrtc.networklogic.error.ApiErrorHandler

interface PaymentApiClient {
    suspend fun getPaymentStatus(statusUri: String): Result<PaymentStatusResponse>
}

class PaymentApiClientImpl(
    private val paymentApi: PaymentApi,
    errorHandler: ApiErrorHandler
) : BaseApiClient(errorHandler), PaymentApiClient {

    override suspend fun getPaymentStatus(statusUri: String): Result<PaymentStatusResponse> {
        return handleRequest { paymentApi.getPaymentStatus(statusUri) }
    }
}