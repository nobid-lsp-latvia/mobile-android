// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.provider

import lv.lvrtc.authlogic.model.OnboardingState

interface OnboardingStorageProvider {
    fun getOnboardingState(): OnboardingState
    fun setOnboardingState(state: OnboardingState)
    fun clearOnboardingState()
}