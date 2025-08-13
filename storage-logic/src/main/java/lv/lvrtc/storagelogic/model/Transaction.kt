// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import lv.lvrtc.storagelogic.model.type.StoredObject
import java.util.Date
import java.util.UUID

enum class TransactionType {
    DOCUMENT_ISSUED,
    DOCUMENT_DELETED,
    DOCUMENT_PRESENTED,
    DOCUMENT_SIGNED,
    DOCUMENT_PAYMENT
}

internal class RealmTransaction : RealmObject {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()
    var documentId: String = ""
    var docType: String = ""
    var nameSpace: String = ""
    var timestamp: Long = System.currentTimeMillis()
    var status: String = ""
    var authority: String? = null
    var eventType = ""
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val docType: String,
    val nameSpace: String,
    var timestamp: Long = System.currentTimeMillis(),
    val status: String = "",
    var authority: String? = null,
    val eventType: String = ""
) : StoredObject

internal fun Transaction.toRealm() = RealmTransaction().apply {
    id = this@toRealm.id
    documentId = this@toRealm.documentId
    docType = this@toRealm.docType
    nameSpace = this@toRealm.nameSpace
    timestamp = this@toRealm.timestamp
    status = this@toRealm.status
    authority = this@toRealm.authority
    eventType = this@toRealm.eventType
}

internal fun RealmTransaction?.toTransaction() = this?.let {
    Transaction(
        id = it.id,
        documentId = it.documentId,
        docType = it.docType,
        nameSpace = it.nameSpace,
        timestamp = it.timestamp,
        status = it.status,
        eventType = it.eventType,
        authority = it.authority
    )
}

internal fun List<RealmTransaction>.toTransactions() = this.map {
    it.toTransaction()!!
}
