// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.storage

import com.google.gson.Gson
import lv.lvrtc.authlogic.model.BiometricAuth
import lv.lvrtc.businesslogic.controller.PrefsController
import lv.lvrtc.authlogic.provider.BiometryStorageProvider

private object BiometryPrefsKeys {
    const val BIOMETRIC_AUTH = "biometric_auth"
    const val USE_BIOMETRICS_AUTH = "use_biometrics_auth"
}

class PrefsBiometryStorageProvider(
    private val prefsController: PrefsController
) : BiometryStorageProvider {

    override fun getBiometricAuth(): BiometricAuth? {
        return try {
            Gson().fromJson(
                prefsController.getString(BiometryPrefsKeys.BIOMETRIC_AUTH, ""),
                BiometricAuth::class.java
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun setBiometricAuth(value: BiometricAuth?) {
        if (value == null) prefsController.clear(BiometryPrefsKeys.BIOMETRIC_AUTH)
        prefsController.setString(BiometryPrefsKeys.BIOMETRIC_AUTH, Gson().toJson(value))
    }

    override fun setUseBiometricsAuth(value: Boolean) {
        prefsController.setBool(BiometryPrefsKeys.USE_BIOMETRICS_AUTH, value)
    }

    override fun getUseBiometricsAuth(): Boolean {
        return prefsController.getBool(BiometryPrefsKeys.USE_BIOMETRICS_AUTH, false)
    }
}
