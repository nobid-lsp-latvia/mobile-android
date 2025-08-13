// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.di

import lv.lvrtc.businesslogic.config.AppBuildType
import lv.lvrtc.businesslogic.config.ConfigLogic
import lv.lvrtc.businesslogic.controller.PrefsController
import lv.lvrtc.businesslogic.controller.crypto.KeystoreController
import lv.lvrtc.businesslogic.controller.crypto.SecureAreaRepository
import lv.lvrtc.businesslogic.controller.log.LogController
import lv.lvrtc.networklogic.api.attestation.AttestationApi
import lv.lvrtc.networklogic.api.attestation.AttestationApiClient
import lv.lvrtc.networklogic.api.attestation.AttestationApiClientImpl
import lv.lvrtc.networklogic.api.payment.PaymentApi
import lv.lvrtc.networklogic.api.payment.PaymentApiClient
import lv.lvrtc.networklogic.api.payment.PaymentApiClientImpl
import lv.lvrtc.networklogic.api.session.SessionApi
import lv.lvrtc.networklogic.api.session.SessionApiClient
import lv.lvrtc.networklogic.api.session.SessionApiClientImpl
import lv.lvrtc.networklogic.api.user.UserApi
import lv.lvrtc.networklogic.api.user.UserApiClient
import lv.lvrtc.networklogic.api.user.UserApiClientImpl
import lv.lvrtc.networklogic.api.wallet.WalletApi
import lv.lvrtc.networklogic.api.wallet.WalletApiClient
import lv.lvrtc.networklogic.api.wallet.WalletApiClientImpl
import lv.lvrtc.networklogic.error.ApiErrorHandler
import lv.lvrtc.networklogic.error.ApiErrorHandlerImpl
import lv.lvrtc.networklogic.interceptor.AuthenticationInterceptor
import lv.lvrtc.networklogic.session.SecureTokenStorage
import lv.lvrtc.networklogic.session.SessionManager
import lv.lvrtc.networklogic.session.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@ComponentScan("lv.lvrtc.networklogic")
class NetworkModule

@Factory
fun providesHttpLoggingInterceptor(configLogic: ConfigLogic) = HttpLoggingInterceptor()
    .apply {
        level = when (configLogic.appBuildType) {
            AppBuildType.DEBUG -> HttpLoggingInterceptor.Level.BODY
            AppBuildType.RELEASE -> HttpLoggingInterceptor.Level.NONE
        }
    }

@Factory
fun provideAuthInterceptor(
    tokenStorage: TokenStorage
): AuthenticationInterceptor = AuthenticationInterceptor(tokenStorage)

@Factory
fun provideOkHttpClient(
    httpLoggingInterceptor: HttpLoggingInterceptor,
    authInterceptor: AuthenticationInterceptor,
    configLogic: ConfigLogic,
): OkHttpClient {
    return OkHttpClient.Builder()
        .readTimeout(configLogic.environmentConfig.readTimeoutSeconds, TimeUnit.SECONDS)
        .connectTimeout(configLogic.environmentConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor(httpLoggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()
}

@Factory
fun provideConverterFactory(): GsonConverterFactory = GsonConverterFactory.create()

@Single
@Named("mainRetrofit")
fun provideRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getServerHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
@Named("walletRetrofit")
fun provideWalletRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getWalletApiHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
@Named("sessionRetrofit")
fun provideSessionRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getSessionApiHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
@Named("attestationRetrofit")
fun provideAttestationRetrofit(
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory,
    configLogic: ConfigLogic
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(configLogic.environmentConfig.getWalletApiHost())
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
}

@Single
fun provideApiErrorHandler(logController: LogController): ApiErrorHandler {
    return ApiErrorHandlerImpl(logController)
}

@Factory
fun provideUserApi(
    @Named("mainRetrofit") retrofit: Retrofit
): UserApi = retrofit.create(UserApi::class.java)

@Factory
fun provideWalletApi(
    @Named("walletRetrofit") retrofit: Retrofit
): WalletApi = retrofit.create(WalletApi::class.java)

@Single
fun provideUserApiClient(
    userApi: UserApi,
    errorHandler: ApiErrorHandler
): UserApiClient = UserApiClientImpl(userApi, errorHandler)

@Single
fun provideWalletApiClient(
    walletApi: WalletApi,
    errorHandler: ApiErrorHandler
): WalletApiClient = WalletApiClientImpl(walletApi, errorHandler)

@Single
fun provideTokenStorage(
    prefsController: PrefsController
): TokenStorage = SecureTokenStorage(prefsController)

@Factory
fun provideSessionApi(
    @Named("sessionRetrofit") retrofit: Retrofit
): SessionApi = retrofit.create(SessionApi::class.java)

@Single
fun provideSessionApiClient(
    sessionApi: SessionApi,
    errorHandler: ApiErrorHandler
): SessionApiClient = SessionApiClientImpl(sessionApi, errorHandler)

@Single
fun provideSessionManager(
    sessionApiClient: SessionApiClient,
    tokenStorage: TokenStorage,
): SessionManager = SessionManager(sessionApiClient, tokenStorage)

@Factory
fun provideAttestationApi(
    @Named("attestationRetrofit") retrofit: Retrofit
): AttestationApi = retrofit.create(AttestationApi::class.java)

@Factory
fun providePaymentApi(
    @Named("mainRetrofit") retrofit: Retrofit
): PaymentApi = retrofit.create(PaymentApi::class.java)

@Single
fun provideAttestationApiClient(
    attestationApi: AttestationApi,
    errorHandler: ApiErrorHandler,
    secureAreaRepository: SecureAreaRepository
): AttestationApiClient = AttestationApiClientImpl(attestationApi, errorHandler, secureAreaRepository)

@Single
fun providePaymentApiClient(
    paymentApi: PaymentApi,
    errorHandler: ApiErrorHandler
): PaymentApiClient = PaymentApiClientImpl(paymentApi, errorHandler)