// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.controller

import com.android.identity.securearea.KeyUnlockData
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.DefaultKeyUnlockData
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredIssueResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OfferResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import lv.lvrtc.authlogic.controller.auth.DeviceAuthenticationResult
import lv.lvrtc.authlogic.model.BiometricCrypto
import lv.lvrtc.businesslogic.extensions.compareLocaleLanguage
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.corelogic.model.DeferredDocumentData
import lv.lvrtc.corelogic.model.DocumentIdentifier
import lv.lvrtc.corelogic.model.FormatType
import lv.lvrtc.corelogic.model.ScopedDocument
import lv.lvrtc.corelogic.model.toDocumentIdentifier
import lv.lvrtc.resourceslogic.R
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.BookmarkStorageController
import lv.lvrtc.storagelogic.controller.TransactionStorageController
import lv.lvrtc.storagelogic.model.Bookmark
import lv.lvrtc.storagelogic.model.Transaction
import lv.lvrtc.storagelogic.model.TransactionType
import java.util.Locale

enum class IssuanceMethod {
    OPENID4VCI
}

sealed class IssueDocumentPartialState {
    data class Success(val documentId: String) : IssueDocumentPartialState()
    data class DeferredSuccess(val deferredDocuments: Map<String, String>) :
        IssueDocumentPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : IssueDocumentPartialState()
}

sealed class IssueDocumentsPartialState {
    data class Success(val documentIds: List<String>) : IssueDocumentsPartialState()
    data class DeferredSuccess(val deferredDocuments: Map<String, String>) :
        IssueDocumentsPartialState()

    data class PartialSuccess(
        val documentIds: List<String>,
        val nonIssuedDocuments: Map<String, String>,
    ) : IssueDocumentsPartialState()

    data class Failure(val errorMessage: String) : IssueDocumentsPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : IssueDocumentsPartialState()
}

sealed class LoadEParakstDocumentState {
    data class Success(val documentId: String) : LoadEParakstDocumentState()
    data class Failure(val error: String?) : LoadEParakstDocumentState()
}

sealed class AddSampleDataPartialState {
    data object Success : AddSampleDataPartialState()
    data class Failure(val error: String) : AddSampleDataPartialState()
}

sealed class DeleteDocumentPartialState {
    data object Success : DeleteDocumentPartialState()
    data class Failure(val errorMessage: String) : DeleteDocumentPartialState()
}

sealed class DeleteAllDocumentsPartialState {
    data object Success : DeleteAllDocumentsPartialState()
    data class Failure(val errorMessage: String) : DeleteAllDocumentsPartialState()
}

sealed class ResolveDocumentOfferPartialState {
    data class Success(val offer: Offer) : ResolveDocumentOfferPartialState()
    data class Failure(val errorMessage: String) : ResolveDocumentOfferPartialState()
}

sealed class FetchScopedDocumentsPartialState {
    data class Success(val documents: List<ScopedDocument>) : FetchScopedDocumentsPartialState()
    data class Failure(val errorMessage: String) : FetchScopedDocumentsPartialState()
}

sealed class IssueDeferredDocumentPartialState {
    data class Issued(
        val deferredDocumentData: DeferredDocumentData,
    ) : IssueDeferredDocumentPartialState()

    data class NotReady(
        val deferredDocumentData: DeferredDocumentData,
    ) : IssueDeferredDocumentPartialState()

    data class Failed(
        val documentId: DocumentId,
        val errorMessage: String,
    ) : IssueDeferredDocumentPartialState()

    data class Expired(
        val documentId: DocumentId,
    ) : IssueDeferredDocumentPartialState()
}

interface WalletCoreDocumentsController {

    /**
     * @return All the documents from the Database.
     * */

//    fun loadSampleData(sampleDataByteArray: ByteArray): Flow<LoadSampleDataPartialState>

    fun loadEParakstDocument(
        sampleData: ByteArray,
        docType: String,
        nameSpace: String
    ): Flow<LoadEParakstDocumentState>

    /**
     * Adds the sample data into the Database.
     * */
    fun addSampleData(): Flow<AddSampleDataPartialState>

    fun getAllDocuments(): List<Document>

    fun getAllIssuedDocuments(): List<IssuedDocument>

