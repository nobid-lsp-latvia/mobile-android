// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lv.lvrtc.corelogic.security.SecurityReason
import lv.lvrtc.uilogic.components.AppIcons
import lv.lvrtc.uilogic.components.content.*
import lv.lvrtc.uilogic.components.utils.*
import lv.lvrtc.uilogic.components.wrap.*
import lv.lvrtc.resourceslogic.R

// TODO: Add contact support?
@Composable
fun SecurityErrorScreen(
    reason: String,
    onExit: () -> Unit
) {
    ContentScreen(
        navigatableAction = ScreenNavigateAction.NONE,
        contentErrorConfig = null,
        loadingType = LoadingType.NONE
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SPACING_LARGE.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WrapIcon(
                    iconData = AppIcons.Error,
                    customTint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(96.dp)
                )

                VSpacer.ExtraLarge()

                ContentTitle(
                    title = stringResource(id = R.string.security_error_title),
                    subtitle = getErrorDescription(reason),
                    titleStyle = MaterialTheme.typography.headlineMedium,
                    subTitleStyle = MaterialTheme.typography.bodyLarge
                )
            }

            Column(
                modifier = Modifier.padding(bottom = SPACING_LARGE.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WrapPrimaryButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.security_error_exit))
                }

                VSpacer.Medium()

                Text(
                    text = stringResource(id = R.string.security_error_support),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun getErrorDescription(reason: String): String {
    return when (SecurityReason.valueOf(reason)) {
        SecurityReason.NO_SCREEN_LOCK -> stringResource(
            id = R.string.security_error_no_screen_lock,
            "SEC_002"
        )
        SecurityReason.DEVICE_ROOTED -> stringResource(
            id = R.string.security_error_root_detected,
            "SEC_001"
        )
        else -> stringResource(
            id = R.string.security_error_generic,
            reason
        )
    }
}