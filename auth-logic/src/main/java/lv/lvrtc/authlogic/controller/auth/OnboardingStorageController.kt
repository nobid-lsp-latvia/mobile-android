// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.controller.auth

import lv.lvrtc.authlogic.config.StorageConfig
import lv.lvrtc.authlogic.model.OnboardingState

interface OnboardingStorageController {
    fun getOnboardingState(): OnboardingState
    fun setOnboardingState(state: OnboardingState)
}

class OnboardingStorageControllerImpl(private val storageConfig: StorageConfig) :
    OnboardingStorageController {

    override fun getOnboardingState(): OnboardingState {
        return storageConfig.onboardingStorageProvider.getOnboardingState()
    }

    override fun setOnboardingState(state: OnboardingState) {
        storageConfig.onboardingStorageProvider.setOnboardingState(state)
    }
}