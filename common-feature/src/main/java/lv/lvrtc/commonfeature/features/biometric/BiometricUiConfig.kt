// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.biometric

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.lvrtc.uilogic.config.ConfigNavigation
import lv.lvrtc.uilogic.config.NavigationType
import lv.lvrtc.uilogic.serializer.UiSerializable
import lv.lvrtc.uilogic.serializer.UiSerializableParser
import lv.lvrtc.uilogic.serializer.adapter.SerializableTypeAdapter

data class BiometricUiConfig(
    val title: String,
    val subTitle: String,
    val quickPinOnlySubTitle: String,
    val isPreAuthorization: Boolean = false,
    val shouldInitializeBiometricAuthOnCreate: Boolean = true,
    val onSuccessNavigation: ConfigNavigation,
    val onBackNavigationConfig: OnBackNavigationConfig
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "biometricConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                NavigationType::class.java,
                SerializableTypeAdapter<NavigationType>()
            ).create()
        }
    }
}

data class OnBackNavigationConfig(
    val onBackNavigation: ConfigNavigation?,
    private val hasToolbarCancelIcon: Boolean
) {
    val isCancellable: Boolean get() = hasToolbarCancelIcon && onBackNavigation != null
}