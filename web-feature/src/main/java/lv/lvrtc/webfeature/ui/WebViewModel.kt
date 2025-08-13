// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.webfeature.ui

import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import lv.lvrtc.commonfeature.features.user.onboarding.OnboardingBridge
import lv.lvrtc.dashboard.ui.DashboardBridge
import lv.lvrtc.resourceslogic.bridge.DASHBOARD
import lv.lvrtc.uilogic.mvi.MviViewModel
import lv.lvrtc.uilogic.mvi.ViewEvent
import lv.lvrtc.uilogic.mvi.ViewSideEffect
import lv.lvrtc.uilogic.mvi.ViewState
import lv.lvrtc.uilogic.navigation.NavigationCommand
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.webbridge.WebBridge
import lv.lvrtc.webbridge.config.WebViewConfig
import lv.lvrtc.webbridge.registry.BridgeRegistry
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class WebViewModel(
    private val bridge: WebBridge,
    private val onboardingBridge: OnboardingBridge,
    private val bridgeRegistry: BridgeRegistry,
    private val navigationService: WebNavigationService,
    private val webViewConfig: WebViewConfig
) : MviViewModel<WebEvent, WebState, WebEffect>() {

    init {
        bridgeRegistry.getAllBridges().forEach { bridge.registerBridge(it) }

        viewModelScope.launch {
            navigationService.navigation.collect { command ->
                when (command) {
                    is NavigationCommand.ToNative -> {
                        setEffect { WebEffect.Navigation.SwitchScreen(command.route) }
                    }
                    is NavigationCommand.ToWeb -> {
                        setEffect { WebEffect.Navigation.LoadUrl(command.path) }
                    }
                    is NavigationCommand.ToExternal -> {
                        setEffect { WebEffect.Navigation.LoadExternalUrl(command.route) }
                    }
                    NavigationCommand.Back -> {
                        setEffect { WebEffect.Navigation.Back }
                    }
                }
            }
        }
    }

    fun handleAuthCode(code: String) {
        onboardingBridge.handleAuthCode(code)
    }

    fun handleDeepLink(uri: Uri) {
        viewModelScope.launch {
            val dashboardBridge = bridgeRegistry.getAllBridges().find {
                it.getName() == DASHBOARD.BRIDGE_NAME
            } as? DashboardBridge

            dashboardBridge?.handlePendingDeepLink(uri)
        }
    }

    override fun setInitialState() = WebState()

    override fun handleEvents(event: WebEvent) {
        when (event) {
            is WebEvent.WebViewCreated -> setupWebView(event.webView)
            is WebEvent.ProgressChanged -> updateProgress(event.progress)
            is WebEvent.WebViewError -> handleError(event.error)
            is WebEvent.RetryLoading -> {
                setState {
                    copy(
                        webViewError = false,
                        isLoading = true
                    )
                }
                setEffect { WebEffect.Reload }
            }
        }
    }

    private fun setupWebView(webView: WebView) {
        bridge.setup(webView)
        setState { copy(isLoading = false) }
    }

    fun getWebUrl(path: String): String {
        return webViewConfig.getAssetUrl(path)
    }

    private fun updateProgress(progress: Int) {
        setState {
            copy(
                isLoading = progress < 100,
                loadingProgress = progress
            )
        }
    }

    private fun handleError(error: WebResourceError?) {
        setState {
            copy(
                webViewError = true,
                isLoading = false
            )
        }
    }
}

data class WebState(
    val currentUrl: String = "",
    val isLoading: Boolean = true,
    val loadingProgress: Int = 0,
    val webViewError: Boolean = false,
) : ViewState

sealed class WebEvent : ViewEvent {
    data class WebViewCreated(val webView: WebView) : WebEvent()
    data class ProgressChanged(val progress: Int) : WebEvent()
    data class WebViewError(val error: WebResourceError?) : WebEvent()
    data object RetryLoading : WebEvent()
}

sealed class WebEffect : ViewSideEffect {
    sealed class Navigation : WebEffect() {
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data class LoadUrl(val path: String) : Navigation()
        data class LoadExternalUrl(val url: String) : Navigation()
        data object Back : Navigation()
    }
    data object Reload : WebEffect()
}