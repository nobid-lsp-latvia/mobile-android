// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.user.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.lvrtc.authlogic.controller.auth.OnboardingStorageController
import lv.lvrtc.authlogic.model.OnboardingState
import lv.lvrtc.authlogic.service.AuthPartialState
import lv.lvrtc.authlogic.service.AuthResult
import lv.lvrtc.authlogic.service.AuthService
import lv.lvrtc.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.lvrtc.corelogic.controller.IssueDocumentPartialState
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.networklogic.api.attestation.AttestationApiClient
import lv.lvrtc.networklogic.api.user.PersonVerificationStatus
import lv.lvrtc.networklogic.api.user.UserApiClient
import lv.lvrtc.networklogic.api.wallet.DocumentType
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.networklogic.error.ApiError
import lv.lvrtc.networklogic.session.SessionManager
import lv.lvrtc.networklogic.session.TokenStorage
import lv.lvrtc.resourceslogic.bridge.DASHBOARD
import lv.lvrtc.resourceslogic.bridge.ONBOARDING
import org.koin.core.annotation.Factory

interface OnboardingInteractor {
    fun handleAuthCode(code: String): Flow<AuthResult>
    fun sendVerificationEmail(email: String): Flow<EmailPartialState>
    fun sendVerificationSms(number: String): Flow<SmsPartialState>
    fun verifyEmailOTP(otp: String): Flow<EmailPartialState>
    fun verifySmsOTP(otp: String): Flow<SmsPartialState>
    fun getPersonVerificationStatus(): Flow<PersonVerificationStatus>
    fun getOnboardingState(): Flow<OnboardingState>
    fun issuePidDocument(docType: String): Flow<IssueDocumentPartialState>
    suspend fun registerWallet()
}

@Factory
class OnboardingInteractorImpl(
    private val userApiClient: UserApiClient,
    private val walletApiClient: WalletApiClient,
    private val tokenStorage: TokenStorage,
    private val onboardingStorageController: OnboardingStorageController,
    private val sessionManager: SessionManager,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val authService: AuthService,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val attestationApiClient: AttestationApiClient
) : OnboardingInteractor {

    override fun sendVerificationEmail(email: String) = flow<EmailPartialState> {
        userApiClient.sendEmailVerification(email)
            .onSuccess {
                val currentState = onboardingStorageController.getOnboardingState()
                onboardingStorageController.setOnboardingState(
                    currentState.copy(email = email)
                )
                emit(EmailPartialState.Success)
            }
            .onFailure { error ->
                val errorTag = when (error) {
                    is ApiError.ValidationException -> error.errorTag
                    else -> null
                }
                emit(EmailPartialState.Error(errorTag))
            }
    }


    override fun sendVerificationSms(number: String) = flow<SmsPartialState> {
        userApiClient.sendSmsVerification(number)
            .onSuccess {
                val currentState = onboardingStorageController.getOnboardingState()
                onboardingStorageController.setOnboardingState(
                    currentState.copy(phone = number)
                )
                emit(SmsPartialState.Success)
            }
            .onFailure { error ->
                val errorTag = when (error) {
                    is ApiError.ValidationException -> error.errorTag
                    else -> null
                }
                emit(SmsPartialState.Error(message = errorTag))
            }
    }

    override fun verifyEmailOTP(otp: String) = flow<EmailPartialState> {
        userApiClient.verifyEmail(otp)
            .onSuccess {
                val currentState = onboardingStorageController.getOnboardingState()
                onboardingStorageController.setOnboardingState(
                    currentState.copy(isEmailVerified = true)
                )
                emit(EmailPartialState.Success)
            }
            .onFailure { error ->
                val errorTag = when (error) {
                    is ApiError.ValidationException -> error.errorTag
                    else -> null
                }
                emit(EmailPartialState.Error(message = errorTag))
            }
    }

    override fun verifySmsOTP(otp: String) = flow<SmsPartialState> {
        userApiClient.verifySms(otp)
            .onSuccess {
                val currentState = onboardingStorageController.getOnboardingState()
                onboardingStorageController.setOnboardingState(
                    currentState.copy(isPhoneVerified = true)
                )
                emit(SmsPartialState.Success)
            }
            .onFailure { error ->
                val errorTag = when (error) {
                    is ApiError.ValidationException -> error.errorTag
                    else -> null
                }
                emit(SmsPartialState.Error(message = errorTag))
            }
    }

    override fun getOnboardingState() = flow {
        emit(onboardingStorageController.getOnboardingState())
    }

    override fun handleAuthCode(code: String): Flow<AuthResult> = flow {
        tokenStorage.saveToken(code)

        if (!sessionManager.checkSession()) {
            emit(AuthResult(AuthPartialState.Failure("Session expired")))
            return@flow
        }

        val personResult = userApiClient.getPerson()

        when {
            personResult.isSuccess -> {
                val status = personResult.getOrNull()
                when (status) {
                    is PersonVerificationStatus.Complete -> {
                        onboardingStorageController.setOnboardingState(
                            OnboardingState(
                                isEmailVerified = true,
                                isPhoneVerified = true
                            )
                        )
                        emit(AuthResult(
                            status = AuthPartialState.Success(DASHBOARD.SCREENS.MAIN),
                            personStatus = status
                        ))
                    }
                    is PersonVerificationStatus.Incomplete -> {
                        val nextScreen = when {
                            status.needsPhone -> ONBOARDING.SCREENS.SMS
                            status.needsEmail -> ONBOARDING.SCREENS.EMAIL
                            else -> DASHBOARD.SCREENS.MAIN
                        }
                        emit(AuthResult(
                            status = AuthPartialState.Success(nextScreen),
                            personStatus = status
                        ))
                    }
                    null -> emit(AuthResult(AuthPartialState.Failure("Invalid response")))
                }
            }
            else -> emit(AuthResult(AuthPartialState.Failure(personResult.exceptionOrNull()?.message ?: "Unknown error")))
        }
    }

    override fun getPersonVerificationStatus(): Flow<PersonVerificationStatus> = flow {
        userApiClient.getPerson()
            .onSuccess { status ->
                emit(status)
            }
            .onFailure {
                emit(PersonVerificationStatus.Incomplete(
                    needsEmail = true,
                    needsPhone = true
                ))
            }
    }

    override fun issuePidDocument(docType: String): Flow<IssueDocumentPartialState> {
        val documentType = when (docType) {
            "pid" -> {
                DocumentType.PID
            }
            "mdl" -> {
                DocumentType.MDL
            }
            "rtu" -> {
                DocumentType.RTU
            }
            else -> {
                DocumentType.PID
            }
        }

        return deviceAuthenticationInteractor.issueDocumentByType(documentType)
    }

    override suspend fun registerWallet() {
        if(!attestationApiClient.checkInstance()) {
            val nonce = attestationApiClient.getNonce()
            nonce.getOrNull()?.c_nonce?.let { attestationApiClient.getInstance(it) }
        }
    }
}

sealed class EmailPartialState {
    data object Success : EmailPartialState()
    data class Error(val message: String?) : EmailPartialState()
}

sealed class SmsPartialState {
    data object Success : SmsPartialState()
    data class Error(val message: String?) : SmsPartialState()
}