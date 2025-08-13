// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.settings

import android.annotation.SuppressLint
import android.os.LocaleList
import androidx.biometric.BiometricManager
import androidx.core.app.ComponentActivity
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.controller.auth.BiometricsAvailability
import lv.lvrtc.authlogic.controller.auth.DeviceAuthenticationResult
import lv.lvrtc.authlogic.model.BiometricCrypto
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.commonfeature.features.biometric.BiometricInteractor
import lv.lvrtc.commonfeature.features.wallet.DeleteWalletPartialState
import lv.lvrtc.commonfeature.features.wallet.WalletInteractor
import lv.lvrtc.resourceslogic.bridge.SETTINGS
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.BridgeRequest
import lv.lvrtc.webbridge.core.BridgeResponse
import java.util.Locale

class SettingsBridge (
    private val biometricInteractor: BiometricInteractor,
    private val resourceProvider: ResourceProvider,
    private val navigationService: WebNavigationService,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val walletInteractor: WalletInteractor,
    private val prefKeys: PrefKeys
): BaseBridge() {
    override fun getName() = SETTINGS.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when(request.function) {
            SETTINGS.ENABLE_BIOMETRICS -> handleEnableBiometrics(request)
            SETTINGS.GET_BIOMETRIC_AVAILABILITY -> handleGetBiometricAvailability(request)
            SETTINGS.SET_THEME -> handleSetTheme(request)
            SETTINGS.SET_LANGUAGE -> handleSetLanguage(request)
            SETTINGS.DELETE_WALLET -> handleDeleteWallet(request)
            else -> createErrorResponse(request, "Unknown function ${request.function}")
        }
    }

    @SuppressLint("RestrictedApi")
    private fun handleEnableBiometrics(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val enabled = data["enabled"] as? Boolean
            ?: return createErrorResponse(request, "Missing enabled parameter")

        val activity = webView?.context as? ComponentActivity
        activity?.let {
            deviceAuthenticationInteractor.authenticateWithBiometrics(
                context = it,
                crypto = BiometricCrypto(null),
                notifyOnAuthenticationFailure = true,
                resultHandler = DeviceAuthenticationResult(
                    onAuthenticationSuccess = {
                        biometricInteractor.storeBiometricsUsageDecision(enabled)
                        emitEvent(createSuccessResponse(request, null))
                    },
                    onAuthenticationError = {
                        emitEvent(createErrorResponse(request, "authentication_error"))
                    }
                )
            )
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetBiometricAvailability(request: BridgeRequest): BridgeResponse {
        var availability: BiometricsAvailability? = null

        biometricInteractor.getBiometricsAvailability { result ->
            availability = result
        }

        val userEnabled = biometricInteractor.getBiometricUserSelection()

        val biometricManager = BiometricManager.from(resourceProvider.provideContext())
        val hasBiometricHardware = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

        val type = when {
            availability is BiometricsAvailability.CanAuthenticate &&
                    userEnabled && hasBiometricHardware -> "exists"

            availability is BiometricsAvailability.CanAuthenticate &&
                    hasBiometricHardware -> "android"

            else -> null
        }

        emitEvent(createSuccessResponse(request, mapOf("type" to null)))
        return createSuccessResponse(request, mapOf("type" to null))
    }

    private fun handleSetTheme(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val theme = data["theme"] as? String
            ?: return createErrorResponse(request, "Missing theme")

        return createSuccessResponse(request, null)
    }

    private fun handleSetLanguage(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val language = data["language"] as? String
            ?: return createErrorResponse(request, "Missing language")

        try {
            val locale = Locale(language)
            val context = resourceProvider.provideContext()

            Locale.setDefault(locale)
            val config = context.resources.configuration

            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)

            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            // Persist the language preference
            prefKeys.setLanguage(language)

            emitEvent(createSuccessResponse(request, null))
            return createSuccessResponse(request, null)
        } catch (e: Exception) {
            return createErrorResponse(request, "Invalid language format")
        }
    }

    @SuppressLint("RestrictedApi")
    private fun handleDeleteWallet(request: BridgeRequest): BridgeResponse {
        val activity = webView?.context as? ComponentActivity

        activity?.let {
            deviceAuthenticationInteractor.getBiometricsAvailability { availability ->
                when (availability) {
                    is BiometricsAvailability.CanAuthenticate -> {
                        deviceAuthenticationInteractor.authenticateWithBiometrics(
                            context = it,
                            crypto = BiometricCrypto(null),
                            notifyOnAuthenticationFailure = true,
                            resultHandler = DeviceAuthenticationResult(
                                onAuthenticationSuccess = {
                                    coroutineScope.launch {
                                        walletInteractor.deleteWallet()
                                            .collect { state ->
                                                when (state) {
                                                    is DeleteWalletPartialState.Success -> {
                                                        emitEvent(createSuccessResponse(request, null))
                                                    }
                                                    is DeleteWalletPartialState.Failure -> {
                                                        emitEvent(createErrorResponse(request, state.error))
                                                    }
                                                }
                                            }
                                    }
                                },
                                onAuthenticationError = {
                                    emitEvent(createErrorResponse(request, "authentication_error"))
                                }
                            )
                        )
                    }
                    else -> {
                        emitEvent(createErrorResponse(request, "Biometric authentication not available"))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }
}