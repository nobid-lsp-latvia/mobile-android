// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.user.state

import kotlinx.coroutines.launch
import lv.lvrtc.commonfeature.util.extractFullNameFromDocumentOrEmpty
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.webbridge.core.BaseBridge
import lv.lvrtc.webbridge.core.BridgeRequest
import lv.lvrtc.webbridge.core.BridgeResponse
import java.util.Locale

class AppStateBridge(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,

    ) : BaseBridge() {
    override fun getName() = "app"

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when(request.function) {
            "getState" -> handleGetUserInfo(request)
            else -> createErrorResponse(request, "Unknown function")
        }
    }

    private fun handleGetUserInfo(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            val mainPid = walletCoreDocumentsController.getMainPidDocument()
            val language = Locale.getDefault().language

            val response = mapOf(
                "theme" to "light",
                "language" to language,
                "fullName" to (mainPid?.let {extractFullNameFromDocumentOrEmpty(it)} ?: ""),
                "system" to "android"
            )

            emitEvent(createSuccessResponse(request, response))
        }
        return createSuccessResponse(request, null)
    }
}