// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lv.lvrtc.networklogic.api.session.SessionApi
import lv.lvrtc.networklogic.api.session.SessionApiClient
import org.koin.core.annotation.Singleton


@Singleton
class SessionManager(
    private val sessionApiClient: SessionApiClient,
    private val tokenStorage: TokenStorage
) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = job + Dispatchers.IO

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.NotInitialized)
    val sessionState = _sessionState.asStateFlow()

    private var keepAliveJob: Job? = null

    suspend fun checkSession(): Boolean {
        if (!tokenStorage.hasToken()) return false

        return sessionApiClient.getStatus()
            .onSuccess { status ->
                if (status.active) {
                    _sessionState.value = SessionState.Active(status.secondsToLive)
                    startKeepAlive(status.secondsToCountdown.toLong())
                    return true
                } else {
                    _sessionState.value = SessionState.Expired
                    tokenStorage.clearToken()
                }
            }
            .onFailure {
                _sessionState.value = SessionState.Error("Session expired")
                tokenStorage.clearToken()
            }
            .isSuccess
    }

    private fun startKeepAlive(initialDelay: Long) {
        keepAliveJob?.cancel()
        keepAliveJob = launch {
            delay(initialDelay * 1000)
            while (isActive) {
                sessionApiClient.keepAlive()
                    .onSuccess {
                        checkSession()
                    }
                    .onFailure {
                        _sessionState.value = SessionState.Error("Keep-alive failed")
                        tokenStorage.clearToken()
                        return@launch
                    }
                delay(KEEP_ALIVE_INTERVAL)
            }
        }
    }

    fun logout() {
        keepAliveJob?.cancel()
        tokenStorage.clearToken()
        _sessionState.value = SessionState.NotInitialized
    }

    companion object {
        private const val KEEP_ALIVE_INTERVAL = 4 * 60 * 1000L // 4 minutes
    }
}

sealed class SessionState {
    object NotInitialized : SessionState()
    data class Active(val secondsToLive: Int) : SessionState()
    object Expired : SessionState()
    data class Error(val message: String) : SessionState()
}