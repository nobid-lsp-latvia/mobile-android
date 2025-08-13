// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.service

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import lv.lvrtc.authlogic.BuildConfig
import lv.lvrtc.networklogic.api.user.PersonVerificationStatus
import lv.lvrtc.networklogic.session.SessionManager
import org.koin.core.annotation.Singleton
import lv.lvrtc.networklogic.session.TokenStorage

interface AuthService {
    fun buildEParakstsAuthUrl(): String
}

@Singleton
class AuthServiceImpl(
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage
) : AuthService {

    override fun buildEParakstsAuthUrl(): String {
        return Uri.parse(BuildConfig.EIDAS_AUTH_URL)
            .buildUpon()
            .appendQueryParameter("client_id", BuildConfig.EPARAKSTS_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.EPARAKSTS_REDIRECT_URI)
            .appendQueryParameter("acr_values", BuildConfig.EPARAKSTS_ACR_VALUES)
            .build()
            .toString()
    }
}

sealed class AuthPartialState {
    data class Success(val url: String) : AuthPartialState()
    data class Failure(val error: String) : AuthPartialState()
}

data class AuthResult(
    val status: AuthPartialState,
    val personStatus: PersonVerificationStatus? = null
)
