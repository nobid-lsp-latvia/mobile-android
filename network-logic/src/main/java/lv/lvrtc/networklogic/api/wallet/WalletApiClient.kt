// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.networklogic.api.wallet

import com.google.gson.Gson
import lv.lvrtc.networklogic.api.base.BaseApiClient
import lv.lvrtc.networklogic.error.ApiError.UnauthorizedException
import lv.lvrtc.networklogic.error.ApiErrorHandler
import lv.lvrtc.networklogic.model.user.DocumentOfferResponse
import lv.lvrtc.networklogic.model.wallet.EParakstIdentitiesResponse
import lv.lvrtc.networklogic.model.wallet.EParakstIdentitiesUrlResponse
import lv.lvrtc.networklogic.model.wallet.SignDocumentRequest
import lv.lvrtc.networklogic.model.wallet.SignDocumentResponse
import lv.lvrtc.networklogic.model.wallet.ValidateContainerResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.File

enum class DocumentType(val type: String) {
    PID("pid"),
    MDL("mdl"),
    RTU("rtu"),
    ESEAL("eseal"),
    ESIGN("esign"),
    IBAN("IBAN");
}

interface WalletApiClient {
    suspend fun getDocumentOffer(documentType: DocumentType): Result<DocumentOfferResponse>

    suspend fun getEParakstIdentitiesUrl(redirectUrl: String):  Result<EParakstIdentitiesUrlResponse>

    suspend fun getEParakstIdentities(token: String): Result<EParakstIdentitiesResponse>

    suspend fun signDocument(
        requestBody: SignDocumentRequest,
        file: File
    ): Result<SignDocumentResponse>

    suspend fun sealDocument(
        requestBody: SignDocumentRequest,
        file: File
    ): Result<SignDocumentResponse>

    suspend fun downloadDocument(requestId: String): Result<Response<ResponseBody>>

    suspend fun closeSigningSession(requestId: String, type: String): Result<Unit>

    suspend fun validateContainer(file: File): Result<ValidateContainerResponse>
}

class WalletApiClientImpl(
    private val api: WalletApi,
    private val errorHandler: ApiErrorHandler
) : BaseApiClient(errorHandler), WalletApiClient {

    override suspend fun getDocumentOffer(documentType: DocumentType): Result<DocumentOfferResponse> {
        return handleRequest { api.getDocumentOffer(documentType.name.lowercase()) }
            .onFailure { throwable ->
                if (throwable is HttpException && throwable.code() == 401) {
                    throw UnauthorizedException("Session token is invalid")
                }
            }
    }

    override suspend fun getEParakstIdentitiesUrl(redirectUrl: String): Result<EParakstIdentitiesUrlResponse> {
        return handleRequest { api.getEParakstIdentitiesUrl(redirectUrl) }
    }

    override suspend fun getEParakstIdentities(token: String): Result<EParakstIdentitiesResponse> {
        return handleRequest { api.getEParakstIdentities(token) }
    }

    override suspend fun signDocument(
        requestBody: SignDocumentRequest,
        file: File
    ): Result<SignDocumentResponse> = handleRequest {
        val jsonRequestBody = createJsonRequestBody(requestBody)
        val filePart = createFileMultipart(file)
        api.signDocument(jsonRequestBody, filePart)
    }

    override suspend fun sealDocument(
        requestBody: SignDocumentRequest,
        file: File
    ): Result<SignDocumentResponse> = handleRequest {
        val jsonRequestBody = createJsonRequestBody(requestBody)
        val filePart = createFileMultipart(file)
        api.sealDocument(jsonRequestBody, filePart)
    }

    override suspend fun validateContainer(file: File): Result<ValidateContainerResponse> = handleRequest {
        val requestData = mapOf(
            "files" to listOf(
                mapOf("FileName" to file.name)
            )
        )
        val jsonRequestBody = createJsonRequestBody(requestData)
        val filePart = createFileMultipart(file)
        api.validateContainer(jsonRequestBody, filePart)
    }

    override suspend fun downloadDocument(requestId: String): Result<Response<ResponseBody>> =
        try {
            val response = api.downloadSignedDocument(requestId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response)
            } else {
                Result.failure(errorHandler.handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(errorHandler.handleException(e))
        }

    override suspend fun closeSigningSession(requestId: String, type: String): Result<Unit> =
        handleRequest { api.closeSigningSession(type, requestId) }

    private fun createJsonRequestBody(requestBody: Any): RequestBody {
        val jsonString = Gson().toJson(requestBody)
        return jsonString.toRequestBody("application/json".toMediaTypeOrNull())
    }

    private fun createFileMultipart(file: File): MultipartBody.Part {
        val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(file.name, file.name, requestBody)
    }
}