// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.webbridge.di

import lv.lvrtc.webbridge.WebBridge
import lv.lvrtc.webbridge.config.WebViewConfig
import lv.lvrtc.webbridge.registry.BridgeProvider
import lv.lvrtc.webbridge.registry.BridgeRegistry
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class WebBridgeModule {
    @Single
    fun provideWebViewConfig() = WebViewConfig()

    @Single
    fun provideWebBridge(config: WebViewConfig) = WebBridge(config)

    @Single
    fun provideBridgeRegistry(
        bridgeProviders: List<BridgeProvider>
    ) = BridgeRegistry(bridgeProviders)
}