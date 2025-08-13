// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.storagelogic.service

import io.realm.kotlin.Realm
import lv.lvrtc.storagelogic.config.StorageConfig

interface RealmService {
    fun get(): Realm
    fun close()
    fun reset()
}

internal class RealmServiceImpl(private val storageConfig: StorageConfig) : RealmService {
    private var realm: Realm? = null

    override fun get(): Realm {
        if (realm == null || realm?.isClosed() == true) {
            try {
                realm = Realm.open(storageConfig.realmConfiguration)
            } catch (e: Exception) {
                if (e.message?.contains("Decryption failed") == true) {
                    // Recovery: Delete corrupt database and create new
                    storageConfig.deleteRealmDatabase()
                    realm = Realm.open(storageConfig.realmConfiguration)
                } else throw e
            }
        }
        return realm!!
    }

    override fun close() {
        realm?.close()
    }

    override fun reset() {
        close()
        realm = null
    }
}