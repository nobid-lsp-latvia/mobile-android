// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.issuance

import lv.lvrtc.commonfeature.features.offer.OfferUiConfig

object OfferConfigRepository {
    private var offerConfig: OfferUiConfig? = null

    fun setOfferConfig(config: OfferUiConfig) {
        offerConfig = config
    }

    fun getOfferConfig(clear: Boolean = true): OfferUiConfig? {
        val config = offerConfig
        if (clear) {
            offerConfig = null
        }
        return config
    }

    fun clear() {
        offerConfig = null
    }
}