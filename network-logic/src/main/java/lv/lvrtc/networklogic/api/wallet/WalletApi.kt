// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.wallet

import lv.lvrtc.networklogic.model.user.DocumentOfferResponse
import lv.lvrtc.networklogic.model.wallet.EParakstIdentitiesResponse
import lv.lvrtc.networklogic.model.wallet.EParakstIdentitiesUrlResponse
import lv.lvrtc.networklogic.model.wallet.SignDocumentResponse
import lv.lvrtc.networklogic.model.wallet.ValidateContainerResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WalletApi {
    @POST("wallet/1.0/{docType}")
    suspend fun getDocumentOffer(@Path("docType") docType: String): Response<DocumentOfferResponse>

    @GET("wallet/eparaksts/identities")
    suspend fun getEParakstIdentitiesUrl(
        @Query("redirecturl") redirectUrl: String
    ): Response<EParakstIdentitiesUrlResponse>

    @GET("wallet/eparaksts/identities/{token}")
    suspend fun getEParakstIdentities(
        @Path("token") token: String
    ): Response<EParakstIdentitiesResponse>

    @Multipart
    @POST("wallet/eparaksts/sign")
    suspend fun signDocument(
        @Part("json") requestData: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<SignDocumentResponse>

    @Multipart
    @POST("wallet/eparaksts/eseal")
    suspend fun sealDocument(
        @Part("json") requestData: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<SignDocumentResponse>

    @Multipart
    @POST("wallet/eparaksts/validate")
    suspend fun validateContainer(
        @Part("json") requestData: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ValidateContainerResponse>

    @GET("wallet/eparaksts/download/{requestId}")
    suspend fun downloadSignedDocument(
        @Path("requestId") requestId: String
    ): Response<ResponseBody>

    // sign or eseal
    @DELETE("wallet/eparaksts/{type}/{requestId}")
    suspend fun closeSigningSession(@Path("type") type: String, @Path("requestId") requestId: String): Response<Unit>
}