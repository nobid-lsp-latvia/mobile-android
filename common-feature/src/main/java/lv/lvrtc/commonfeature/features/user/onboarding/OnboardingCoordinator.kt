// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.user.onboarding

import lv.lvrtc.authlogic.controller.auth.OnboardingStorageController
import lv.lvrtc.businesslogic.controller.PrefKeys
import lv.lvrtc.corelogic.controller.WalletCoreDocumentsController
import lv.lvrtc.networklogic.session.SessionManager
import lv.lvrtc.networklogic.session.TokenStorage
import lv.lvrtc.uilogic.navigation.Screen
import lv.lvrtc.uilogic.navigation.WebScreens

class OnboardingCoordinator(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val onboardingStorageController: OnboardingStorageController,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val prefKeys: PrefKeys
) {
     suspend fun getCurrentScreen(): Screen {
        if (!tokenStorage.hasToken()) {
            return WebScreens.Activation
        }

        val state = onboardingStorageController.getOnboardingState()
        val onboardingIncomplete = !state.isEmailVerified || !state.isPhoneVerified
        if (onboardingIncomplete && !sessionManager.checkSession()) {
            tokenStorage.clearToken()
            return WebScreens.Activation
        }

         return when {
             !prefKeys.getAppActivated() -> WebScreens.Activation
             state.isEmailVerified && state.isPhoneVerified -> {
                 WebScreens.LOADING
             }
             !state.isPhoneVerified -> WebScreens.SMS
             !state.isEmailVerified -> WebScreens.Email
             !hasDocuments() -> {
                 WebScreens.WELCOME
             }
             else -> {
                 WebScreens.Main
             }
         }
    }

    private fun hasDocuments(): Boolean {
        return walletCoreDocumentsController.getAllDocuments().isNotEmpty()
    }
}