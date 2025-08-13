// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.webbridge

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import lv.lvrtc.webbridge.config.WebViewConfig
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.WebBridgeInterface

interface UrlHandler {
    fun handleUrl(request: WebResourceRequest): Boolean
}

class WebBridge(
    private val config: WebViewConfig
) {
    private val bridges = mutableListOf<WebBridgeInterface>()

    fun registerBridge(bridge: WebBridgeInterface) {
        bridges.add(bridge)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(webView: WebView) {
        configureWebView(webView)
        setupAssetLoader(webView)
        attachBridges(webView)
        loadInitialPage(webView)
    }

    private fun configureWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = config.security.javaScriptEnabled
            domStorageEnabled = config.security.domStorageEnabled
            allowFileAccess = config.security.allowFileAccess
            cacheMode = config.network.cacheMode
        }
    }

    private fun setupAssetLoader(webView: WebView) {
        val assetLoader = config.createAssetLoader(webView.context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ) = if (request.url.scheme == "https") {
                assetLoader.shouldInterceptRequest(request.url)
            } else null

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                var handled = false
                bridges.forEach { bridge ->
                    if ((bridge as? UrlHandler)?.handleUrl(request) == true) {
                        handled = true
                    }
                }
                return handled
            }
        }
    }

    private fun attachBridges(webView: WebView) {
        bridges.forEach { bridge ->
            if (bridge is BaseBridge) {
                bridge.attachWebView(webView)
            }
            webView.addJavascriptInterface(bridge, bridge.getName())
        }
    }

    private fun loadInitialPage(webView: WebView) {
        webView.loadUrl(config.getAssetUrl("index.html"))
    }
}