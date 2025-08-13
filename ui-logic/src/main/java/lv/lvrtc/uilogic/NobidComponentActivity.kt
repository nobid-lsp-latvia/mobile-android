// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.uilogic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.corelogic.util.CoreActions
import lv.lvrtc.resourceslogic.theme.ThemeManager
import lv.lvrtc.uilogic.navigation.DeepLinkAction
import lv.lvrtc.uilogic.navigation.DeepLinkType
import lv.lvrtc.uilogic.navigation.IssuanceScreens
import lv.lvrtc.uilogic.navigation.RouterHost
import lv.lvrtc.uilogic.navigation.handleDeepLinkAction
import lv.lvrtc.uilogic.navigation.hasDeepLink
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.annotation.KoinExperimentalAPI
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.getValue

open class NobidComponentActivity : FragmentActivity() {

    private val routerHost: RouterHost by inject()

    private val prefKeys: PrefKeys by inject()

    private var flowStarted: Boolean = false

    internal var pendingDeepLink: Uri? = null

    internal fun cacheDeepLink(intent: Intent?) {
        pendingDeepLink = intent?.data
    }

    override fun attachBaseContext(newBase: Context) {
        val context = applySavedLocale(newBase)
        super.attachBaseContext(context)
    }

    @OptIn(KoinExperimentalAPI::class)
    @Composable
    protected fun Content(
        intent: Intent?,
        builder: NavGraphBuilder.(NavController) -> Unit
    ) {
        ThemeManager.getInstance().Theme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                KoinAndroidContext {
                    routerHost.StartFlow {
                        builder(it)
                    }
                    flowStarted = true
                    handleDeepLink(intent, coldBoot = true)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        if (flowStarted) {
            handleDeepLink(intent)
        } else {
            runPendingDeepLink(intent)
        }
    }

    private fun runPendingDeepLink(intent: Intent?) {
        lifecycleScope.launch {
            var count = 0
            while (!flowStarted && count <= 10) {
                count++
                delay(500)
            }
            if (count <= 10) {
                handleDeepLink(intent)
            }
        }
    }

    private fun handleDeepLink(intent: Intent?, coldBoot: Boolean = false) {
        hasDeepLink(intent?.data)?.let {
            if (it.type == DeepLinkType.ISSUANCE && !coldBoot) {
                handleDeepLinkAction(
                    routerHost.getNavController(),
                    it.link
                )
            } else if (it.type == DeepLinkType.SIGN_DOCUMENT) {
                cacheDeepLink(intent)
                if (routerHost.userIsLoggedInWithDocuments()) {
                    handleDeepLinkAction(
                        routerHost.getNavController(),
                        it
                    )
                }
            } else if (
                it.type == DeepLinkType.CREDENTIAL_OFFER
                && !routerHost.userIsLoggedInWithDocuments()
                && routerHost.userIsLoggedInWithNoDocuments()
            ) {
                cacheDeepLink(intent)
                routerHost.popToIssuanceOnboardingScreen()
            } else if (it.type == DeepLinkType.OPENID4VP
                && routerHost.userIsLoggedInWithDocuments()
                && (routerHost.isScreenOnBackStackOrForeground(IssuanceScreens.AddDocument)
                        || routerHost.isScreenOnBackStackOrForeground(IssuanceScreens.DocumentOffer))
            ) {
                handleDeepLinkAction(
                    routerHost.getNavController(),
                    DeepLinkAction(it.link, DeepLinkType.DYNAMIC_PRESENTATION)
                )
            } else if (it.type != DeepLinkType.ISSUANCE) {
                cacheDeepLink(intent)
                if (routerHost.userIsLoggedInWithDocuments()) {
                    routerHost.popToDashboardScreen()
                }
            }
            if (intent?.data?.host == "auth-done") {
                val code = intent?.data?.getQueryParameter("code")
                if (code != null) {
                    sendBroadcast(Intent(CoreActions.EPARAKSTS_AUTH_DONE).apply {
                        putExtra("code", code)
                    })
                }
            }
            setIntent(Intent())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type != null) {
                    val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                    if (uri != null) {
                        try {
                            val accessibleUri = uri.grantAppAccess(this)
                            val deepLinkUri = Uri.parse("${BuildConfig.SIGN_FILE_SHARE_SCHEME}://${BuildConfig.FILE_SHARE_HOST}/sign?filePath=${accessibleUri}")

                            pendingDeepLink = deepLinkUri

                            finish()
                            startActivity(Intent(this, this::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("fromShare", true)
                                data = deepLinkUri
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        } catch (e: Exception) {
                            Log.e("NobidComponentActivity", "Error handling shared file: ${e.message}", e)
                        }
                    }
                }
            }

            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        handleFileShare(intent)

                        finish()
                        startActivity(Intent(this, this::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            data = intent.data
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } else {
                        // no-op
                    }
                }
            }
        }
    }

    private fun handleFileShare(intent: Intent) {
        val uri = when (intent.action) {
            Intent.ACTION_SEND -> intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }

        uri?.let {
            try {
                val accessibleUri = uri.grantAppAccess(this)

                val deepLink = DeepLinkAction(
                    link = Uri.parse("${BuildConfig.SIGN_FILE_SHARE_SCHEME}://${BuildConfig.FILE_SHARE_HOST}/sign?filePath=${accessibleUri}"),
                    type = DeepLinkType.SIGN_DOCUMENT
                )

                if (routerHost.userIsLoggedInWithDocuments()) {
                    handleDeepLinkAction(routerHost.getNavController(), deepLink)
                } else {
                    cacheDeepLink(Intent().apply { data = deepLink.link })
                }
            } catch (e: Exception) {
                Log.e("NobidComponentActivity", "Error accessing file: ${e.message}", e)
            }
        }
    }

    class FailedToEnableAccessForURI(override val message: String = "Failed to provide access.") : Exception()

    private fun Uri.grantAppAccess(context: Context): Uri {
        return try {
            val mimeType = context.contentResolver.getType(this)
            val fileExtension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "tmp"

            var filename = ""

            context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        filename = cursor.getString(displayNameIndex)
                    }
                }
            }

            if (filename.isEmpty()) {
                this.path?.let { path ->
                    val lastSlashIndex = path.lastIndexOf('/')
                    if (lastSlashIndex != -1 && lastSlashIndex < path.length - 1) {
                        filename = path.substring(lastSlashIndex + 1)
                    }
                }
            }

            if (filename.isEmpty()) {
                filename = "document_${System.currentTimeMillis()}.$fileExtension"
            }

            val safeFilename = if (filename.contains(".")) {
                val name = filename.substringBeforeLast(".")
                val ext = filename.substringAfterLast(".")
                "${name}_${System.currentTimeMillis()}.$ext"
            } else {
                "${filename}_${System.currentTimeMillis()}.$fileExtension"
            }

            val inputStream = context.contentResolver.openInputStream(this)
                ?: throw FailedToEnableAccessForURI("Input stream is null.")

            val tempFile = File(context.cacheDir, safeFilename)

            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            inputStream.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)

        } catch (e: Exception) {
            throw FailedToEnableAccessForURI().apply {
                this.stackTrace = e.stackTrace
            }
        }
    }

    private fun applySavedLocale(base: Context): Context {
        val savedLanguage = try {
            prefKeys.getLanguage()
        } catch (e: Exception) {
            ""
        }
        return if (savedLanguage.isNotEmpty()) {
            val locale = Locale(savedLanguage)
            Locale.setDefault(locale)
            val config = base.resources.configuration
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
            base.createConfigurationContext(config)
        } else {
            base
        }
    }
}