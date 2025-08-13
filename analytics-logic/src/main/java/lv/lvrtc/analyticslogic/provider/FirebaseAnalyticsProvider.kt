package lv.lvrtc.analyticslogic.provider

import android.app.Application
import android.webkit.WebView
import com.google.firebase.crashlytics.FirebaseCrashlytics

interface CrashlyticsProvider {
    fun logError(error: Throwable, message: String? = null)
}

class FirebaseCrashlyticsProvider : AnalyticsProvider, CrashlyticsProvider {

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = true
        }
    }

    override fun initialize(context: Application, key: String) {
        crashlytics
    }

    override fun logScreen(name: String, arguments: Map<String, String>) {
        crashlytics.setCustomKey("current_screen", name)
        arguments.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
        }
    }

    override fun logEvent(event: String, arguments: Map<String, String>) {
        crashlytics.log("$event: $arguments")
    }

    override fun logError(error: Throwable, message: String?) {
        crashlytics.setCustomKey("webview_version", WebView.getCurrentWebViewPackage()?.versionName ?: "unknown")
        message?.let { crashlytics.log(it) }
        crashlytics.recordException(error)
    }
}