// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.util.securearea

import android.util.Base64
import android.util.Log
import com.android.identity.cbor.Bstr
import com.android.identity.cbor.Cbor
import com.android.identity.cbor.CborMap
import com.android.identity.cbor.Nint
import com.android.identity.cbor.Tstr
import com.android.identity.crypto.EcSignature
import com.android.identity.securearea.KeyInfo
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object AttestationEncoder {

    /**
     * Generates a complete attestation CBOR object, including signature and algorithm.
     *
     * @param keyInfo The key information containing attestation details.
     * @param signature The signature generated over the challenge.
     * @return Base64 encoded CBOR attestation.
     */
    fun getAttestationCborEncoded(keyInfo: KeyInfo, signature: EcSignature): String? {
        return try {
            val x5cArray = keyInfo.attestation.certChain?.toDataItem() ?: return null

            val attStmtMap = CborMap(mutableMapOf(
                Tstr("alg") to Nint(7u),
                Tstr("sig") to Bstr(signature.toCoseEncoded()),
                Tstr("x5c") to x5cArray
            ))

            val cborMap = CborMap(mutableMapOf(
                Tstr("fmt") to Tstr("android-key"),
                Tstr("attStmt") to attStmtMap
            ))

            val cborBytes = Cbor.encode(cborMap)
            Base64.encodeToString(cborBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
