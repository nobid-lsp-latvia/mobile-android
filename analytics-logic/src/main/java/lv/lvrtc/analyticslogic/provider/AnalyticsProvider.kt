package lv.lvrtc.analyticslogic.provider

import android.app.Application

interface AnalyticsProvider {
    fun initialize(context: Application, key: String)
    fun logScreen(name: String, arguments: Map<String, String> = emptyMap())
    fun logEvent(event: String, arguments: Map<String, String>)
}