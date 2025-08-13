// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.uilogic.components.wrap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import lv.lvrtc.uilogic.components.utils.SIZE_MEDIUM
import lv.lvrtc.uilogic.components.utils.SPACING_MEDIUM

private val buttonsShape: RoundedCornerShape = RoundedCornerShape(SIZE_MEDIUM.dp)
private val buttonsContentPadding: PaddingValues = PaddingValues(SPACING_MEDIUM.dp)

@Composable
fun WrapPrimaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val textColor = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        MaterialTheme.colorScheme.background
    }

    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = buttonsShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = textColor,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
        ),
        contentPadding = buttonsContentPadding,
        content = content
    )
}

@Composable
fun WrapSecondaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = buttonsShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            disabledContainerColor = MaterialTheme.colorScheme.background,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.scrim,
        ),
        contentPadding = buttonsContentPadding,
        content = content
    )
}