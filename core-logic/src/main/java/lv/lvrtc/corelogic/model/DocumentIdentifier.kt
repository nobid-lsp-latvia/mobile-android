// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.model

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat


typealias FormatType = String

sealed interface DocumentIdentifier {
    val formatType: FormatType

    data object MdocPid : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.pid.1"
    }

    data object MdocPseudonym : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.pseudonym.age_over_18.1"
    }

    data object SdJwtPid : DocumentIdentifier {
        override val formatType: FormatType
            get() = "urn:eu.europa.ec.eudi:pid:1"
    }

    data object SdJwtA2Pay : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.iban"
    }

    data object MdocRTUDiploma : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.rtu_diploma_mdoc"
    }

    data object MdocMDL : DocumentIdentifier {
        override val formatType: FormatType
            get() = "org.iso.18013.5.1.mDL"
    }

    data object SdJwtMDL : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.mdl_jwt_vc_json"
    }

    data object MdocESeal : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.eseal"
    }

    data object MdocESign : DocumentIdentifier {
        override val formatType: FormatType
            get() = "eu.europa.ec.eudi.esign"
    }

    data class OTHER(
        override val formatType: FormatType,
    ) : DocumentIdentifier
}

fun FormatType.toDocumentIdentifier(): DocumentIdentifier = when (this) {
    DocumentIdentifier.MdocPid.formatType -> DocumentIdentifier.MdocPid
    DocumentIdentifier.SdJwtPid.formatType -> DocumentIdentifier.SdJwtPid
    DocumentIdentifier.MdocPseudonym.formatType -> DocumentIdentifier.MdocPseudonym
    DocumentIdentifier.MdocRTUDiploma.formatType -> DocumentIdentifier.MdocRTUDiploma
    DocumentIdentifier.MdocMDL.formatType -> DocumentIdentifier.MdocMDL
    DocumentIdentifier.MdocESeal.formatType -> DocumentIdentifier.MdocESeal
    DocumentIdentifier.MdocESign.formatType -> DocumentIdentifier.MdocESign
    DocumentIdentifier.SdJwtA2Pay.formatType -> DocumentIdentifier.SdJwtA2Pay
    DocumentIdentifier.SdJwtMDL.formatType -> DocumentIdentifier.SdJwtMDL
    else -> DocumentIdentifier.OTHER(formatType = this)
}

fun Document.toDocumentIdentifier(): DocumentIdentifier {
    val formatType = when (val f = format) {
        is MsoMdocFormat -> f.docType
        is SdJwtVcFormat -> f.vct
    }
    return createDocumentIdentifier(formatType)
}

fun Document.getFormatType(): String? {
    return when (val format = this.format) {
        is MsoMdocFormat -> format.docType
        is SdJwtVcFormat -> format.vct
        else -> null
    }
}

private fun createDocumentIdentifier(
    formatType: FormatType
): DocumentIdentifier {
    return when (formatType) {
        DocumentIdentifier.MdocPid.formatType -> DocumentIdentifier.MdocPid
        DocumentIdentifier.SdJwtPid.formatType -> DocumentIdentifier.SdJwtPid
        DocumentIdentifier.MdocPseudonym.formatType -> DocumentIdentifier.MdocPseudonym
        DocumentIdentifier.MdocRTUDiploma.formatType -> DocumentIdentifier.MdocRTUDiploma
        DocumentIdentifier.MdocMDL.formatType -> DocumentIdentifier.MdocMDL
        DocumentIdentifier.MdocESeal.formatType -> DocumentIdentifier.MdocESeal
        DocumentIdentifier.MdocESign.formatType -> DocumentIdentifier.MdocESign
        DocumentIdentifier.SdJwtA2Pay.formatType -> DocumentIdentifier.SdJwtA2Pay
        DocumentIdentifier.SdJwtMDL.formatType -> DocumentIdentifier.SdJwtMDL
        else -> DocumentIdentifier.OTHER(formatType = formatType)
    }
}