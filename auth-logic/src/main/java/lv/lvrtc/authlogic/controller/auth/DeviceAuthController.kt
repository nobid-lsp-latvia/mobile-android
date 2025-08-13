// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.controller.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.model.BiometricCrypto
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.resourceslogic.R

interface DeviceAuthController {
    fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit)
    fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        result: DeviceAuthenticationResult
    )

    fun launchBiometricSystemScreen()
}

class DeviceAuthControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val biometricAuthController: BiometricAuthController
) : DeviceAuthController {

    override fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit) {
        biometricAuthController.deviceSupportsBiometrics(listener)
    }

    override fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        result: DeviceAuthenticationResult
    ) {
        (context as? FragmentActivity)?.let { activity ->

            activity.lifecycleScope.launch {

                val biometricManager = BiometricManager.from(resourceProvider.provideContext())
                val hasBiometrics =
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
                val hasDeviceCredential =
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                val promptTitle = if (!hasBiometrics && hasDeviceCredential) {
                    resourceProvider.getString(R.string.device_credential_prompt_title)
                } else {
                    resourceProvider.getString(R.string.biometric_prompt_title)
                }
                val promptSubtitle = if (!hasBiometrics && hasDeviceCredential) {
                    resourceProvider.getString(R.string.device_credential_prompt_subtitle)
                } else {
                    resourceProvider.getString(R.string.biometric_prompt_subtitle)
                }

                val data = biometricAuthController.authenticate(
                    activity = activity,
                    biometryCrypto = biometryCrypto,
                    promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(promptTitle)
                        .setSubtitle(promptSubtitle)
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build(),
                    notifyOnAuthenticationFailure = notifyOnAuthenticationFailure
                )

                if (data.authenticationResult != null) {
                    result.onAuthenticationSuccess()
                } else if (data.hasError) {
                    result.onAuthenticationError()
                } else {
                    result.onAuthenticationFailure()
                }
            }
        }
    }

    override fun launchBiometricSystemScreen() {
        biometricAuthController.launchBiometricSystemScreen()
    }
}

data class DeviceAuthenticationResult(
    val onAuthenticationSuccess: () -> Unit = {},
    val onAuthenticationError: () -> Unit = {},
    val onAuthenticationFailure: () -> Unit = {},
)