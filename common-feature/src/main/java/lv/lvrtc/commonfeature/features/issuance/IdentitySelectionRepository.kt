// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.issuance

import lv.lvrtc.networklogic.model.wallet.EParakstIdentitiesResponse

object IdentitySelectionRepository {
    private var selectionOptions: EParakstIdentitiesResponse? = null

    fun setSelectionOptions(options: EParakstIdentitiesResponse) {
        selectionOptions = options
    }

    fun getSelectionOptions(): EParakstIdentitiesResponse? {
        return selectionOptions
    }

    fun clearSelectionOptions() {
        selectionOptions = null
    }
}