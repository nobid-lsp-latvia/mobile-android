// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.issuance

enum class IssuanceFlowUiConfig {
    NO_DOCUMENT, EXTRA_DOCUMENT;

    companion object {
        fun fromIssuanceFlowUiConfig(value: IssuanceFlowUiConfig): String {
            return try {
                value.name
            } catch (e: Exception) {
                throw RuntimeException("Wrong IssuanceFlowUiConfig")
            }
        }

        fun fromString(value: String): IssuanceFlowUiConfig {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                throw RuntimeException("Wrong IssuanceFlowUiConfig")
            }
        }
    }
}