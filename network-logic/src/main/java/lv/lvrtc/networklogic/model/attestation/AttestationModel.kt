// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.model.attestation

data class InstanceRq(
    val challenge: String,
    val key_attestation: String,
    val hardware_key_tag: String
)

data class NonceResp(
    val c_nonce: String
)