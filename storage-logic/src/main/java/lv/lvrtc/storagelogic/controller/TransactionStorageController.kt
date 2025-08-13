// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.controller

import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import lv.lvrtc.storagelogic.controller.type.StorageController
import lv.lvrtc.storagelogic.model.RealmTransaction
import lv.lvrtc.storagelogic.model.Transaction
import lv.lvrtc.storagelogic.model.toRealm
import lv.lvrtc.storagelogic.model.toTransaction
import lv.lvrtc.storagelogic.model.toTransactions
import lv.lvrtc.storagelogic.service.RealmService

interface TransactionStorageController : StorageController<Transaction>

class TransactionStorageControllerImpl(
    private val realmService: RealmService,
) : TransactionStorageController {

    override suspend fun store(value: Transaction) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm())
        }
    }

    override suspend fun update(value: Transaction) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm(), updatePolicy = UpdatePolicy.ALL)
        }
    }

    override suspend fun store(values: List<Transaction>) {
        realmService.get().writeBlocking {
            values.map { copyToRealm(it.toRealm()) }
        }
    }

    override suspend fun retrieve(identifier: String): Transaction? {
        return retrieve("identifier == $0", identifier)
    }

    override suspend fun retrieve(query: String, vararg args: Any?): Transaction? {
        return realmService.get().query<RealmTransaction>(query, *args)
            .find()
            .firstOrNull()
            .toTransaction()
    }

    override suspend fun retrieveAll(documentId: String?): List<Transaction> {
        documentId?.let {
            return realmService.get().query<RealmTransaction>("documentId == $0", documentId).find().toTransactions()
        }
        return realmService.get().query<RealmTransaction>().find().toTransactions()
    }

    override suspend fun delete(identifier: String) {
        realmService.get().apply {
            query<RealmTransaction>("identifier == $0", identifier)
                .find()
                .firstOrNull()
                ?.let { result ->
                    writeBlocking {
                        findLatest(result)?.also {
                            delete(it)
                        }
                    }
                }
        }
    }

    override suspend fun deleteAll() {
        realmService.get().writeBlocking {
            val allValues = query<RealmTransaction>().find()
            delete(allValues)
        }
    }
}