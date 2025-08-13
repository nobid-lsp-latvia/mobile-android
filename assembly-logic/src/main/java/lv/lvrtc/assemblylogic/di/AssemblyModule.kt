// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.assemblylogic.di

import android.app.Application
import lv.lvrtc.dashboard.di.FeatureDashboardModule
import lv.lvrtc.analyticslogic.di.LogicAnalyticsModule
import lv.lvrtc.authlogic.di.AuthModule
import lv.lvrtc.businesslogic.di.BusinessModule
import lv.lvrtc.commonfeature.di.CommonModule
import lv.lvrtc.corelogic.di.CoreModule
import lv.lvrtc.issuancefeature.di.IssuanceModule
import lv.lvrtc.networklogic.di.NetworkModule
import lv.lvrtc.presentationfeature.di.FeaturePresentationModule
import lv.lvrtc.resourceslogic.di.ResourcesModule
import lv.lvrtc.startupfeature.di.StartupModule
import lv.lvrtc.storagelogic.di.LogicStorageModule
import lv.lvrtc.transactionsfeature.di.FeatureTransactionsModule
import lv.lvrtc.signfeature.di.SignModule
import lv.lvrtc.uilogic.di.LogicUiModule
import lv.lvrtc.webbridge.di.WebBridgeModule
import lv.lvrtc.webfeature.di.WebFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module

private val assembledModules = listOf(
    // Project modules
    LogicAnalyticsModule().module,
    LogicUiModule().module,
    AuthModule().module,
    BusinessModule().module,
    CoreModule().module,
    ResourcesModule().module,
    LogicStorageModule().module,
    FeatureTransactionsModule().module,
    WebBridgeModule().module,
    StartupModule().module,
    CommonModule().module,
    NetworkModule().module,
    SignModule().module,
    // WebView modules
    WebFeatureModule().module,
    IssuanceModule().module,
    FeatureDashboardModule().module,
    FeaturePresentationModule().module,
)

internal fun Application.setupKoin() {
    startKoin {
        androidContext(this@setupKoin)
        androidLogger()
        modules(assembledModules)
    }
}