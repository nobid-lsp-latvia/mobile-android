// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.uilogic.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import lv.lvrtc.uilogic.NobidComponentActivity

fun Context.cacheDeepLink(uri: Uri) {
    val intent = Intent().apply {
        data = uri
    }
    (this as? NobidComponentActivity)?.cacheDeepLink(intent)
}

fun Context.finish() {
    (this as? NobidComponentActivity)?.finish()
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.getPendingDeepLink(): Uri? {
    return (this as? NobidComponentActivity)?.pendingDeepLink?.let { deepLink ->
        clearPendingDeepLink()
        deepLink
    }
}

private fun Context.clearPendingDeepLink() {
    (this as? NobidComponentActivity)?.pendingDeepLink = null
}

fun Context.openUrl(uri: Uri) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
    }
}