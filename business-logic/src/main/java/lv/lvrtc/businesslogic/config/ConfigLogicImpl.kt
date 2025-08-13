// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.businesslogic.config

import lv.lvrtc.businesslogic.BuildConfig

class ConfigLogicImpl : ConfigLogic {
    override val appFlavor: AppFlavor
        get() = try {
            AppFlavor.valueOf(BuildConfig.FLAVOR.uppercase())
        } catch (e: IllegalArgumentException) {
            AppFlavor.ZZDEV
        }

    override val environmentConfig: EnvironmentConfig
        get() = when (appFlavor) {
            AppFlavor.ZZDEV -> ZZDevEnvironmentConfig()
            AppFlavor.ZZWEB -> ZZDevEnvironmentConfig()
            AppFlavor.ZZDEMO -> ZZDevEnvironmentConfig()
            AppFlavor.PROD -> ProdEnvironmentConfig()
            AppFlavor.DEMO -> DemoEnvironmentConfig()
        }
}

private class ZZDevEnvironmentConfig : EnvironmentConfig() {
    override fun getServerHost() = "https://edim-dev.zzdats.lv/api/1.0/"
    override fun getSessionApiHost() = "https://edim-dev.zzdats.lv/idauth/api/1.0/"
    override fun getVciIssuerUrl() = "https://edim-demo-issuer-dev.zzdats.lv"
    override fun getWalletApiHost() = "https://edim-dev.zzdats.lv/"
}

private class ProdEnvironmentConfig : EnvironmentConfig() {
    override fun getServerHost() = ""
    override fun getSessionApiHost() = ""
    override fun getVciIssuerUrl() = ""
    override fun getWalletApiHost() = ""
}

private class DemoEnvironmentConfig : EnvironmentConfig() {
    override fun getServerHost() = "https://edim-test.eparaksts.lv/api/1.0/"
    override fun getSessionApiHost() = "https://edim-test.eparaksts.lv/idauth/api/1.0/"
    override fun getVciIssuerUrl() = "https://edim-demo-issuer-test.eparaksts.lv"
    override fun getWalletApiHost() = "https://edim-test.eparaksts.lv/"
}