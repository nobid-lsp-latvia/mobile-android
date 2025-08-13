// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.user

import lv.lvrtc.networklogic.api.base.BaseApiClient
import lv.lvrtc.networklogic.error.ApiError
import lv.lvrtc.networklogic.error.ApiErrorHandler
import lv.lvrtc.networklogic.model.user.EmailRequest
import lv.lvrtc.networklogic.model.user.SmsRequest
import lv.lvrtc.networklogic.model.user.VerifyEmailRequest
import lv.lvrtc.networklogic.model.user.VerifySmsRequest

sealed class PersonVerificationStatus {
    data object Complete : PersonVerificationStatus()
    data class Incomplete(
        val needsEmail: Boolean,
        val needsPhone: Boolean
    ) : PersonVerificationStatus()
}

interface UserApiClient {
    suspend fun getPerson(): Result<PersonVerificationStatus>
    suspend fun sendEmailVerification(email: String): Result<Unit>
    suspend fun verifyEmail(code: String): Result<Unit>
    suspend fun sendSmsVerification(phoneNumber: String): Result<Unit>
    suspend fun verifySms(code: String): Result<Unit>
}

class UserApiClientImpl(
    private val userApi: UserApi,
    private val errorHandler: ApiErrorHandler
) : BaseApiClient(errorHandler), UserApiClient {

    override suspend fun sendEmailVerification(email: String): Result<Unit> {
        return handleRequest { userApi.sendEmailVerification(EmailRequest(email)) }
    }

    override suspend fun verifyEmail(code: String): Result<Unit> {
        return handleRequest { userApi.verifyEmail(VerifyEmailRequest(code)) }
    }

    override suspend fun sendSmsVerification(phoneNumber: String): Result<Unit> {
        return handleRequest { userApi.sendSmsVerification(SmsRequest(phoneNumber)) }
    }

    override suspend fun verifySms(code: String): Result<Unit> {
        return handleRequest { userApi.verifySms(VerifySmsRequest(code)) }
    }

    override suspend fun getPerson(): Result<PersonVerificationStatus> {
        return try {
            val response = userApi.getPerson()
            when (response.code()) {
                200 -> {
                    val person = response.body()
                    if (person != null) {
                        val hasEmail = person.contacts.any { it.type.equals("email", ignoreCase = true) }
                        val hasPhone = person.contacts.any { it.type.equals("phone", ignoreCase = true) }

                        if (hasEmail && hasPhone) {
                            Result.success(PersonVerificationStatus.Complete)
                        } else {
                            Result.success(PersonVerificationStatus.Incomplete(
                                needsEmail = !hasEmail,
                                needsPhone = !hasPhone
                            ))
                        }
                    } else {
                        Result.failure(IllegalStateException("Person response body is null"))
                    }
                }
                404 -> Result.success(PersonVerificationStatus.Incomplete(
                    needsEmail = true,
                    needsPhone = true
                ))
                401, 403 -> {
                    Result.failure(ApiError.UnauthorizedException("Authentication required"))
                }
                else -> Result.failure(errorHandler.handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(errorHandler.handleException(e))
        }
    }
}