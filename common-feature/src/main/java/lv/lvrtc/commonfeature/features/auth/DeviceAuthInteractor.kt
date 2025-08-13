// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import lv.lvrtc.authlogic.controller.auth.BiometricsAvailability
import lv.lvrtc.authlogic.controller.auth.DeviceAuthController
import lv.lvrtc.authlogic.controller.auth.DeviceAuthenticationResult
import lv.lvrtc.authlogic.model.BiometricCrypto
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.corelogic.controller.IssueDocumentPartialState
import lv.lvrtc.corelogic.controller.IssueDocumentsPartialState
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.networklogic.api.wallet.DocumentType
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.networklogic.error.ApiError.UnauthorizedException
import lv.lvrtc.resourceslogic.provider.ResourceProvider

interface DeviceAuthenticationInteractor {
    fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit)
    fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun launchBiometricSystemScreen()
    fun issueDocumentByType(documentType: DocumentType): Flow<IssueDocumentPartialState>
}

class DeviceAuthenticationInteractorImpl(
    private val deviceAuthenticationController: DeviceAuthController,
    private val walletApiClient: WalletApiClient,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val authService: AuthService,
    private val resourceProvider: ResourceProvider
) : DeviceAuthenticationInteractor {

    override fun launchBiometricSystemScreen() {
        deviceAuthenticationController.launchBiometricSystemScreen()
    }

    override fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit) {
        deviceAuthenticationController.deviceSupportsBiometrics(listener)
    }

    override fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    ) {
        deviceAuthenticationController.authenticate(
            context,
            crypto,
            notifyOnAuthenticationFailure,
            resultHandler
        )
    }

    var savedDocType: DocumentType? = null

    override fun issueDocumentByType(documentType: DocumentType): Flow<IssueDocumentPartialState> = flow {
        try {
            if(savedDocType == null)
                savedDocType = documentType
            val offer = walletApiClient.getDocumentOffer(savedDocType!!).fold(
                onSuccess = {
                    savedDocType = null
                    it
                            },
                onFailure = { error ->
                    if (error is UnauthorizedException) {
                        coroutineScope {
                            launch {
                                val url = authService.buildEParakstsAuthUrl()
                                // Create intent with NEW_TASK flag since we're outside Activity context
                                val customTabsIntent = CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                resourceProvider.provideContext().let { context ->
                                    customTabsIntent.launchUrl(context, Uri.parse(url))
                                }
                            }
                        }

                        emit(IssueDocumentPartialState.Failure("Authentication required"))
                        return@flow
                    }
                    throw error
                }
            )

            // Continue with normal document issuance flow
            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offer.offerUrl,
                txCode = null
            ).collect { result ->
                when (result) {
                    is IssueDocumentsPartialState.Success -> {
                        emit(IssueDocumentPartialState.Success(result.documentIds.first()))
                    }
                    is IssueDocumentsPartialState.Failure -> {
                        emit(IssueDocumentPartialState.Failure(result.errorMessage))
                    }
                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        emit(IssueDocumentPartialState.UserAuthRequired(
                            result.crypto,
                            result.resultHandler
                        ))
                    }
                    else -> {
                        emit(IssueDocumentPartialState.Failure(resourceProvider.genericErrorMessage()))
                    }
                }
            }
        } catch (e: Exception) {
            emit(IssueDocumentPartialState.Failure(e.message ?: resourceProvider.genericErrorMessage()))
        }
    }
}