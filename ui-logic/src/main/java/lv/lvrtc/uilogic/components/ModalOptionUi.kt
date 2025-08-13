// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.uilogic.components

import lv.lvrtc.uilogic.mvi.ViewEvent

data class ModalOptionUi<T : ViewEvent>(
    val title: String,
    val icon: IconData,
    val event: T
)