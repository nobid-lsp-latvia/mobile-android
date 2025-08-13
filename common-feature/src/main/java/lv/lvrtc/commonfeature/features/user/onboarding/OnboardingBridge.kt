// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.user.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.controller.auth.BiometricsAvailability
import lv.lvrtc.authlogic.controller.auth.DeviceAuthenticationResult
import lv.lvrtc.authlogic.service.AuthPartialState
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.commonfeature.BuildConfig
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.corelogic.controller.IssueDocumentPartialState
import lv.lvrtc.networklogic.api.user.PersonVerificationStatus
import lv.lvrtc.resourceslogic.bridge.ONBOARDING
import lv.lvrtc.uilogic.navigation.NavigationCommand.ToNative
import lv.lvrtc.uilogic.navigation.NavigationCommand.ToWeb
import lv.lvrtc.uilogic.navigation.WebNavigationService
import lv.lvrtc.uilogic.navigation.WebScreens
import lv.lvrtc.webbridge.UrlHandler
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.BridgeRequest
import lv.lvrtc.webbridge.core.BridgeResponse

class OnboardingBridge(
    private val onboardingInteractor: OnboardingInteractor,
    private val navigationService: WebNavigationService,
    private val authService: AuthService,
    private val onboardingCoordinator: OnboardingCoordinator,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val prefKeys: PrefKeys
) : BaseBridge(), UrlHandler {

    override fun getName() = ONBOARDING.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            ONBOARDING.INITIATE_EPARAKSTS -> handleEParaksts(request)
            ONBOARDING.SUBMIT_EMAIL -> handleEmailSubmission(request)
            ONBOARDING.SUBMIT_SMS -> handleSmsSubmission(request)
            ONBOARDING.SUBMIT_EMAIL_OTP -> handleEmailOTPSubmission(request)
            ONBOARDING.SUBMIT_SMS_OTP -> handleSmsOTPSubmission(request)
            ONBOARDING.INITIALISE_WALLET -> handleInitialiseWallet(request)
            else -> createErrorResponse(request, "Unknown function ${request.function}")
        }
    }

    private fun handleEmailSubmission(request: BridgeRequest): BridgeResponse {
        val email = (request.data as? Map<*, *>)?.get("email") as? String
            ?: return createErrorResponse(request, "Invalid email")

        CoroutineScope(Dispatchers.IO).launch {
            val result = onboardingInteractor.sendVerificationEmail(email).first()
            val response = when (result) {
                is EmailPartialState.Success -> createSuccessResponse(request, null)
                is EmailPartialState.Error -> createErrorResponse(request, result.message)
            }
            emitEvent(response)
        }

        return createSuccessResponse(request, null)
    }

    private fun handleSmsSubmission(request: BridgeRequest): BridgeResponse {
        val number = (request.data as? Map<*, *>)?.get("number") as? String
            ?: return createErrorResponse(request, "Invalid number")

        CoroutineScope(Dispatchers.IO).launch {
            val result = onboardingInteractor.sendVerificationSms(number).first()
            val response = when (result) {
                is SmsPartialState.Success -> createSuccessResponse(request, null)
                is SmsPartialState.Error -> createErrorResponse(request, result.message)
            }
            emitEvent(response)
        }

        return createSuccessResponse(request, null)
    }

    private fun handleEmailOTPSubmission(request: BridgeRequest): BridgeResponse {
        val otp = (request.data as? Map<*, *>)?.get("otp") as? String
            ?: return createErrorResponse(request, "Invalid OTP")

        coroutineScope.launch {
            val result = onboardingInteractor.verifyEmailOTP(otp).first()
            val response = when (result) {
                is EmailPartialState.Success -> {
                    calculateNextStep()
                    createSuccessResponse(request, null)
                }

                is EmailPartialState.Error -> createErrorResponse(request, result.message)
            }
            emitEvent(response)
        }

        return createSuccessResponse(request, null)
    }

    private fun handleSmsOTPSubmission(request: BridgeRequest): BridgeResponse {
        val otp = (request.data as? Map<*, *>)?.get("otp") as? String
            ?: return createErrorResponse(request, "Invalid OTP")

        coroutineScope.launch {
            val result = onboardingInteractor.verifySmsOTP(otp).first()
            val response = when (result) {
                is SmsPartialState.Success -> {
                    calculateNextStep()
                    createSuccessResponse(request, null)
                }

                is SmsPartialState.Error -> createErrorResponse(request, result.message)
            }
            emitEvent(response)
        }

        return createSuccessResponse(request, null)
    }

    fun handleAuthCode(code: String) {
        coroutineScope.launch {
            onboardingInteractor.handleAuthCode(code).collect { result ->
                when (result.status) {
                    is AuthPartialState.Success -> {
                        when (result.personStatus) {
                            is PersonVerificationStatus.Complete -> {
                                navigationService.navigate(
                                    ToWeb(WebScreens.LOADING.path)
                                )
                            }

                            else -> {
                                calculateNextStep()
                            }
                        }
                        registerWalletInstance()
                    }

                    is AuthPartialState.Failure -> {
                        Log.d("OnboardingBridge", "Failure: ${result.status}")
                    }
                }
            }
        }
    }

    private fun registerWalletInstance() {
        coroutineScope.launch {
            onboardingInteractor.registerWallet()
        }
    }

    private suspend fun calculateNextStep() {
        val nextScreen = onboardingCoordinator.getCurrentScreen()

        when (nextScreen.screenName) {
            "QUICK_PIN" -> {
                navigationService.navigate(
                    ToNative(
                        nextScreen.screenRoute
                    )
                )
            }

            else -> {
                navigationService.navigate(
                    ToWeb(nextScreen.path)
                )
            }
        }
    }

    override fun handleUrl(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        if (url.startsWith("eparakstsid")) {
            val uri = request.url
            val successUrl = uri.getQueryParameter("successurl")
            val failureUrl = uri.getQueryParameter("failureurl")

            val originalParams = uri.queryParameterNames
            val newBuilder = Uri.Builder()
                .scheme(uri.scheme)
                .authority(uri.authority)
                .path(uri.path)

            for (paramName in originalParams) {
                if (paramName != "successurl" && paramName != "failureurl") {
                    for (value in uri.getQueryParameters(paramName)) {
                        newBuilder.appendQueryParameter(paramName, value)
                    }
                }
            }

            newBuilder.appendQueryParameter(
                "successurl",
                "${BuildConfig.DEEPLINK}resume_authn?url=${Uri.encode(successUrl ?: "")}"
            )
            newBuilder.appendQueryParameter(
                "failureurl",
                "${BuildConfig.DEEPLINK}resume_authn?url=${Uri.encode(failureUrl ?: "")}"
            )

            val modifiedUri = newBuilder.build()

            val intent = Intent(Intent.ACTION_VIEW, modifiedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                webView?.context?.startActivity(intent)
            } catch (_: Exception) {
            }
            return true
        }
        return false
    }

    private fun handleEParaksts(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            val url = authService.buildEParakstsAuthUrl()
            val customTabsIntent = CustomTabsIntent.Builder().setShowTitle(true).build()
            webView?.context?.let { context ->
                customTabsIntent.launchUrl(context, Uri.parse(url))
            }
        }
        return createSuccessResponse(request, null)
    }

    @SuppressLint("RestrictedApi")
    private fun handleInitialiseWallet(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            onboardingInteractor.issuePidDocument(prefKeys.getLastDocType())
                .collect { result ->
                    prefKeys.setLastDocType("")

                    when (result) {
                        is IssueDocumentPartialState.Success -> {
                            emitEvent(createSuccessResponse(request, null))
                        }

                        is IssueDocumentPartialState.Failure -> {
                            emitEvent(createErrorResponse(request, null))
                        }

                        is IssueDocumentPartialState.UserAuthRequired -> {
                            val activity = webView?.context as? ComponentActivity
                            activity?.let {
                                deviceAuthenticationInteractor.getBiometricsAvailability {
                                    when (it) {
                                        is BiometricsAvailability.CanAuthenticate -> {
                                            deviceAuthenticationInteractor.authenticateWithBiometrics(
                                                context = activity,
                                                crypto = result.crypto,
                                                notifyOnAuthenticationFailure = true,
                                                resultHandler = DeviceAuthenticationResult(
                                                    onAuthenticationSuccess = {
                                                        result.resultHandler.onAuthenticationSuccess()
                                                    },
                                                    onAuthenticationError = {
                                                        result.resultHandler.onAuthenticationError()
                                                        emitEvent(
                                                            createErrorResponse(
                                                                request,
                                                                "authentication_error"
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        is BiometricsAvailability.NonEnrolled -> {
                                            deviceAuthenticationInteractor.launchBiometricSystemScreen()
                                        }

                                        is BiometricsAvailability.Failure -> {
                                            result.resultHandler.onAuthenticationError()
                                            emitEvent(
                                                createErrorResponse(
                                                    request,
                                                    "authentication_error"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is IssueDocumentPartialState.DeferredSuccess -> TODO()
                    }
                }
        }
        return createSuccessResponse(request, null)
    }
}