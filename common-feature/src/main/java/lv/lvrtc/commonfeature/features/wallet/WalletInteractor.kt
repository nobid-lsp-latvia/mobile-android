// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.wallet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.lvrtc.storagelogic.config.StorageConfig
import lv.lvrtc.businesslogic.controller.PrefsController
import lv.lvrtc.businesslogic.controller.crypto.SecureAreaRepository
import lv.lvrtc.commonfeature.util.extractFirstNameFromDocumentOrEmpty
import lv.lvrtc.corelogic.controller.DeleteAllDocumentsPartialState
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.networklogic.api.attestation.AttestationApiClient
import lv.lvrtc.storagelogic.service.RealmService

interface WalletInteractor {
    fun deleteWallet(
        mainPidDocumentId: String? = null
    ): Flow<DeleteWalletPartialState>

    fun getFirstName(): String
}

sealed class DeleteWalletPartialState {
    data object Success : DeleteWalletPartialState()
    data class Failure(val error: String) : DeleteWalletPartialState()
}

class WalletInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val prefsController: PrefsController,
    private val storageConfig: StorageConfig,
    private val realmService: RealmService,
    private val secureAreaRepository: SecureAreaRepository
) : WalletInteractor {

    override fun getFirstName(): String {
        return walletCoreDocumentsController.getMainPidDocument()?.let { extractFirstNameFromDocumentOrEmpty(it) } ?: ""
    }

    override fun deleteWallet(mainPidDocumentId: String?): Flow<DeleteWalletPartialState> = flow {
        try {
            val pidDoc = mainPidDocumentId ?: walletCoreDocumentsController.getMainPidDocument()?.id

            if (pidDoc != null) {
                walletCoreDocumentsController.deleteAllDocuments(pidDoc)
                    .collect { state ->
                        when (state) {
                            is DeleteAllDocumentsPartialState.Success -> {
                                realmService.close()
                                storageConfig.deleteRealmDatabase()
                                secureAreaRepository.deleteWalletInstance()
                                prefsController.clearAll()
                                realmService.reset()
                                emit(DeleteWalletPartialState.Success)
                            }
                            is DeleteAllDocumentsPartialState.Failure -> {
                                emit(DeleteWalletPartialState.Failure(state.errorMessage))
                            }
                        }
                    }
            } else {
                // No PID document
                realmService.close()
                storageConfig.deleteRealmDatabase()
                secureAreaRepository.deleteWalletInstance()
                prefsController.clearAll()
                realmService.reset()
                emit(DeleteWalletPartialState.Success)
            }
        } catch (e: Exception) {
            emit(DeleteWalletPartialState.Failure(e.message ?: "Unknown error"))
        }
    }
}