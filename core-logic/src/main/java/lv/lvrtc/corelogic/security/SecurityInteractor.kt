// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.security

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import java.security.KeyStore

interface SecurityInteractor {
    suspend fun validateDeviceSecurity(): SecurityValidation
}

sealed class SecurityValidation {
    data object Valid : SecurityValidation()
    data class Invalid(val reason: SecurityReason) : SecurityValidation()
}

enum class SecurityReason(val code: String) {
    DEVICE_ROOTED("SEC_001"),
    NO_SCREEN_LOCK("SEC_002"),
    DEVICE_ENCRYPTION_DISABLED("SEC_003"),
    UNSAFE_KEYSTORE("SEC_004"),
}

class SecurityInteractorImpl(
    private val resourceProvider: ResourceProvider
) : SecurityInteractor {

    override suspend fun validateDeviceSecurity(): SecurityValidation {
        val context = resourceProvider.provideContext()

        if (isDeviceRooted(context)) {
            return SecurityValidation.Invalid(SecurityReason.DEVICE_ROOTED)
        }

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguardManager.isDeviceSecure) {
            return SecurityValidation.Invalid(SecurityReason.NO_SCREEN_LOCK)
        }

        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val encryptionStatus = devicePolicyManager.storageEncryptionStatus
        if (encryptionStatus != DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE &&
            encryptionStatus != DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER) {
            return SecurityValidation.Invalid(SecurityReason.DEVICE_ENCRYPTION_DISABLED)
        }

        if (!isKeystoreSecure()) {
            return SecurityValidation.Invalid(SecurityReason.UNSAFE_KEYSTORE)
        }

        return SecurityValidation.Valid
    }

    private fun isDeviceRooted(context: Context): Boolean {
        val rootApps = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.topjohnwu.magisk"
        )

        return rootApps.any { app ->
            try {
                context.packageManager.getPackageInfo(app, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun isKeystoreSecure(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            true
        } catch (e: Exception) {
            false
        }
    }
}