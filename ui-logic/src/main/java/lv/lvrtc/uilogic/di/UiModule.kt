// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.uilogic.di

import lv.lvrtc.analyticslogic.controller.AnalyticsController
import lv.lvrtc.uilogic.navigation.RouterHost
import lv.lvrtc.uilogic.navigation.RouterHostImpl
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.uilogic.serializer.UiSerializer
import lv.lvrtc.uilogic.serializer.UiSerializerImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.lvrtc.uilogic")
class LogicUiModule

@Single
fun provideRouterHost(
    analyticsController: AnalyticsController
): RouterHost = RouterHostImpl(analyticsController)

@Factory
fun provideUiSerializer(): UiSerializer = UiSerializerImpl()

@Single
fun provideWebNavigationService(): WebNavigationService = WebNavigationService()