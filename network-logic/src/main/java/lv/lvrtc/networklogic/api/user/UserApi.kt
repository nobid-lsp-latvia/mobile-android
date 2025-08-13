// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.user

import lv.lvrtc.networklogic.model.user.EmailRequest
import lv.lvrtc.networklogic.model.user.PersonResponse
import lv.lvrtc.networklogic.model.user.SmsRequest
import lv.lvrtc.networklogic.model.user.VerifyEmailRequest
import lv.lvrtc.networklogic.model.user.VerifySmsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("person")
    suspend fun getPerson(): Response<PersonResponse>

    @POST("person/email")
    suspend fun sendEmailVerification(
        @Body request: EmailRequest
    ): Response<Unit>

    @POST("person/email/verify")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): Response<Unit>

    @POST("person/phone")
    suspend fun sendSmsVerification(
        @Body request: SmsRequest
    ): Response<Unit>

    @POST("person/phone/verify")
    suspend fun verifySms(
        @Body request: VerifySmsRequest
    ): Response<Unit>
}