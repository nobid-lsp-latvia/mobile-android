// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.webbridge.core

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import lv.lvrtc.networklogic.error.ErrorUtils

interface WebBridgeInterface {
    fun getName(): String

    @JavascriptInterface
    fun handleRequest(request: String): String
}

data class BridgeRequest(
    val id: String,
    val function: String,
    val data: Any? = null
)

data class BridgeResponse(
    val id: String,
    val status: Status,
    val data: Any? = null,
    val error: String? = null
) {
    enum class Status { SUCCESS, ERROR }
}

abstract class BaseBridge : WebBridgeInterface {
    protected val coroutineScope = CoroutineScope(Dispatchers.IO)
    protected var webView: WebView? = null
    private val gson = Gson()

    fun attachWebView(webView: WebView) {
        this.webView = webView
    }

    @JavascriptInterface
    override fun handleRequest(request: String): String =
        try {
            gson.toJson(handleRequest(gson.fromJson(request, BridgeRequest::class.java)))
        } catch (e: Exception) {
            gson.toJson(BridgeResponse(id = "", status = BridgeResponse.Status.ERROR, error = e.message))
        }

    protected fun emitEvent(response: BridgeResponse) {
        webView?.post {
            val jsonData = gson.toJson(response)
            webView?.evaluateJavascript(
                """
                window.dispatchEvent(new CustomEvent('lx-embed-response', {
                    detail: $jsonData
                }));
                """.trimIndent(),
                null
            )
        }
    }

    protected abstract fun handleRequest(request: BridgeRequest): BridgeResponse

    protected fun createSuccessResponse(request: BridgeRequest, data: Any?): BridgeResponse {
        return BridgeResponse(
            id = request.id,
            status = BridgeResponse.Status.SUCCESS,
            data = data
        )
    }

    protected fun createErrorResponse(request: BridgeRequest, error: Any?): BridgeResponse {
        return BridgeResponse(
            id = request.id,
            status = BridgeResponse.Status.ERROR,
            error = ErrorUtils.extractErrorCode(error.toString())
        )
    }
}