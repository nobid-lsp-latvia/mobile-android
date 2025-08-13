// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.businesslogic.controller

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import lv.lvrtc.businesslogic.extensions.decodeFromPemBase64String

interface PrefsController {
    fun contains(key: String): Boolean
    fun clear(key: String)
    fun clearAll()

    fun setString(key: String, value: String)
    fun setLong(key: String, value: Long)
    fun setBool(key: String, value: Boolean)
    fun setInt(key: String, value: Int)

    fun getString(key: String, defaultValue: String): String
    fun getLong(key: String, defaultValue: Long): Long
    fun getBool(key: String, defaultValue: Boolean): Boolean
    fun getInt(key: String, defaultValue: Int): Int
}

class PrefsControllerImpl(private val context: Context) : PrefsController {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefsKeyEncryptionScheme by lazy {
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
    }

    private val prefsValueEncryptionScheme by lazy {
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    }

    private fun getSharedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            prefsKeyEncryptionScheme,
            prefsValueEncryptionScheme
        )
    }

    override fun contains(key: String): Boolean {
        return getSharedPrefs().contains(key)
    }

    override fun clear(key: String) {
        getSharedPrefs().edit().remove(key).apply()
    }

    override fun clearAll() {
        getSharedPrefs().edit().clear().apply()
    }

    override fun setString(key: String, value: String) {
        getSharedPrefs().edit().putString(key, value).apply()
    }

    override fun setLong(key: String, value: Long) {
        getSharedPrefs().edit().putLong(key, value).apply()
    }

    override fun setBool(key: String, value: Boolean) {
        getSharedPrefs().edit().putBoolean(key, value).apply()
    }

    override fun setInt(key: String, value: Int) {
        getSharedPrefs().edit().putInt(key, value).apply()
    }


    override fun getString(key: String, defaultValue: String): String {
        return getSharedPrefs().getString(key, defaultValue) ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return getSharedPrefs().getLong(key, defaultValue)
    }

    override fun getBool(key: String, defaultValue: Boolean): Boolean {
        return getSharedPrefs().getBoolean(key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return getSharedPrefs().getInt(key, defaultValue)
    }
}

interface PrefKeys {
    fun getBiometricKey(): String
    fun setBiometricKey(key: String)
    fun getStorageKey(): ByteArray?
    fun setStorageKey(key: String)
    fun getHardwareKey(): String
    fun setHardwareKey(key: String)
    fun getLastDocType(): String
    fun setLastDocType(key: String)
    fun getAppActivated(): Boolean
    fun setAppActivated(key: Boolean)
    fun setLanguage(language: String)
    fun getLanguage(): String
}

class PrefKeysImpl(
    private val prefsController: PrefsController
) : PrefKeys {

    override fun setBiometricKey(key: String) {
        return prefsController.setString("biometric_key", key)
    }

    override fun getBiometricKey(): String {
        return prefsController.getString("biometric_key", "")
    }

    override fun setStorageKey(key: String) {
        prefsController.setString("StorageKey", key)
    }

    override fun getStorageKey(): ByteArray? {
        val key = prefsController.getString("StorageKey", "")
        return if (key.isNotEmpty()) key.decodeFromPemBase64String() else null
    }

    override fun setHardwareKey(key: String) {
        prefsController.setString("HardwareKey", key)
    }

    override fun getHardwareKey(): String {
        return prefsController.getString("HardwareKey", "")
    }

    override fun setLastDocType(key: String) {
        prefsController.setString("LastDocType", key)
    }

    override fun getLastDocType(): String {
        return prefsController.getString("LastDocType", "")
    }

    override fun getAppActivated(): Boolean {
        return prefsController.getBool("AppActivated", false)
    }

    override fun setAppActivated(key: Boolean) {
        prefsController.setBool("AppActivated", key)
    }

    override fun setLanguage(language: String) {
        prefsController.setString("SelectedLanguage", language)
    }

    override fun getLanguage(): String {
        return prefsController.getString("SelectedLanguage", "")
    }
}