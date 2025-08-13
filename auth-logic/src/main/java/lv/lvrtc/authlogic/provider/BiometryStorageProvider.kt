// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.provider

import lv.lvrtc.authlogic.model.BiometricAuth

interface BiometryStorageProvider {
    fun getBiometricAuth(): BiometricAuth?
    fun setBiometricAuth(value: BiometricAuth?)
    fun setUseBiometricsAuth(value: Boolean)
    fun getUseBiometricsAuth(): Boolean
}