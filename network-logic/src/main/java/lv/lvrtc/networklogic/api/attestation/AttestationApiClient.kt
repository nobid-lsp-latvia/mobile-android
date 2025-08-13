// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.attestation

import android.util.Log
import lv.lvrtc.businesslogic.controller.crypto.SecureAreaRepository
import lv.lvrtc.networklogic.api.base.BaseApiClient
import lv.lvrtc.networklogic.error.ApiErrorHandler
import lv.lvrtc.networklogic.model.attestation.InstanceRq
import lv.lvrtc.networklogic.model.attestation.NonceResp

interface AttestationApiClient {
    suspend fun getNonce(): Result<NonceResp>
    suspend fun getInstance(nonce: String): Result<Unit>
    fun checkInstance(): Boolean
}

class AttestationApiClientImpl(
    private val attestationApi: AttestationApi,
    private val errorHandler: ApiErrorHandler,
    private val secureAreaRepository: SecureAreaRepository
) : BaseApiClient(errorHandler), AttestationApiClient {

    companion object {
        private const val TAG = "AttestationApiClient"
    }

    override suspend fun getNonce(): Result<NonceResp> =
        handleRequest { attestationApi.getNonce() }

    override suspend fun getInstance(nonce: String): Result<Unit> =
        handleRequest {
            try {
                val attestationData = secureAreaRepository.generateKeyWithAttestation(nonce)
                val instanceRequest = attestationData.toInstanceRequest()
                    ?: throw IllegalStateException("Invalid attestation request: Missing required fields")

                attestationApi.getInstance(instanceRequest)
            } catch (e: Exception) {
                // If any exception happens before API call, delete wallet instance
                secureAreaRepository.deleteWalletInstance()
                throw e
            }
        }.onFailure {
            // Ensures wallet is deleted if the API request fails
            secureAreaRepository.deleteWalletInstance()
        }

    override fun checkInstance(): Boolean {
        return secureAreaRepository.checkInstance()
    }

    /**
     * Converts attestation data to [InstanceRq], ensuring all required fields are present.
     */
    private fun Map<String, String?>.toInstanceRequest(): InstanceRq? {
        val challenge = this["challenge"]
        val keyAttestation = this["key_attestation"]
        val hardwareKeyTag = this["hardware_key_tag"]

        return if (!challenge.isNullOrEmpty() && !keyAttestation.isNullOrEmpty() && !hardwareKeyTag.isNullOrEmpty()) {
            InstanceRq(challenge, keyAttestation, hardwareKeyTag)
        } else {
            Log.e(TAG, "Error: Missing required fields in attestation request")
            null
        }
    }
}