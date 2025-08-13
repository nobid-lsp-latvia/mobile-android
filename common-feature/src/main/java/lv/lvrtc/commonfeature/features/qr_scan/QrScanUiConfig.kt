// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.qr_scan

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import lv.lvrtc.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.lvrtc.uilogic.serializer.UiSerializable
import lv.lvrtc.uilogic.serializer.UiSerializableParser
import lv.lvrtc.uilogic.serializer.adapter.SerializableTypeAdapter

sealed interface QrScanFlow {
    data object Presentation : QrScanFlow
    data class Issuance(val issuanceFlow: IssuanceFlowUiConfig) : QrScanFlow
}

data class QrScanUiConfig(
    val title: String,
    val subTitle: String,
    val qrScanFlow: QrScanFlow
) : UiSerializable {

    companion object Parser : UiSerializableParser {
        override val serializedKeyName = "qrScanConfig"
        override fun provideParser(): Gson {
            return GsonBuilder().registerTypeAdapter(
                QrScanFlow::class.java,
                SerializableTypeAdapter<QrScanFlow>()
            ).create()
        }
    }
}