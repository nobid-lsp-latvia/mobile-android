// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.request.transformer

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.DocItem
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import lv.lvrtc.commonfeature.features.request.model.DocumentPayloadDomain
import lv.lvrtc.commonfeature.features.request.model.DomainDocumentFormat
import lv.lvrtc.commonfeature.util.docNamespace
import lv.lvrtc.commonfeature.util.transformPathsToDomainClaims
import lv.lvrtc.corelogic.model.ClaimPath
import lv.lvrtc.corelogic.model.DomainClaim
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import kotlin.collections.mapNotNull

object RequestTransformer {

    fun transformToDomainItems(
        storageDocuments: List<IssuedDocument>,
        resourceProvider: ResourceProvider,
        requestDocuments: List<RequestedDocument>,
    ): Result<List<DocumentPayloadDomain>> = runCatching {
        val resultList = mutableListOf<DocumentPayloadDomain>()

        requestDocuments.forEach { requestDocument ->
            val storageDocument =
                storageDocuments.first { it.id == requestDocument.documentId }

            val pathsWithIntent = requestDocument.requestedItems.map { (docItem, intentToRetain) ->
                docItem.toClaimPath() to intentToRetain
            }.toMap()

            val domainClaims = transformPathsToDomainClaims(
                pathsWithIntent = pathsWithIntent,
                claims = storageDocument.data.claims,
                metadata = storageDocument.metadata,
                resourceProvider = resourceProvider,
            )

            if (domainClaims.isNotEmpty()) {
                resultList.add(
                    DocumentPayloadDomain(
                        docName = storageDocument.name,
                        docId = storageDocument.id,
                        domainDocFormat = DomainDocumentFormat.getFormat(
                            format = storageDocument.format,
                            namespace = storageDocument.docNamespace
                        ),
                        docClaimsDomain = domainClaims
                    )
                )
            }
        }

        resultList
    }

    fun transformToPresentationItems(
        documentsDomain: List<DocumentPayloadDomain>,
        resourceProvider: ResourceProvider,
    ): List<PresentationDocument> {
        return documentsDomain.map { domainPayload ->
            PresentationDocument(
                docId = domainPayload.docId,
                docName = domainPayload.docName,
                domainDocFormat = domainPayload.domainDocFormat,
                claims = domainPayload.docClaimsDomain.flatMap { domainClaim ->
                    domainClaim.flattenToPresentationClaims(domainPayload.docId, resourceProvider)
                }
            )
        }
    }

    fun createDisclosedDocuments(items: List<PresentationDocument>): DisclosedDocuments {
        val disclosedDocuments = items.mapNotNull { presentationDoc ->
            val selectedClaims = presentationDoc.claims.filter { it.isChecked }

            if (selectedClaims.isEmpty()) {
                return@mapNotNull null
            }

            val disclosedItems = selectedClaims.map { claim ->
                when (presentationDoc.domainDocFormat) {
                    is DomainDocumentFormat.SdJwtVc -> SdJwtVcItem(
                        path = claim.path.value
                    )
                    is DomainDocumentFormat.MsoMdoc -> MsoMdocItem(
                        namespace = presentationDoc.domainDocFormat.namespace,
                        elementIdentifier = claim.elementIdentifier ?: claim.path.toElementIdentifier()
                    )
                }
            }

            DisclosedDocument(
                documentId = presentationDoc.docId,
                disclosedItems = disclosedItems,
                keyUnlockData = null
            )
        }
        return DisclosedDocuments(disclosedDocuments)
    }
}

private fun ClaimPath.toElementIdentifier(): String = this.value.firstOrNull() ?: ""

fun DocItem.toClaimPath(): ClaimPath {
    return when (this) {
        is MsoMdocItem -> return ClaimPath(listOf(this.elementIdentifier))
        is SdJwtVcItem -> return ClaimPath(this.path)
        else -> ClaimPath(emptyList())
    }
}

private fun DomainClaim.flattenToPresentationClaims(docId: String, resourceProvider: ResourceProvider): List<PresentationClaim> {
    return when (this) {
        is DomainClaim.Group -> {
            items.flatMap { it.flattenToPresentationClaims(docId, resourceProvider) }
        }
        is DomainClaim.Primitive -> {
            listOf(
                PresentationClaim(
                    id = path.toId(docId),
                    displayTitle = displayTitle,
                    value = value.toString(),
                    isRequired = isRequired,
                    path = path,
                    isChecked = true,
                    isEnabled = !isRequired,
                    elementIdentifier = if (this.path.value.isNotEmpty()) this.path.value.first() else null,
                    intentToRetain = this.intentToRetain
                )
            )
        }
    }
}

data class PresentationDocument(
    val docId: DocumentId,
    val docName: String,
    val domainDocFormat: DomainDocumentFormat,
    val claims: List<PresentationClaim>
)

data class PresentationClaim(
    val id: String,
    val displayTitle: String,
    val value: String,
    val isRequired: Boolean,
    val path: ClaimPath,
    var isChecked: Boolean,
    val isEnabled: Boolean,
    val elementIdentifier: String?,
    val intentToRetain: Boolean
)