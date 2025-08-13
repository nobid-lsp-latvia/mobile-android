// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.extension

import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import lv.lvrtc.businesslogic.extensions.compareLocaleLanguage
import lv.lvrtc.corelogic.model.DocumentIdentifier
import lv.lvrtc.corelogic.model.toDocumentIdentifier
import java.util.Locale

fun Offer.getIssuerName(locale: Locale): String =
    issuerMetadata.display.find { it.locale?.let { locale.compareLocaleLanguage(Locale(it)) } == true }?.name
        ?: issuerMetadata.credentialIssuerIdentifier.value.value.host

val Offer.OfferedDocument.documentIdentifier: DocumentIdentifier?
    get() = when (val format = documentFormat) {
        is MsoMdocFormat -> format.docType.toDocumentIdentifier()
        is SdJwtVcFormat -> format.vct.toDocumentIdentifier()
        null -> null
    }

fun Offer.OfferedDocument.getName(locale: Locale): String? = configuration
    .display
    .find { locale.compareLocaleLanguage(it.locale) }
    ?.name
    ?: when (val format = documentFormat) {
        is MsoMdocFormat -> format.docType
        is SdJwtVcFormat -> format.vct
        null -> null
    }