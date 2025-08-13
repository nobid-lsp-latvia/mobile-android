// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.document_details.model

import lv.lvrtc.uilogic.components.InfoTextWithNameAndImageData
import lv.lvrtc.uilogic.components.InfoTextWithNameAndValueData

sealed interface DocumentDetailsUi {

    data class DefaultItem(
        val itemData: InfoTextWithNameAndValueData
    ) : DocumentDetailsUi

    data class SignatureItem(
        val itemData: InfoTextWithNameAndImageData
    ) : DocumentDetailsUi

    data object Unknown : DocumentDetailsUi
}
