// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.payment

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

data class PaymentStatusResponse(
    val payment_status: String
)

interface PaymentApi {
    @GET
    suspend fun getPaymentStatus(@Url statusUri: String): Response<PaymentStatusResponse>
}