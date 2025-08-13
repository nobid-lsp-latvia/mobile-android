// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.security

import android.util.Log
import com.android.identity.android.securearea.AndroidKeystoreCreateKeySettings
import com.android.identity.crypto.Algorithm
import com.android.identity.crypto.EcCurve
import com.android.identity.securearea.KeyInvalidatedException
import com.android.identity.securearea.KeyPurpose
import com.android.identity.securearea.SecureArea
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.corelogic.util.securearea.AttestationEncoder.getAttestationCborEncoded
import lv.lvrtc.corelogic.util.securearea.ChallengeProcessor
import lv.lvrtc.corelogic.util.securearea.ECCPublicKeyHasher
import lv.lvrtc.corelogic.util.securearea.KeyAliasGenerator


interface SecureAreaController {
    /**
     * Generates a new key with attestation.
     *
     * @param challenge A Base64-encoded challenge from the backend.
     * @return A map containing challenge, attestation, and key alias.
     */
    suspend fun generateKeyWithAttestation(challenge: String): Map<String, String?>

    fun checkInstance(): Boolean

    /**
     * Deletes a key from SecureArea.
     *
     * @param alias The alias of the key.
     */
    fun deleteKey()
}

class SecureAreaControllerImpl(
    private val secureArea: SecureArea,
    private val prefKeys: PrefKeys
) : SecureAreaController {

    override suspend fun generateKeyWithAttestation(challenge: String): Map<String, String?> {
        return try {
            val challengeBytes = ChallengeProcessor.decodeBase64ToSha256(challenge) ?: return emptyMap()

            val alias = KeyAliasGenerator.generate()
            prefKeys.setHardwareKey(alias)

            val createKeySettings = AndroidKeystoreCreateKeySettings.Builder(challengeBytes)
                .setKeyPurposes(setOf(KeyPurpose.SIGN))
                .setEcCurve(EcCurve.P256)
                .setUserAuthenticationRequired(false, 0, emptySet())
                .setUseStrongBox(true)
                .build()

            secureArea.createKey(alias, createKeySettings)
            val keyInfo = secureArea.getKeyInfo(alias)

            val signature = secureArea.sign(
                alias,
                Algorithm.ES256,
                challengeBytes,
                null  // No key unlock data needed for non-user-auth keys
            )

            val attestationCbor = getAttestationCborEncoded(keyInfo, signature)

            val keyHash = ECCPublicKeyHasher.getHash(secureArea, alias)

            return mapOf(
                "challenge" to challenge,
                "key_attestation" to attestationCbor,
                "hardware_key_tag" to keyHash
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    override fun checkInstance(): Boolean {
        val alias = prefKeys.getHardwareKey()
        try {
            secureArea.getKeyInfo(alias)
            return true
        } catch (e: KeyInvalidatedException) {
            Log.e("SecureAreaController","Key $alias was invalidated: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("SecureAreaController","Key $alias does not exist: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


    override fun deleteKey() {
        val alias = prefKeys.getHardwareKey()
        try {
            secureArea.getKeyInfo(alias)
            secureArea.deleteKey(alias)
        } catch (e: IllegalArgumentException) {
            Log.e("SecureAreaController","Key $alias does not exist: ${e.message}")
        } catch (e: KeyInvalidatedException) {
            Log.e("SecureAreaController","Key $alias was invalidated: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
