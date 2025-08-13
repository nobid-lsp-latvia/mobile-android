// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.controller.type

import lv.lvrtc.storagelogic.model.type.StoredObject

interface StorageController<T : StoredObject> {
    suspend fun store(value: T)
    suspend fun store(values: List<T>)
    suspend fun retrieve(identifier: String): T?
    suspend fun retrieve(
        query: String,
        vararg args: Any?,
    ): T?

    suspend fun update(value: T)
    suspend fun retrieveAll(documentId: String?): List<T>
    suspend fun delete(identifier: String)
    suspend fun deleteAll()
}