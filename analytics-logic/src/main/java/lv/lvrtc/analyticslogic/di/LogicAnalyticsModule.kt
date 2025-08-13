package lv.lvrtc.analyticslogic.di

import lv.lvrtc.analyticslogic.config.AnalyticsConfig
import lv.lvrtc.analyticslogic.controller.AnalyticsController
import lv.lvrtc.analyticslogic.controller.AnalyticsControllerImpl
import lv.lvrtc.analyticslogic.provider.CrashlyticsProvider
import lv.lvrtc.analyticslogic.provider.FirebaseCrashlyticsProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.analyticslogic")
class LogicAnalyticsModule

@Single
fun provideAnalyticsConfig(): AnalyticsConfig {
    return try {
        val impl = Class.forName("lv.lvrtc.analyticslogic.config.AnalyticsConfigImpl")
        return impl.getDeclaredConstructor().newInstance() as AnalyticsConfig
    } catch (_: Exception) {
        val impl = object : AnalyticsConfig {}
        impl
    }
}

@Single
fun provideAnalyticsController(analyticsConfig: AnalyticsConfig): AnalyticsController =
    AnalyticsControllerImpl(analyticsConfig)

@Single
fun provideCrashlyticsProvider(): CrashlyticsProvider = FirebaseCrashlyticsProvider()