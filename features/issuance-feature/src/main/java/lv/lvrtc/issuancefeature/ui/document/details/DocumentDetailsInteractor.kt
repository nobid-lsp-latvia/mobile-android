// SPDX-License-Identifier: EUPL-1.2

/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package lv.lvrtc.issuancefeature.ui.document.details

import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import lv.lvrtc.businesslogic.extensions.safeAsync
import lv.lvrtc.commonfeature.features.document_details.model.DocumentUi
import lv.lvrtc.commonfeature.features.document_details.transformer.DocumentDetailsTransformer
import lv.lvrtc.corelogic.controller.DeleteAllDocumentsPartialState
import lv.lvrtc.corelogic.controller.DeleteDocumentPartialState
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.corelogic.model.DocumentIdentifier
import lv.lvrtc.corelogic.model.toDocumentIdentifier
import lv.lvrtc.resourceslogic.provider.ResourceProvider
import lv.lvrtc.storagelogic.controller.BookmarkStorageController
import lv.lvrtc.storagelogic.model.Bookmark
import kotlin.collections.count
import kotlin.let

sealed class DocumentDetailsInteractorPartialState {
    data class Success(val documentUi: DocumentUi, val isBookmarked: Boolean) : DocumentDetailsInteractorPartialState()
    data class Failure(val error: String) : DocumentDetailsInteractorPartialState()
}

sealed class DocumentDetailsInteractorDeleteDocumentPartialState {
    data object SingleDocumentDeleted : DocumentDetailsInteractorDeleteDocumentPartialState()
    data object AllDocumentsDeleted : DocumentDetailsInteractorDeleteDocumentPartialState()
    data class Failure(val errorMessage: String) :
        DocumentDetailsInteractorDeleteDocumentPartialState()
}

sealed class DocumentDetailsInteractorStoreBookmarkPartialState {
    data class Success(
        val bookmarkId: String
    ) : DocumentDetailsInteractorStoreBookmarkPartialState()

    data object Failure : DocumentDetailsInteractorStoreBookmarkPartialState()
}

sealed class DocumentDetailsInteractorDeleteBookmarkPartialState {
    data object Success : DocumentDetailsInteractorDeleteBookmarkPartialState()
    data object Failure : DocumentDetailsInteractorDeleteBookmarkPartialState()
}

interface DocumentDetailsInteractor {
    fun getDocumentDetails(
        documentId: DocumentId,
    ): Flow<DocumentDetailsInteractorPartialState>

    fun deleteDocument(
        documentId: DocumentId
    ): Flow<DocumentDetailsInteractorDeleteDocumentPartialState>

    fun storeBookmark(
        bookmarkId: String
    ): Flow<DocumentDetailsInteractorStoreBookmarkPartialState>

    fun deleteBookmark(
        bookmarkId: String
    ): Flow<DocumentDetailsInteractorDeleteBookmarkPartialState>
}

class DocumentDetailsInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val bookmarkStorageController: BookmarkStorageController,
    private val resourceProvider: ResourceProvider,
) : DocumentDetailsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getDocumentDetails(
        documentId: DocumentId,
    ): Flow<DocumentDetailsInteractorPartialState> =
        flow {

            val document =
                walletCoreDocumentsController.getDocumentById(documentId = documentId) as? IssuedDocument

            document?.let { issuedDocument ->
                val itemUi = DocumentDetailsTransformer.transformToUiItem(
                    document = issuedDocument,
                    resourceProvider = resourceProvider,
                )
                itemUi?.let { documentUi ->
                    val documentIsBookmarked = bookmarkStorageController.retrieve(documentId) != null

                    emit(
                        DocumentDetailsInteractorPartialState.Success(
                            documentUi = documentUi,
                            isBookmarked = documentIsBookmarked
                        )
                    )
                } ?: emit(DocumentDetailsInteractorPartialState.Failure(error = genericErrorMsg))
            } ?: emit(DocumentDetailsInteractorPartialState.Failure(error = genericErrorMsg))
        }.safeAsync {
            DocumentDetailsInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun deleteDocument(
        documentId: DocumentId
    ): Flow<DocumentDetailsInteractorDeleteDocumentPartialState> =
        flow {
            val document = walletCoreDocumentsController.getDocumentById(documentId = documentId)
            val format = document?.format
            val docType = (format as? MsoMdocFormat)?.docType ?: (format as? SdJwtVcFormat)?.vct
            val docIdentifier = docType?.toDocumentIdentifier()

            val shouldDeleteAllDocuments: Boolean =
                if (docIdentifier == DocumentIdentifier.MdocPid || docIdentifier == DocumentIdentifier.SdJwtPid) {

                    val allPidDocuments = walletCoreDocumentsController.getAllDocumentsByType(
                        documentIdentifiers = listOf(
                            DocumentIdentifier.MdocPid,
                            DocumentIdentifier.SdJwtPid
                        )
                    )

                    if (allPidDocuments.count() > 1) {
                        walletCoreDocumentsController.getMainPidDocument()?.id == documentId
                    } else {
                        true
                    }
                } else {
                    false
                }

            if (shouldDeleteAllDocuments) {
                walletCoreDocumentsController.deleteAllDocuments(mainPidDocumentId = documentId)
                    .map {
                        when (it) {
                            is DeleteAllDocumentsPartialState.Failure -> DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                                errorMessage = it.errorMessage
                            )

                            is DeleteAllDocumentsPartialState.Success -> DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted
                        }
                    }
            } else {
                walletCoreDocumentsController.deleteDocument(documentId = documentId).map {
                    when (it) {
                        is DeleteDocumentPartialState.Failure -> DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                            errorMessage = it.errorMessage
                        )

                        is DeleteDocumentPartialState.Success -> DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            DocumentDetailsInteractorDeleteDocumentPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun storeBookmark(bookmarkId: DocumentId): Flow<DocumentDetailsInteractorStoreBookmarkPartialState> =
        flow {
            bookmarkStorageController.store(Bookmark(identifier = bookmarkId))
            emit(DocumentDetailsInteractorStoreBookmarkPartialState.Success(bookmarkId = bookmarkId))
        }.safeAsync {
            DocumentDetailsInteractorStoreBookmarkPartialState.Failure
        }

    override fun deleteBookmark(bookmarkId: DocumentId): Flow<DocumentDetailsInteractorDeleteBookmarkPartialState> =
        flow {
            bookmarkStorageController.delete(bookmarkId)
            emit(DocumentDetailsInteractorDeleteBookmarkPartialState.Success)
        }.safeAsync {
            DocumentDetailsInteractorDeleteBookmarkPartialState.Failure
        }
}