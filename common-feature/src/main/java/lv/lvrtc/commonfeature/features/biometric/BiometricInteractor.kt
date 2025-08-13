// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.biometric

import android.content.Context
import lv.lvrtc.authlogic.controller.auth.BiometricAuthController
import lv.lvrtc.authlogic.controller.auth.BiometricsAuthenticate
import lv.lvrtc.authlogic.controller.auth.BiometricsAvailability
import lv.lvrtc.authlogic.controller.storage.BiometryStorageController

interface BiometricInteractor {
    fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit)
    fun getBiometricUserSelection(): Boolean
    fun storeBiometricsUsageDecision(shouldUseBiometrics: Boolean)
    fun authenticateWithBiometrics(
        context: Context,
        notifyOnAuthenticationFailure: Boolean,
        listener: (BiometricsAuthenticate) -> Unit
    )
    fun launchBiometricSystemScreen()
}

class BiometricInteractorImpl(
    private val biometryStorageController: BiometryStorageController,
    private val biometricAuthenticationController: BiometricAuthController,
) : BiometricInteractor {

    override fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit) {
        biometricAuthenticationController.deviceSupportsBiometrics(listener)
    }

    override fun getBiometricUserSelection(): Boolean {
        return biometryStorageController.getUseBiometricsAuth()
    }

    override fun storeBiometricsUsageDecision(shouldUseBiometrics: Boolean) {
        biometryStorageController.setUseBiometricsAuth(shouldUseBiometrics)
    }

    override fun authenticateWithBiometrics(
        context: Context,
        notifyOnAuthenticationFailure: Boolean,
        listener: (BiometricsAuthenticate) -> Unit
    ) {
        biometricAuthenticationController.authenticate(
            context,
            notifyOnAuthenticationFailure,
            listener
        )
    }

    override fun launchBiometricSystemScreen() {
        biometricAuthenticationController.launchBiometricSystemScreen()
    }
}