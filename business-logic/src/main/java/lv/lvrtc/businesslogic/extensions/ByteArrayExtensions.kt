// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.businesslogic.extensions

import android.util.Base64

fun ByteArray.encodeToPemBase64String(): String? {
    val encodedString = Base64.encodeToString(this, Base64.NO_WRAP) ?: return null
    return encodedString.splitToLines(64)
}

fun decodeFromBase64(base64EncodedString: String, flag: Int = Base64.DEFAULT): ByteArray {
    return try {
        Base64.decode(base64EncodedString, flag)
    } catch (e: Exception) {
        ByteArray(size = 0)
    }
}