// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.webfeature.ui

import android.app.Activity
import android.content.Intent
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import lv.lvrtc.corelogic.util.CoreActions
import lv.lvrtc.uilogic.components.SystemBroadcastReceiver
import lv.lvrtc.uilogic.components.content.ContentScreen
import lv.lvrtc.uilogic.components.content.ScreenNavigateAction
import lv.lvrtc.uilogic.extension.getPendingDeepLink

@Composable
fun WebScreen(
    navController: NavController,
    viewModel: WebViewModel,
) {
    val context = LocalContext.current
    val state = viewModel.viewState.value
    var webView by remember { mutableStateOf<WebView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val path = currentBackStackEntry?.arguments?.getString("path")
    val status = currentBackStackEntry?.arguments?.getString("status")

    val backgroundColor = MaterialTheme.colorScheme.background

    StatusBarConfig()

    SystemBroadcastReceiver(
        actions = listOf(
            CoreActions.VCI_RESUME_ACTION,
            CoreActions.VCI_DYNAMIC_PRESENTATION,
            CoreActions.EPARAKSTS_AUTH_DONE,
            CoreActions.EPARAKSTS_RESUME,
            CoreActions.SIGN_COMPLETE
        )
    ) { intent ->
        when (intent?.action) {
            CoreActions.VCI_RESUME_ACTION -> {
                intent.extras?.getString("uri")?.let { uri ->
                    webView?.evaluateJavascript("""
                        window.dispatchEvent(new CustomEvent('lx-vciresume-response', { 
                            detail: { uri: '$uri' }
                        }));
                    """.trimIndent(), null)
                }
            }
            CoreActions.EPARAKSTS_AUTH_DONE -> {
                intent?.getStringExtra("code")?.let { code ->
                    viewModel.handleAuthCode(code)
                }
            }
            CoreActions.EPARAKSTS_RESUME -> {
                intent.getStringExtra("url")?.let { url ->
                    webView?.loadUrl(url)
                }
            }
            CoreActions.SIGN_COMPLETE -> {
                val success = intent.getBooleanExtra("success", false)
                val path = if (success) "sign-done/success" else "sign/error"
                webView?.loadUrl(viewModel.getWebUrl("index.html#/$path"))
            }
        }
    }
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE
        ) { padding ->
            if (state.webViewError) {
                FallbackScreen(
                    onRetry = { viewModel.setEvent(WebEvent.RetryLoading) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)
                        )
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = with(LocalDensity.current) {
                                    WindowInsets.systemBars.getBottom(this).toDp()
                                }
                            ),
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                setBackgroundColor(backgroundColor.toArgb())
                                settings.apply {
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                    javaScriptEnabled = true
                                    domStorageEnabled = true

                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(
                                        view: WebView?,
                                        newProgress: Int,
                                    ) {
                                        viewModel.setEvent(WebEvent.ProgressChanged(newProgress))
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        viewModel.setEvent(WebEvent.WebViewError(error))
                                        view?.stopLoading()
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse,
                                    ) {
                                        viewModel.setEvent(WebEvent.WebViewError(null))
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler,
                                        error: SslError,
                                    ) {
                                        viewModel.setEvent(WebEvent.WebViewError(null))
                                        handler.cancel()
                                    }
                                }
                                webView = this
                                viewModel.setEvent(WebEvent.WebViewCreated(this))
                            }
                        }
                    )
                }
            }
        }

    BackHandler(enabled = true) {
        val currentUrl = webView?.url ?: ""
        if (currentUrl.contains("sign-done")) {
            // Do nothing
        } else {
            webView?.let { view ->
                if (view.canGoBack()) {
                    view.goBack()
                } else {
                    (lifecycleOwner as? ComponentActivity)?.finish()
                }
            }
        }
    }

    LaunchedEffect(path, status) {
        path?.let {
            val fullPath = if (status != null) "$it?status=$status" else it
            webView?.loadUrl(viewModel.getWebUrl("index.html#/$fullPath"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WebEffect.Navigation.SwitchScreen -> {
                    navController.navigate(effect.screenRoute)
                }
                is WebEffect.Navigation.LoadUrl -> {
                    webView?.loadUrl(viewModel.getWebUrl("index.html#/${effect.path}"))
                }
                is WebEffect.Navigation.LoadExternalUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri())
                    context.startActivity(intent)
                }
                is WebEffect.Navigation.Back -> {
                    webView?.let { view ->
                        if (view.canGoBack()) {
                            view.goBack()
                        } else {
                            (lifecycleOwner as? ComponentActivity)?.finish()
                        }
                    }
                }
                is WebEffect.Reload -> {
                    webView?.reload()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                webView?.destroy()
            }
            if(event == Lifecycle.Event.ON_RESUME) {
                if (webView?.url?.contains("eparaksts") == true) {
                    webView?.reload()
                }
                webView?.evaluateJavascript("""
                        window.dispatchEvent(new CustomEvent('resume', { 
                            detail: { }
                        }));
                    """.trimIndent(), null)

                webView?.context?.getPendingDeepLink()?.let { deepLinkUri ->
                    viewModel.handleDeepLink(deepLinkUri)
                }
            }

        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun StatusBarConfig() {
    val activity = LocalView.current.context as? Activity
    val window = activity?.window
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = MaterialTheme.colorScheme.background

    DisposableEffect(isDarkTheme) {
        val originalColor = window?.statusBarColor

        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = backgroundColor.toArgb()
            WindowInsetsControllerCompat(it, it.decorView).apply {
                // Set status bar icons dark in light theme, light in dark theme
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }

        onDispose {
            window?.statusBarColor = originalColor ?: 0
        }
    }
}