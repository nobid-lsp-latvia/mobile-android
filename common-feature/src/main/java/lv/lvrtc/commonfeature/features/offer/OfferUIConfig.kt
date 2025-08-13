// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.offer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.serializer.UiSerializable
import lv.lvrtc.uilogic.serializer.UiSerializableParser
import lv.lvrtc.uilogic.serializer.adapter.SerializableTypeAdapter

data class OfferUiConfig(
    val offerURI: String,
    val onSuccessNavigation: ConfigNavigation,
    val onCancelNavigation: ConfigNavigation,
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "offerConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                NavigationType::class.java,
                SerializableTypeAdapter<NavigationType>()
            ).create()
        }
    }
}