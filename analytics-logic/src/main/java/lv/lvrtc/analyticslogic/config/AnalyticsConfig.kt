package lv.lvrtc.analyticslogic.config

import lv.lvrtc.analyticslogic.provider.AnalyticsProvider
import lv.lvrtc.analyticslogic.provider.FirebaseCrashlyticsProvider

interface AnalyticsConfig {
    val analyticsProviders: Map<String, AnalyticsProvider>
        get() = emptyMap()
}

class AnalyticsConfigImpl : AnalyticsConfig {
    override val analyticsProviders: Map<String, AnalyticsProvider> = mapOf(
        "firebase_crashlytics" to FirebaseCrashlyticsProvider()
    )
}