    fun getAllDocumentsByType(documentIdentifiers: List<DocumentIdentifier>): List<IssuedDocument>

    fun getDocumentById(documentId: DocumentId): Document?

    fun getMainPidDocument(): IssuedDocument?

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String,
    ): Flow<IssueDocumentPartialState>

    fun issueDocumentsByOfferUri(
        offerUri: String,
        txCode: String? = null,
    ): Flow<IssueDocumentsPartialState>

    fun deleteDocument(
        documentId: String,
    ): Flow<DeleteDocumentPartialState>

    fun deleteAllDocuments(mainPidDocumentId: String): Flow<DeleteAllDocumentsPartialState>

    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState>

    fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState>

    fun resumeOpenId4VciWithAuthorization(uri: String)

    suspend fun getScopedDocuments(locale: Locale): FetchScopedDocumentsPartialState
}

class WalletCoreDocumentsControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet,
    private val transactionStorageController: TransactionStorageController,
    private val bookmarkStorageController: BookmarkStorageController
) : WalletCoreDocumentsController {

    private val genericErrorMessage
        get() = resourceProvider.genericErrorMessage()

    private val documentErrorMessage
        get() = resourceProvider.getString(R.string.issuance_generic_error)

    private val openId4VciManager by lazy {
        eudiWallet.createOpenId4VciManager()
    }

    private suspend fun logTransaction(
        documentId: String,
        docType: String,
        nameSpace: String,
        type: TransactionType,
        status: String = "SUCCESS",
        authority: String? = null,
    ) {
        transactionStorageController.store(
            Transaction(
                documentId = documentId,
                docType = docType,
                nameSpace = nameSpace,
                status = status,
                eventType = type.name,
                authority = authority,
            )
        )
    }

    override fun loadEParakstDocument(
        sampleData: ByteArray,
        docType: String,
        nameSpace: String
    ): Flow<LoadEParakstDocumentState> = flow {
        eudiWallet.loadMdocSampleDocuments(
            sampleData = sampleData,
            createSettings = eudiWallet.getDefaultCreateDocumentSettings(),
            documentNamesMap = mapOf(docType to nameSpace)
        ).kotlinResult
            .onSuccess { documents ->
                documents.firstOrNull()?.let { document ->
                    val documentId = document

                    bookmarkStorageController.store(Bookmark(identifier = documentId))

                    logTransaction(
                        documentId = documentId,
                        docType = docType,
                        nameSpace = nameSpace,
                        type = TransactionType.DOCUMENT_ISSUED
                    )

                    emit(LoadEParakstDocumentState.Success(documentId))
                } ?: emit(LoadEParakstDocumentState.Failure(null))
            }
            .onFailure {
                emit(LoadEParakstDocumentState.Failure(it.message ?: genericErrorMessage))
            }
    }.safeAsync {
        LoadEParakstDocumentState.Failure(it.localizedMessage ?: genericErrorMessage)
    }

    override fun addSampleData(): Flow<AddSampleDataPartialState> = flow {
        emit(AddSampleDataPartialState.Success)
    }.safeAsync {
        AddSampleDataPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
    }

    override fun getAllDocuments(): List<Document> =
        eudiWallet.getDocuments { it is IssuedDocument || it is DeferredDocument }

    override fun getAllIssuedDocuments(): List<IssuedDocument> =
        eudiWallet.getDocuments().filterIsInstance<IssuedDocument>()

    override suspend fun getScopedDocuments(locale: Locale): FetchScopedDocumentsPartialState {
        return try {

            val metadata = openId4VciManager.getIssuerMetadata().getOrThrow()

            val documents = metadata.credentialConfigurationsSupported.map { (id, config) ->

                val name: String = config.display
                    .firstOrNull { locale.compareLocaleLanguage(it.locale) }
                    ?.name
                    ?: config.display.firstOrNull()?.name
                    ?: id.value

                val isPid: Boolean = when (config) {
                    is MsoMdocCredential -> config.docType.toDocumentIdentifier() == DocumentIdentifier.MdocPid
                    // TODO: Re-activate once SD-JWT PID Rule book is in place in ARF.
                    //is SdJwtVcCredential -> config.type.toDocumentIdentifier() == DocumentIdentifier.SdJwtPid
                    else -> false
                }

                ScopedDocument(
                    name = name,
                    configurationId = id.value,
                    isPid = isPid
                )
            }
            if (documents.isNotEmpty()) {
                FetchScopedDocumentsPartialState.Success(documents)
            } else {
                FetchScopedDocumentsPartialState.Failure(genericErrorMessage)
            }
        } catch (e: Exception) {
            FetchScopedDocumentsPartialState.Failure(e.localizedMessage ?: genericErrorMessage)
        }
    }

    override fun getAllDocumentsByType(documentIdentifiers: List<DocumentIdentifier>): List<IssuedDocument> =
        getAllDocuments()
            .filterIsInstance<IssuedDocument>()
            .filter {
                when (it.format) {
                    is MsoMdocFormat -> documentIdentifiers.any { id ->
                        id.formatType == (it.format as MsoMdocFormat).docType
                    }

                    is SdJwtVcFormat -> documentIdentifiers.any { id ->
                        id.formatType == (it.format as SdJwtVcFormat).vct
                    }
                }
            }

    override fun getDocumentById(documentId: DocumentId): Document? {
        return eudiWallet.getDocumentById(documentId = documentId)
    }

    override fun getMainPidDocument(): IssuedDocument? =
        getAllDocumentsByType(
            documentIdentifiers = listOf(
                DocumentIdentifier.MdocPid,
                DocumentIdentifier.SdJwtPid
            )
        ).minByOrNull { it.createdAt }

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String,
    ): Flow<IssueDocumentPartialState> = flow {
        when (issuanceMethod) {

            IssuanceMethod.OPENID4VCI -> {
                issueDocumentWithOpenId4VCI(configId).collect { response ->
                    when (response) {
                        is IssueDocumentsPartialState.Failure -> emit(
                            IssueDocumentPartialState.Failure(
                                errorMessage = documentErrorMessage
                            )
                        )

                        is IssueDocumentsPartialState.Success -> {
                            response.documentIds.forEach { docId ->
                                getDocumentById(docId)?.let { doc ->
                                    val authority = try {
                                        doc.metadata?.claims?.firstOrNull { it.name.name == "issuing_authority" }
                                            ?.name?.name
                                    } catch (e: Exception) {
                                        null
                                    }

                                    logTransaction(
                                        documentId = doc.id,
                                        docType = doc.toDocumentIdentifier().formatType,
                                        nameSpace = doc.name,
                                        type = TransactionType.DOCUMENT_ISSUED,
                                        authority = authority,
                                    )
                                }
                            }
                            emit(IssueDocumentPartialState.Success(response.documentIds.first()))
                        }

                        is IssueDocumentsPartialState.UserAuthRequired -> emit(
                            IssueDocumentPartialState.UserAuthRequired(
                                crypto = response.crypto,
                                resultHandler = response.resultHandler
                            )
                        )

                        is IssueDocumentsPartialState.PartialSuccess -> emit(
                            IssueDocumentPartialState.Success(
                                response.documentIds.first()
                            )
                        )

                        is IssueDocumentsPartialState.DeferredSuccess -> emit(
                            IssueDocumentPartialState.DeferredSuccess(
                                response.deferredDocuments
                            )
                        )
                    }
                }
            }
        }
    }.safeAsync {
        IssueDocumentPartialState.Failure(errorMessage = documentErrorMessage)
    }

    override fun issueDocumentsByOfferUri(
        offerUri: String,
        txCode: String?,
    ): Flow<IssueDocumentsPartialState> =
        callbackFlow {
            openId4VciManager.issueDocumentByOfferUri(
                offerUri = offerUri,
                onIssueEvent = issuanceCallback(),
                txCode = txCode,
            )
            awaitClose()
        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = documentErrorMessage
            )
        }

    override fun deleteDocument(documentId: String): Flow<DeleteDocumentPartialState> = flow {
        val document = getDocumentById(documentId)

        eudiWallet.deleteDocumentById(documentId = documentId)
            .kotlinResult
            .onSuccess {
                document?.let { doc ->
                    logTransaction(
                        documentId = doc.id,
                        docType = doc.toDocumentIdentifier().formatType,
                        nameSpace = doc.name,
                        type = TransactionType.DOCUMENT_DELETED,
                    )
                }
                emit(DeleteDocumentPartialState.Success)
            }
            .onFailure {
                emit(
                    DeleteDocumentPartialState.Failure(
                        errorMessage = it.localizedMessage ?: genericErrorMessage
                    )
                )
            }
    }.safeAsync {
        DeleteDocumentPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMessage
        )
    }

    override fun deleteAllDocuments(mainPidDocumentId: String): Flow<DeleteAllDocumentsPartialState> =
        flow {
            val allDocuments = getAllDocuments()
            val mainPidDocument = getMainPidDocument()

            mainPidDocument?.let {
                val restOfDocuments = allDocuments.minusElement(it)

                var restOfAllDocsDeleted = true
                var restOfAllDocsDeletedFailureReason = ""

                restOfDocuments.forEach { document ->

                    deleteDocument(
                        documentId = document.id
                    ).collect { deleteDocumentPartialState ->
                        when (deleteDocumentPartialState) {
                            is DeleteDocumentPartialState.Failure -> {
                                restOfAllDocsDeleted = false
                                restOfAllDocsDeletedFailureReason =
                                    deleteDocumentPartialState.errorMessage
                            }

                            is DeleteDocumentPartialState.Success -> {}
                        }
                    }
                }

                if (restOfAllDocsDeleted) {
                    deleteDocument(
                        documentId = mainPidDocumentId
                    ).collect { deleteMainPidDocumentPartialState ->
                        when (deleteMainPidDocumentPartialState) {
                            is DeleteDocumentPartialState.Failure -> emit(
                                DeleteAllDocumentsPartialState.Failure(
                                    errorMessage = deleteMainPidDocumentPartialState.errorMessage
                                )
                            )

                            is DeleteDocumentPartialState.Success -> emit(
                                DeleteAllDocumentsPartialState.Success
                            )
                        }
                    }
                } else {
                    emit(DeleteAllDocumentsPartialState.Failure(errorMessage = restOfAllDocsDeletedFailureReason))
                }
            } ?: emit(
                DeleteAllDocumentsPartialState.Failure(
                    errorMessage = genericErrorMessage
                )
            )
        }.safeAsync {
            DeleteAllDocumentsPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState> =
        callbackFlow {
            openId4VciManager.resolveDocumentOffer(
                offerUri = offerUri,
                onResolvedOffer = { offerResult ->
                    when (offerResult) {
                        is OfferResult.Failure -> {
                            trySendBlocking(
                                ResolveDocumentOfferPartialState.Failure(
                                    errorMessage = offerResult.cause.localizedMessage
                                        ?: genericErrorMessage
                                )
                            )
                        }

                        is OfferResult.Success -> {
                            trySendBlocking(
                                ResolveDocumentOfferPartialState.Success(
                                    offer = offerResult.offer
                                )
                            )
                        }
                    }
                }
            )

            awaitClose()
        }.safeAsync {
            ResolveDocumentOfferPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState> =
        callbackFlow {
            (getDocumentById(docId) as? DeferredDocument)?.let { deferredDoc ->
                openId4VciManager.issueDeferredDocument(
                    deferredDocument = deferredDoc,
                    executor = null,
                    onIssueResult = { deferredIssuanceResult ->
                        when (deferredIssuanceResult) {
                            is DeferredIssueResult.DocumentFailed -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Failed(
                                        documentId = deferredIssuanceResult.documentId,
                                        errorMessage = deferredIssuanceResult.cause.localizedMessage
                                            ?: documentErrorMessage
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentIssued -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Issued(
                                        DeferredDocumentData(
                                            documentId = deferredIssuanceResult.documentId,
                                            formatType = deferredIssuanceResult.docType,
                                            docName = deferredIssuanceResult.name
                                        )
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentNotReady -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.NotReady(
                                        DeferredDocumentData(
                                            documentId = deferredIssuanceResult.documentId,
                                            formatType = deferredIssuanceResult.docType,
                                            docName = deferredIssuanceResult.name
                                        )
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentExpired -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Expired(
                                        documentId = deferredIssuanceResult.documentId
                                    )
                                )
                            }
                        }
                    }
                )
            } ?: trySendBlocking(
                IssueDeferredDocumentPartialState.Failed(
                    documentId = docId,
                    errorMessage = documentErrorMessage
                )
            )

            awaitClose()
        }.safeAsync {
            IssueDeferredDocumentPartialState.Failed(
                documentId = docId,
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        openId4VciManager.resumeWithAuthorization(uri)
    }

    private fun issueDocumentWithOpenId4VCI(configId: String): Flow<IssueDocumentsPartialState> =
        callbackFlow {

            openId4VciManager.issueDocumentByConfigurationIdentifier(
                credentialConfigurationId = configId,
                onIssueEvent = issuanceCallback()
            )

            awaitClose()

        }.safeAsync {
            IssueDocumentsPartialState.Failure(
                errorMessage = documentErrorMessage
            )
        }

    private fun handleDocumentIssued(event: IssueEvent.DocumentIssued): Flow<Unit> = flow {
        bookmarkStorageController.store(Bookmark(identifier = event.documentId))
        val doc = getDocumentById(event.documentId)
        val authority = if (doc is IssuedDocument) extractIssuingAuthority(doc) else null
        logTransaction(
            documentId = event.documentId,
            docType = event.docType.toString(),
            nameSpace = event.name,
            type = TransactionType.DOCUMENT_ISSUED,
            authority = authority
        )
        emit(Unit)
    }

    private fun ProducerScope<IssueDocumentsPartialState>.issuanceCallback(): OpenId4VciManager.OnIssueEvent {

        var totalDocumentsToBeIssued = 0
        val nonIssuedDocuments: MutableMap<FormatType, String> = mutableMapOf()
        val deferredDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()
        val issuedDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()

        val listener = OpenId4VciManager.OnIssueEvent { event ->
            when (event) {
                is IssueEvent.DocumentFailed -> {
                    nonIssuedDocuments[event.docType] = event.name
                }

                is IssueEvent.DocumentRequiresCreateSettings -> {
                    event.resume(eudiWallet.getDefaultCreateDocumentSettings())
                }

                is IssueEvent.DocumentRequiresUserAuth -> {
                    val keyUnlockData = event.document.DefaultKeyUnlockData
                    trySendBlocking(
                        IssueDocumentsPartialState.UserAuthRequired(
                            BiometricCrypto(keyUnlockData?.getCryptoObjectForSigning(event.signingAlgorithm)),
                            DeviceAuthenticationResult(
                                onAuthenticationSuccess = { event.resume(keyUnlockData as KeyUnlockData) },
                                onAuthenticationError = { event.cancel(null) }
                            )
                        )
                    )
                }

                is IssueEvent.Failure -> {
                    trySendBlocking(
                        IssueDocumentsPartialState.Failure(
                            errorMessage = documentErrorMessage
                        )
                    )
                }

                is IssueEvent.Finished -> {

                    if (deferredDocuments.isNotEmpty()) {
                        trySendBlocking(IssueDocumentsPartialState.DeferredSuccess(deferredDocuments))
                        return@OnIssueEvent
                    }

                    if (event.issuedDocuments.isEmpty()) {
                        trySendBlocking(
                            IssueDocumentsPartialState.Failure(
                                errorMessage = documentErrorMessage
                            )
                        )
                        return@OnIssueEvent
                    }

                    if (event.issuedDocuments.size == totalDocumentsToBeIssued) {
                        trySendBlocking(
                            IssueDocumentsPartialState.Success(
                                documentIds = event.issuedDocuments
                            )
                        )
                        return@OnIssueEvent
                    }

                    trySendBlocking(
                        IssueDocumentsPartialState.PartialSuccess(
                            documentIds = event.issuedDocuments,
                            nonIssuedDocuments = nonIssuedDocuments
                        )
                    )
                }

                is IssueEvent.DocumentIssued -> {
                    issuedDocuments[event.documentId] = event.docType
                    // TODO: For now automatically favorite all documents
                    runBlocking {
                        handleDocumentIssued(event).collect {}
                    }
                }

                is IssueEvent.Started -> {
                    totalDocumentsToBeIssued = event.total
                }

                is IssueEvent.DocumentDeferred -> {
                    deferredDocuments[event.documentId] = event.docType
                }
            }
        }

        return listener
    }

    private fun extractIssuingAuthority(document: IssuedDocument): String? {
        return document.data.claims
            .firstOrNull { it.identifier == "issuing_authority" }
            ?.value
            ?.toString()
    }
}