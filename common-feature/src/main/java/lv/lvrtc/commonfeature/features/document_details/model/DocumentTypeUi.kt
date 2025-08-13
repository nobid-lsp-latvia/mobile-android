// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.document_details.model

import eu.europa.ec.eudi.wallet.document.DocumentId
import lv.lvrtc.corelogic.model.DocumentIdentifier

enum class DocumentUiIssuanceState {
    Issued, Pending, Failed
}

data class DocumentUi(
    val documentIssuanceState: DocumentUiIssuanceState,
    val documentName: String,
    val documentIdentifier: DocumentIdentifier,
    val documentExpirationDateFormatted: String,
    val documentExpirationDate: String,
    val documentHasExpired: Boolean,
    val documentIsBookmarked: Boolean,
    val documentImage: String,
    val documentDetails: List<DocumentDetailsUi>,
    val userFullName: String? = null,
    val documentId: DocumentId,
    val issuingAuthority: String? = null,
    val issuerCountry: String? = null,
    val issuanceDate: String? = null,
    val displayNumber: String? = null,
    val description: String? = null,
    val additionalInfo: String? = null
)