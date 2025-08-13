// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features

import lv.lvrtc.commonfeature.config.RequestUriConfig

object PresentationConfigStore {
    private var config: RequestUriConfig? = null

    fun setConfig(config: RequestUriConfig) {
        this.config = config
    }

    fun getConfig(): RequestUriConfig? {
        return config
    }

    fun clear() {
        config = null
    }
}