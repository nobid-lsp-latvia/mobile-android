// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.success

import androidx.annotation.DrawableRes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.serializer.UiSerializable
import lv.lvrtc.uilogic.serializer.UiSerializableParser
import lv.lvrtc.uilogic.serializer.adapter.SerializableTypeAdapter

data class SuccessUIConfig(
    val headerConfig: HeaderConfig?,
    val content: String,
    val imageConfig: ImageConfig,
    val buttonConfig: List<ButtonConfig>,
    val onBackScreenToNavigate: ConfigNavigation,
) : UiSerializable {

    data class ImageConfig(
        val type: Type,
        @DrawableRes val drawableRes: Int? = null,
        val contentDescription: String? = null
    ) {
        enum class Type {
            DRAWABLE, DEFAULT
        }
    }

    data class ButtonConfig(
        val text: String,
        val style: Style,
        val navigation: ConfigNavigation,
    ) {
        enum class Style {
            PRIMARY, OUTLINE
        }
    }

    data class HeaderConfig(
        val title: String,
    )

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "successConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                NavigationType::class.java,
                SerializableTypeAdapter<NavigationType>()
            ).create()
        }
    }
}