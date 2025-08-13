// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.security

import lv.lvrtc.businesslogic.controller.crypto.SecureAreaRepository

/**
 * Implementation of [SecureAreaRepository] that delegates cryptographic key management
 * to [SecureAreaController].
 *
 * This class acts as a bridge between business logic and core security operations.
 */
class SecureAreaRepositoryImpl(
    private val secureAreaController: SecureAreaController
) : SecureAreaRepository {

    /**
     * Generates a cryptographic key with attestation by delegating the request
     * to [SecureAreaController].
     *
     * @param nonce A unique, one-time challenge (Base64-encoded) provided by the server.
     * @return A map containing key attestation metadata.
     */
    override suspend fun generateKeyWithAttestation(nonce: String): Map<String, String?> {
        return secureAreaController.generateKeyWithAttestation(nonce)
    }

    override fun checkInstance(): Boolean {
        return secureAreaController.checkInstance()
    }

    override fun deleteWalletInstance() {
        return secureAreaController.deleteKey()
    }


}