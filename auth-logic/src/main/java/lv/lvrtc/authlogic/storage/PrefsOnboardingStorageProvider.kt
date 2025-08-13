// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.authlogic.storage

import lv.lvrtc.authlogic.model.OnboardingState
import lv.lvrtc.authlogic.provider.OnboardingStorageProvider
import lv.lvrtc.businesslogic.controller.PrefsController

private object OnboardingPrefsKeys {
    const val EMAIL_VERIFIED = "onboarding_email_verified"
    const val PHONE_VERIFIED = "onboarding_phone_verified"
    const val EMAIL = "onboarding_email"
    const val PHONE = "onboarding_phone"
    const val REGISTRATION_ID = "onboarding_registration_id"
}

class PrefsOnboardingStorageProvider(
    private val prefsController: PrefsController
) : OnboardingStorageProvider {

    override fun getOnboardingState(): OnboardingState {
        return OnboardingState(
            isEmailVerified = prefsController.getBool(OnboardingPrefsKeys.EMAIL_VERIFIED, false),
            isPhoneVerified = prefsController.getBool(OnboardingPrefsKeys.PHONE_VERIFIED, false),
            email = prefsController.getString(OnboardingPrefsKeys.EMAIL, ""),
            phone = prefsController.getString(OnboardingPrefsKeys.PHONE, ""),
            registrationId = prefsController.getString(OnboardingPrefsKeys.REGISTRATION_ID, "")
        )
    }

    override fun setOnboardingState(state: OnboardingState) {
        prefsController.setBool(OnboardingPrefsKeys.EMAIL_VERIFIED, state.isEmailVerified)
        prefsController.setBool(OnboardingPrefsKeys.PHONE_VERIFIED, state.isPhoneVerified)
        prefsController.setString(OnboardingPrefsKeys.EMAIL, state.email ?: "")
        prefsController.setString(OnboardingPrefsKeys.PHONE, state.phone ?: "")
        prefsController.setString(OnboardingPrefsKeys.REGISTRATION_ID, state.registrationId ?: "")
    }

    override fun clearOnboardingState() {
        prefsController.clear(OnboardingPrefsKeys.EMAIL_VERIFIED)
        prefsController.clear(OnboardingPrefsKeys.PHONE_VERIFIED)
        prefsController.clear(OnboardingPrefsKeys.EMAIL)
        prefsController.clear(OnboardingPrefsKeys.PHONE)
        prefsController.clear(OnboardingPrefsKeys.REGISTRATION_ID)
    }
}