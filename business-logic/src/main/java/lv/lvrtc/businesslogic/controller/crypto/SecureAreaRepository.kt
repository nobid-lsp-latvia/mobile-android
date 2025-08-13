// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.businesslogic.controller.crypto

/**
 * Interface for handling secure key management and attestation using Secure Area.
 */
interface SecureAreaRepository {

    /**
     * Generates a cryptographic key with attestation.
     *
     * This function:
     * - Creates a new key in the Secure Area.
     * - Generates an attestation statement, which can be verified by a remote server.
     * - Returns key-related metadata required for further authentication.
     *
     * @param nonce A unique, one-time challenge (Base64-encoded) provided by the server.
     * @return A map containing:
     *  - `"challenge"`: The original challenge used for attestation.
     *  - `"key_attestation"`: The CBOR-encoded attestation data.
     *  - `"hardware_key_tag"`: A hash representation of the generated key.
     */
    suspend fun generateKeyWithAttestation(nonce: String): Map<String, String?>

    fun checkInstance(): Boolean

    fun deleteWalletInstance()
}