// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.attestation

import lv.lvrtc.networklogic.model.attestation.InstanceRq
import lv.lvrtc.networklogic.model.attestation.NonceResp
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AttestationApi {
    @POST("wallet/nonce")
    suspend fun getNonce(): Response<NonceResp>

    @POST("wallet/instance")
    suspend fun getInstance(@Body rq: InstanceRq): Response<Unit>
}