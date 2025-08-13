// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.util

import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import lv.lvrtc.businesslogic.extensions.decodeFromBase64
import lv.lvrtc.businesslogic.util.safeLet
import lv.lvrtc.businesslogic.util.toDateFormatted
import lv.lvrtc.businesslogic.util.toLocalDate
import lv.lvrtc.corelogic.extension.getLocalizedClaimName
import lv.lvrtc.corelogic.extension.removeEmptyGroups
import lv.lvrtc.corelogic.extension.sortRecursivelyBy
import lv.lvrtc.corelogic.model.ClaimPath
import lv.lvrtc.corelogic.model.DomainClaim
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import java.time.LocalDate
import lv.lvrtc.resourceslogic.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun extractValueFromDocumentOrEmpty(
    document: IssuedDocument,
    key: String
): String {
    return document.data.claims
        .firstOrNull { it.identifier == key }
        ?.value
        ?.toString()
        ?: ""
}

/**
 * Converts any supported date representation (timestamp or date string) to yyyy-MM-dd HH:mm format.
 */
fun convertAnyToFormattedDate(value: Any?): String? {
    // 1. Try as Unix timestamp (seconds since epoch)
    val timestamp = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
    if (timestamp != null) {
        val instant = Instant.ofEpochSecond(timestamp)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    // 2. Try as date string (ISO or localized)
    if (value is String) {
        val localDate: LocalDate? = value.toLocalDate()
        if (localDate != null) {
            val localDateTime = localDate.atStartOfDay()
            return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }
    }

    return value?.toString()
}

fun extractFullNameFromDocumentOrEmpty(document: IssuedDocument): String {
    // Try standard and Diploma first name formats
    val firstName = listOf(
        DocumentJsonKeys.FIRST_NAME,      // "given_name"
        DocumentJsonKeys.DIPLOMA_FIRST_NAME    // "givenName"
    ).firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(document, key)
        if (value.isNotEmpty()) value else null
    } ?: ""

    // Try standard and Diploma last name formats
    val lastName = listOf(
        DocumentJsonKeys.LAST_NAME,       // "family_name"
        DocumentJsonKeys.DIPLOMA_LAST_NAME     // "familyName"
    ).firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(document, key)
        if (value.isNotEmpty()) value else null
    } ?: ""

    val fullName = when {
        firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
        firstName.isNotBlank() -> firstName
        lastName.isNotBlank() -> lastName
        else -> ""
    }
    return fullName
}

fun extractFirstNameFromDocumentOrEmpty(document: IssuedDocument): String {
    // Try standard and Diploma first name formats
    val firstName = listOf(
        DocumentJsonKeys.FIRST_NAME,      // "given_name"
        DocumentJsonKeys.DIPLOMA_FIRST_NAME    // "givenName"
    ).firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(document, key)
        if (value.isNotEmpty()) value else null
    } ?: ""

    return firstName
}

fun keyIsBase64(key: String): Boolean {
    val listOfBase64Keys = DocumentJsonKeys.BASE64_IMAGE_KEYS
    return listOfBase64Keys.contains(key)
}

private fun keyIsUserPseudonym(key: String): Boolean {
    return key == DocumentJsonKeys.USER_PSEUDONYM
}

private fun keyIsGender(key: String): Boolean {
    val listOfGenderKeys = DocumentJsonKeys.GENDER_KEYS
    return listOfGenderKeys.contains(key)
}

private fun getGenderValue(value: String, resourceProvider: ResourceProvider): String =
    when (value) {
        "1" -> {
            resourceProvider.getString(R.string.request_gender_male)
        }

        "2" -> {
            resourceProvider.getString(R.string.request_gender_female)
        }

        else -> {
            value
        }
    }

private val SD_JWT_TIMESTAMP_FIELDS = setOf("iat", "exp", "nbf")

private fun convertTimestampToDate(timestamp: Any?): String? {
    return try {
        val timestampLong = when (timestamp) {
            is Number -> timestamp.toLong()
            is String -> timestamp.toLongOrNull()
            else -> null
        } ?: return null

        val instant = Instant.ofEpochSecond(timestampLong)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: Exception) {
        null
    }
}

