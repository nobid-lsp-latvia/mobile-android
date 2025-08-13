// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.corelogic.model

import lv.lvrtc.authlogic.model.BiometricCrypto

data class AuthenticationData(
    val crypto: BiometricCrypto,
    val onAuthenticationSuccess: () -> Unit
)