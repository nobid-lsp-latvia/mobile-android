// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.session

import lv.lvrtc.networklogic.api.base.BaseApiClient
import lv.lvrtc.networklogic.error.ApiErrorHandler
import lv.lvrtc.networklogic.model.session.SessionResponse
import lv.lvrtc.networklogic.model.session.SessionStatus

interface SessionApiClient {
    suspend fun createSession(): Result<SessionResponse>
    suspend fun getStatus(): Result<SessionStatus>
    suspend fun keepAlive(): Result<Unit>
}

class SessionApiClientImpl(
    private val sessionApi: SessionApi,
    private val errorHandler: ApiErrorHandler
) : BaseApiClient(errorHandler), SessionApiClient {

    override suspend fun createSession(): Result<SessionResponse> =
        handleRequest { sessionApi.createSession() }

    override suspend fun getStatus(): Result<SessionStatus> =
        handleRequest { sessionApi.getSessionStatus() }

    override suspend fun keepAlive(): Result<Unit> =
        handleRequest { sessionApi.keepAlive() }
}