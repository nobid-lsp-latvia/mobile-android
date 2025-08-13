// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.config

import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.businesslogic.extensions.encodeToPemBase64String
import lv.lvrtc.storagelogic.model.RealmTransaction
import lv.lvrtc.storagelogic.model.RealmBookmark
import java.security.SecureRandom

interface StorageConfig {
    val storageName: String
    val storageVersion: Long
    val realmConfiguration: RealmConfiguration

    fun deleteRealmDatabase()
}

class StorageConfigImpl(
    private val prefKeys: PrefKeys,
) : StorageConfig {

    override val storageName: String
        get() = "lv.lvrtc.edim.storage"

    override val storageVersion: Long
        get() = 1L
    override val realmConfiguration: RealmConfiguration
        get() = RealmConfiguration.Builder(
            schema = setOf(
                RealmTransaction::class,
                RealmBookmark::class
            )
        )
            .name(storageName)
            .schemaVersion(storageVersion)
            .encryptionKey(retrieveOrGenerateStorageKey())
            .deleteRealmIfMigrationNeeded()
            .build()

    private fun retrieveOrGenerateStorageKey(): ByteArray {
        val storedKey = prefKeys.getStorageKey()
        if (storedKey != null) {
            return storedKey
        }
        val key = ByteArray(Realm.ENCRYPTION_KEY_LENGTH)
        SecureRandom().nextBytes(key)
        prefKeys.setStorageKey(key.encodeToPemBase64String().orEmpty())
        return key
    }

    override fun deleteRealmDatabase() {
        try {
            Realm.deleteRealm(realmConfiguration)
        } catch (e: Exception) {
            Log.e("StorageConfig", "Error deleting realm database", e)
        }
    }
}