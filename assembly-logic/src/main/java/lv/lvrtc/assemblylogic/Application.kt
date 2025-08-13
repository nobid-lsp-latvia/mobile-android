// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.assemblylogic

import android.app.Application
import android.util.Log
import eu.europa.ec.eudi.wallet.EudiWallet
import lv.lvrtc.analyticslogic.controller.AnalyticsController
import lv.lvrtc.assemblylogic.di.setupKoin
import lv.lvrtc.corelogic.config.WalletConfig
import lv.lvrtc.corelogic.controller.WalletCoreLogController
import org.koin.android.ext.android.inject

class Application : Application() {

    private val walletConfig: WalletConfig by inject()
    private val analyticsController: AnalyticsController by inject()
    private val walletCoreLogController: WalletCoreLogController by inject()

    override fun onCreate() {
        super.onCreate()
        setupKoin()
        initializeReporting()
        initializeEudiWallet()
    }

    private fun initializeReporting() {
        analyticsController.initialize(this)
    }

    private fun initializeEudiWallet() {
        EudiWallet(
            applicationContext,
            walletConfig.config
        ) {
            withLogger(walletCoreLogController)
        }
    }
}