// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.util

import lv.lvrtc.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument

object DocumentFieldExtractor {
    fun extractDisplayNumber(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.PID_ID_NUMBER)
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.MDL_ID_NUMBER)
            DocumentIdentifier.MdocRTUDiploma ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.DIPLOMA_ID_NUMBER)
            DocumentIdentifier.SdJwtA2Pay ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.SEB_IBAN)
            else -> ""
        }

    fun extractDescription(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.ISSUER_COUNTRY)
            DocumentIdentifier.MdocRTUDiploma -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.DIPLOMA_THEMATIC_AREA)
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.ISSUER_COUNTRY)
            DocumentIdentifier.MdocESeal -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.SIGNING_NAME)
            else -> ""
        }

    fun extractAdditionalInfo(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid -> ""
            DocumentIdentifier.MdocRTUDiploma -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.DIPLOMA_ACHIEVEMENT)
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL -> {
                val privileges = document.data.claims
                    .firstOrNull { it.identifier == DocumentJsonKeys.DRIVING_PRIVILEGES }
                    ?.value
                extractDrivingPrivileges(privileges)
            }
            else -> ""
        }
}