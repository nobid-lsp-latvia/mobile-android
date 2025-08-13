// SPDX-License-Identifier: EUPL-1.2

/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package lv.lvrtc.commonfeature.features.document_details.transformer

import android.util.Log
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import lv.lvrtc.businesslogic.extensions.compareLocaleLanguage
import lv.lvrtc.businesslogic.util.toDateFormatted
import lv.lvrtc.commonfeature.features.document_details.model.DocumentDetailsUi
import lv.lvrtc.commonfeature.features.document_details.model.DocumentUi
import lv.lvrtc.commonfeature.features.document_details.model.DocumentUiIssuanceState
import lv.lvrtc.commonfeature.util.DocumentFieldExtractor
import lv.lvrtc.commonfeature.util.DocumentJsonKeys
import lv.lvrtc.commonfeature.util.convertAnyToFormattedDate
import lv.lvrtc.commonfeature.util.documentHasExpired
import lv.lvrtc.commonfeature.util.extractDrivingPrivileges
import lv.lvrtc.commonfeature.util.extractFullNameFromDocumentOrEmpty
import lv.lvrtc.commonfeature.util.extractValueFromDocumentOrEmpty
import lv.lvrtc.commonfeature.util.parseKeyValueUi
import lv.lvrtc.corelogic.model.DocumentIdentifier
import lv.lvrtc.corelogic.model.toDocumentIdentifier
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.uilogic.components.InfoTextWithNameAndImageData
import lv.lvrtc.uilogic.components.InfoTextWithNameAndValueData
import lv.lvrtc.resourceslogic.R

object DocumentDetailsTransformer {

    fun transformToUiItem(
        document: IssuedDocument,
        resourceProvider: ResourceProvider,
    ): DocumentUi? {

        val documentIdentifierUi = document.toDocumentIdentifier()

        val detailsItems = document.data.claims
            .map { claim ->
                transformToDocumentDetailsUi(
                    displayKey = claim.metadata?.display?.firstOrNull {
                        resourceProvider.getLocale().compareLocaleLanguage(it.locale)
                    }?.name ?: claim.metadata?.display?.firstOrNull()?.name,
                    key = claim.identifier,
                    item = claim.value ?: "",
                    resourceProvider = resourceProvider
                )
            }

        val documentImage = extractValueFromDocumentOrEmpty(
            document = document,
            key = DocumentJsonKeys.PORTRAIT
        )

        val documentExpirationDate = getDocumentExpiryDate(document)

        val docHasExpired = documentHasExpired(documentExpirationDate)

        val displayNumber = DocumentFieldExtractor.extractDisplayNumber(documentIdentifierUi, document)
        val description = DocumentFieldExtractor.extractDescription(documentIdentifierUi, document)
        val additionalInfo = DocumentFieldExtractor.extractAdditionalInfo(documentIdentifierUi, document)

        val issuingAuthority = when (documentIdentifierUi) {
            is DocumentIdentifier.SdJwtA2Pay -> "SEB"
            else -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.ISSUING_AUTHORITY)
        }

        return DocumentUi(
            documentId = document.id,
            documentName = mapDocumentName(document.name),
            documentIdentifier = documentIdentifierUi,
            documentExpirationDateFormatted = documentExpirationDate.toDateFormatted().orEmpty(),
            documentExpirationDate = documentExpirationDate,
            documentHasExpired = docHasExpired,
            documentImage = documentImage,
            documentDetails = detailsItems,
            userFullName = extractFullNameFromDocumentOrEmpty(document),
            documentIssuanceState = DocumentUiIssuanceState.Issued,
            issuingAuthority = issuingAuthority,
            issuerCountry = getDocumentIssuerCountry(document),
            issuanceDate = getDocumentIssuanceDate(document),
            displayNumber = displayNumber,
            description = description,
            additionalInfo = additionalInfo,
            documentIsBookmarked = false
            )
    }

}

private fun transformToDocumentDetailsUi(
    key: String,
    displayKey: String?,
    item: Any,
    resourceProvider: ResourceProvider
): DocumentDetailsUi {

    val values = StringBuilder()
    val localizedKey = displayKey ?: key


    parseKeyValueUi(
        item = item,
        groupIdentifier = localizedKey,
        groupIdentifierKey = key,
        resourceProvider = resourceProvider,
        allItems = values
    )
    val groupedValues = values.toString()

    return when (key) {

        DocumentJsonKeys.SIGNATURE -> {
            DocumentDetailsUi.SignatureItem(
                itemData = InfoTextWithNameAndImageData(
                    identifier = key,
                    title = localizedKey,
                    base64Image = groupedValues
                )
            )
        }

        DocumentJsonKeys.PORTRAIT -> {
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData.create(
                    identifier = key,
                    title = localizedKey,
                    infoValues = arrayOf(resourceProvider.getString(R.string.document_details_portrait_readable_identifier))
                )
            )
        }

        DocumentJsonKeys.DRIVING_PRIVILEGES -> {
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData.create(
                    identifier = key,
                    title = localizedKey,
                    infoValues = arrayOf(extractDrivingPrivileges(item))
                )
            )
        }

        else -> {
            DocumentDetailsUi.DefaultItem(
                itemData = InfoTextWithNameAndValueData.create(
                    identifier = key,
                    title = localizedKey,
                    infoValues = arrayOf(groupedValues)
                )
            )
        }
    }
}

data class VehicleCategory(
    val vehicleCategoryCode: String,
    val issueDate: String,
    val expiryDate: String,
    val restrictions: List<Restriction>
)

data class Restriction(
    val sign: String,
    val value: String
)

private fun mapDocumentName(rawName: String?): String =
    if (rawName == "eu.europa.ec.eudi.iban") "IBAN" else rawName.orEmpty()

private fun getDocumentIssuanceDate(document: IssuedDocument): String {
    val possibleKeys = listOf(
        DocumentJsonKeys.ISSUANCE_DATE,    // PID
        DocumentJsonKeys.ISSUE_DATE,   // MDL
        DocumentJsonKeys.DIPLOMA_ISSUANCE_DATE,  // Diploma,
        DocumentJsonKeys.SIGNING_ISSUANCE_DATE,
        DocumentJsonKeys.JWT_ISSUED_DATE
    )

    return possibleKeys.firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(
            document = document,
            key = key
        )
        val formatted = convertAnyToFormattedDate(value)
        if (!formatted.isNullOrEmpty()) formatted else null
    } ?: ""
}

private fun getDocumentExpiryDate(document: IssuedDocument): String {
    val possibleKeys = listOf(
        DocumentJsonKeys.EXPIRY_DATE,    // PID
        DocumentJsonKeys.SIGNING_EXPIRY_DATE,   // SIGNING
        DocumentJsonKeys.JWT_EXPIRTY_DATE
    )

    return possibleKeys.firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(
            document = document,
            key = key
        )
        val formatted = convertAnyToFormattedDate(value)
        if (!formatted.isNullOrEmpty()) formatted else null
    } ?: ""
}

private fun getDocumentIssuerCountry(document: IssuedDocument): String {
    val possibleKeys = listOf(
        DocumentJsonKeys.ISSUER_COUNTRY,      // Standard
        DocumentJsonKeys.DIPLOMA_ISSUER_COUNTRY // Diploma
    )

    return possibleKeys.firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(
            document = document,
            key = key
        )
        if (value.isNotEmpty()) value else null
    } ?: ""
}