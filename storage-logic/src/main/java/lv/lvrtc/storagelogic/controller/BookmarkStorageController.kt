// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.controller

import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import lv.lvrtc.storagelogic.controller.type.StorageController
import lv.lvrtc.storagelogic.model.Bookmark
import lv.lvrtc.storagelogic.model.RealmBookmark
import lv.lvrtc.storagelogic.model.toBookmark
import lv.lvrtc.storagelogic.model.toBookmarks
import lv.lvrtc.storagelogic.model.toRealm
import lv.lvrtc.storagelogic.service.RealmService

interface BookmarkStorageController : StorageController<Bookmark>

class BookmarkStorageControllerImpl(
    private val realmService: RealmService,
) : BookmarkStorageController {

    override suspend fun store(value: Bookmark) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm())
        }
    }

    override suspend fun update(value: Bookmark) {
        realmService.get().writeBlocking {
            copyToRealm(value.toRealm(), updatePolicy = UpdatePolicy.ALL)
        }
    }

    override suspend fun store(values: List<Bookmark>) {
        realmService.get().writeBlocking {
            values.map { copyToRealm(it.toRealm()) }
        }
    }

    override suspend fun retrieve(identifier: String): Bookmark? {
        return retrieve("identifier == $0", identifier)
    }

    override suspend fun retrieve(query: String, vararg args: Any?): Bookmark? {
        return realmService.get().query<RealmBookmark>(query, *args)
            .find()
            .firstOrNull()
            .toBookmark()
    }

    override suspend fun retrieveAll(documentId: String?): List<Bookmark> {
        return realmService.get().query<RealmBookmark>().find().toBookmarks()
    }

    override suspend fun delete(identifier: String) {
        realmService.get().apply {
            query<RealmBookmark>("identifier == $0", identifier)
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
            val allValues = query<RealmBookmark>().find()
            delete(allValues)
        }
    }
}