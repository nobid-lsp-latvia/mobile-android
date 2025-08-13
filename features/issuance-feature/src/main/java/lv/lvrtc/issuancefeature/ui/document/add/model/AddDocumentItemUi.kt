// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.issuancefeature.ui.document.add.model

import lv.lvrtc.uilogic.components.IconData

data class DocumentOptionItemUi(
    val configId: String,
    val text: String,
    val icon: IconData,
    val available: Boolean,
    val alreadyHave: Boolean
)