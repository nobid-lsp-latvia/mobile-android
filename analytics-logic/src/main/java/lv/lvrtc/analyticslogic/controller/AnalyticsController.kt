package lv.lvrtc.analyticslogic.controller

import android.app.Application
import lv.lvrtc.analyticslogic.config.AnalyticsConfig

interface AnalyticsController {
    fun initialize(context: Application)
    fun logScreen(name: String, arguments: Map<String, String> = emptyMap())
    fun logEvent(eventName: String, arguments: Map<String, String> = emptyMap())
}

class AnalyticsControllerImpl(
    private val analyticsConfig: AnalyticsConfig,
) : AnalyticsController {

    override fun initialize(context: Application) {
        analyticsConfig.analyticsProviders.forEach { (key, analyticProvider) ->
            analyticProvider.initialize(context, key)
        }
    }

    override fun logScreen(name: String, arguments: Map<String, String>) {
        analyticsConfig.analyticsProviders.values.forEach {
            it.logScreen(name, arguments)
        }
    }

    override fun logEvent(eventName: String, arguments: Map<String, String>) {
        analyticsConfig.analyticsProviders.values.forEach {
            it.logEvent(eventName, arguments)
        }
    }
}
