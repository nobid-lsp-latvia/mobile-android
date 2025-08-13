// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.error

import java.net.SocketTimeoutException
import java.net.UnknownHostException


object ErrorUtils {
    private val ALLOWED_ERROR_TAGS = setOf(
        "invalid",
        "toomanyattempts",
        "e164",
        // Signing
        "sign_not_found_eseal_signing_identity",
        "sign_not_found_signing_identity",
        "sign_not_found_identity",
        "sign_documents_already_exist",
        "sign_no_documents_selected"
    )

    fun extractErrorCode(throwable: Throwable?): String? {
        return when {
            throwable is ApiError.NetworkException -> throwable.message ?: "network_error"
            throwable is ApiError.UnauthorizedException -> "unauthorized"
            throwable is ApiError.ServerException -> "server_error"
            throwable is ApiError.BadRequestException -> "bad_request"
            throwable is ApiError.ForbiddenException -> "forbidden"
            throwable is ApiError.ApiException -> "api_error"

            throwable is UnknownHostException -> "unknown_host"
            throwable is SocketTimeoutException -> "timeout"

            throwable?.message?.contains("UnknownHostException") == true -> "unknown_host"
            throwable?.message?.contains("SocketTimeoutException") == true -> "timeout"
            throwable?.message?.contains("ECONNRESET") == true -> "connection_reset"
            throwable?.message?.contains("SSL") == true -> "ssl_error"

            throwable is ApiError.ValidationException ->
                if (throwable.errorTag != null && ALLOWED_ERROR_TAGS.contains(throwable.errorTag)) {
                    throwable.errorTag
                } else {
                    "validation_error"
                }

            else -> null
        }
    }

    fun extractErrorCode(errorMessage: String?): String? {
        return extractErrorCode(Throwable(errorMessage))
    }
}