fun parseKeyValueUi(
    item: Any,
    groupIdentifier: String,
    groupIdentifierKey: String,
    keyIdentifier: String = "",
    resourceProvider: ResourceProvider,
    allItems: StringBuilder
) {
    when (item) {

        is Map<*, *> -> {
            item.forEach { (key, value) ->
                safeLet(key as? String, value) { key, value ->
                    parseKeyValueUi(
                        item = value,
                        groupIdentifier = groupIdentifier,
                        groupIdentifierKey = groupIdentifierKey,
                        keyIdentifier = key,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Collection<*> -> {
            item.forEach { value ->
                value?.let {
                    parseKeyValueUi(
                        item = it,
                        groupIdentifier = groupIdentifier,
                        groupIdentifierKey = groupIdentifierKey,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Boolean -> {
            allItems.append(
                if (item) {
                    "true"
                } else {
                    "false"
                }
            )
        }

        else -> {
            if (groupIdentifierKey in SD_JWT_TIMESTAMP_FIELDS) {
                val formattedDate = convertTimestampToDate(item)
                if (formattedDate != null) {
                    allItems.append(formattedDate)
                    return
                }
            }

            val date: String? = (item as? String)
            allItems.append(
                when {

                    keyIsGender(groupIdentifierKey) -> {
                        getGenderValue(item.toString(), resourceProvider)
                    }

                    keyIsUserPseudonym(groupIdentifierKey) -> {
                        item.toString().decodeFromBase64()
                    }

                    date != null && keyIdentifier.isEmpty() -> {
                        date
                    }

                    else -> {
                        val jsonString = item.toString()
                        if (keyIdentifier.isEmpty()) {
                            jsonString
                        } else {
                            val lineChange = if (allItems.isNotEmpty()) "\n" else ""
                            val value = jsonString ?: jsonString
                            "$lineChange$keyIdentifier: $value"
                        }
                    }
                }
            )
        }
    }
}

fun documentHasExpired(
    documentExpirationDate: String,
    currentDate: LocalDate = LocalDate.now(),
): Boolean {
    val localDateOfDocumentExpirationDate = documentExpirationDate.toLocalDate()

    return localDateOfDocumentExpirationDate?.let {
        currentDate.isAfter(it)
    } ?: false
}

fun extractDrivingPrivileges(drivingPrivileges: Any?): String {
    return try {
        @Suppress("UNCHECKED_CAST")
        (drivingPrivileges as? List<Map<String, Any>>)?.mapNotNull { category ->
            category["vehicle_category_code"] as? String
        }?.joinToString(", ") ?: ""
    } catch (_: Exception) {
        ""
    }
}

val IssuedDocument.docNamespace: NameSpace?
    get() = when (val data = this.data) {
        is MsoMdocData -> data.nameSpaces.keys.first()
        is SdJwtVcData -> null
    }

fun transformPathsToDomainClaims(
    pathsWithIntent: Map<ClaimPath, Boolean>,
    claims: List<DocumentClaim>,
    metadata: DocumentMetaData?,
    resourceProvider: ResourceProvider,
): List<DomainClaim> {
    return pathsWithIntent.entries.fold<Map.Entry<ClaimPath, Boolean>, List<DomainClaim>>(
        initial = emptyList()
    ) { acc, (path, intentToRetain) ->
        insertPath(
            tree = acc,
            path = path,
            disclosurePath = path,
            claims = claims,
            metadata = metadata,
            resourceProvider = resourceProvider,
            intentToRetain = intentToRetain
        )
    }.removeEmptyGroups()
        .sortRecursivelyBy {
            it.displayTitle.lowercase()
        }
}

private fun insertPath(
    tree: List<DomainClaim>,
    path: ClaimPath,
    disclosurePath: ClaimPath,
    claims: List<DocumentClaim>,
    metadata: DocumentMetaData?,
    resourceProvider: ResourceProvider,
    intentToRetain: Boolean
): List<DomainClaim> {
    if (path.value.isEmpty()) return tree

    val userLocale = resourceProvider.getLocale()

    val key = path.value.first()

    val existingNode = tree.find { it.key == key }

    val currentClaim: DocumentClaim? = claims.find { it.identifier == key }

    return if (path.value.size == 1) {
        // Leaf node (Primitive or Nested Structure)
        if (existingNode == null && currentClaim != null) {
            val accumulatedClaims: MutableList<DomainClaim> = mutableListOf()
            createKeyValue(
                item = currentClaim.value!!,
                groupKey = currentClaim.identifier,
                resourceProvider = resourceProvider,
                metadata = metadata,
                disclosurePath = disclosurePath,
                allItems = accumulatedClaims,
                intentToRetain = intentToRetain
            )
            tree + accumulatedClaims
        } else {
            tree // Already exists or not available, return unchanged
        }
    } else {
        // Group node (Intermediate)
        val childClaims =
            (claims.find { key == it.identifier } as? SdJwtVcClaim)?.children ?: claims
        val updatedNode = if (existingNode is DomainClaim.Group) {
            // Update existing group by inserting the next path segment into its items
            existingNode.copy(
                items = insertPath(
                    tree = existingNode.items,
                    path = ClaimPath(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    metadata = metadata,
                    resourceProvider = resourceProvider,
                    intentToRetain = intentToRetain
                )
            )
        } else {
            // Create a new group and insert the next path segment
            DomainClaim.Group(
                key = currentClaim?.identifier ?: key,
                displayTitle = getReadableNameFromIdentifier(
                    metadata = metadata,
                    userLocale = userLocale,
                    identifier = currentClaim?.identifier ?: key
                ),
                path = ClaimPath(disclosurePath.value.take((disclosurePath.value.size - path.value.size) + 1)),
                items = insertPath(
                    tree = emptyList(),
                    path = ClaimPath(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    metadata = metadata,
                    resourceProvider = resourceProvider,
                    intentToRetain = intentToRetain
                )
            )
        }

        tree.filter { it.key != key } + updatedNode
    }
}

fun getReadableNameFromIdentifier(
    metadata: DocumentMetaData?,
    userLocale: Locale,
    identifier: String,
): String {
    return metadata?.claims
        ?.find { it.name.name == identifier }
        ?.display.getLocalizedClaimName(
            userLocale = userLocale,
            fallback = identifier
        )
}

@OptIn(ExperimentalUuidApi::class)
fun createKeyValue(
    item: Any,
    groupKey: String,
    childKey: String = "",
    disclosurePath: ClaimPath,
    resourceProvider: ResourceProvider,
    metadata: DocumentMetaData?,
    allItems: MutableList<DomainClaim>,
    intentToRetain: Boolean,
) {
    when (item) {

        is Map<*, *> -> {
            item.forEach { (key, value) ->
                safeLet(key as? String, value) { key, value ->
                    val newGroupKey = if (value is Collection<*>) key else groupKey
                    val newChildKey = if (value is Collection<*>) "" else key
                    createKeyValue(
                        item = value,
                        groupKey = newGroupKey,
                        childKey = newChildKey,
                        disclosurePath = disclosurePath,
                        resourceProvider = resourceProvider,
                        metadata = metadata,
                        allItems = allItems,
                        intentToRetain = intentToRetain
                    )
                }
            }
        }

        is Collection<*> -> {

            val children: MutableList<DomainClaim> = mutableListOf()

            item.forEach { value ->
                value?.let {
                    createKeyValue(
                        item = it,
                        groupKey = groupKey,
                        disclosurePath = disclosurePath,
                        resourceProvider = resourceProvider,
                        metadata = metadata,
                        allItems = children,
                        intentToRetain = intentToRetain
                    )
                }
            }

            if (childKey.isEmpty()) {
                allItems.add(
                    DomainClaim.Group(
                        key = groupKey,
                        displayTitle = getReadableNameFromIdentifier(
                            metadata = metadata,
                            userLocale = resourceProvider.getLocale(),
                            identifier = groupKey
                        ),
                        path = ClaimPath(listOf(Uuid.random().toString())),
                        items = children
                    )
                )
            } else {
                allItems.addAll(children)
            }
        }

        else -> {

            val date: String? = (item as? String)?.toDateFormatted()

            val formattedValue = when {
                keyIsGender(groupKey) -> getGenderValue(item.toString(), resourceProvider)
                keyIsUserPseudonym(groupKey) -> item.toString().decodeFromBase64()
                date != null -> date
                item is Boolean -> if (item) "true" else "false"

                else -> item.toString()
            }

            allItems.add(
                DomainClaim.Primitive(
                    key = childKey.ifEmpty { groupKey },
                    displayTitle = getReadableNameFromIdentifier(
                        metadata = metadata,
                        userLocale = resourceProvider.getLocale(),
                        identifier = childKey.ifEmpty { groupKey }
                    ),
                    path = disclosurePath,
                    isRequired = intentToRetain,
                    value = formattedValue,
                    intentToRetain = intentToRetain
                )
            )
        }
    }
}