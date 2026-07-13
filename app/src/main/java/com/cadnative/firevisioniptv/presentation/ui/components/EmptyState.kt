package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_TILE
import com.cadnative.firevisioniptv.presentation.ui.animation.animateFadeIn
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder

/**
 * Contextual empty state: a tinted icon medallion, an optional title, a clear
 * message and an optional focusable retry action. [title]/[icon] are additive
 * and default to sensible values so existing `EmptyState(message)` callers keep
 * working unchanged.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    title: String? = null,
    icon: ImageVector = Icons.Default.Inbox
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Space3),
            modifier = Modifier
                .padding(Dimens.ScreenPaddingHorizontalTv)
                .animateFadeIn()
        ) {
            // Tinted medallion — softer and more on-brand than a bare icon.
            Box(
                modifier = Modifier
                    .size(Dimens.StateMedallionSize)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(Dimens.IconLarge)
                )
            }

            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(Dimens.Space1))
                var isFocused by remember { mutableStateOf(false) }
                val border = if (isFocused) {
                    BorderStroke(2.dp, FocusBorder)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(
                    onClick = onRetry,
                    border = border,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .tvFocusVisuals(
                            focused = isFocused,
                            shape = MaterialTheme.shapes.medium,
                            focusedScale = FOCUS_SCALE_TILE
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                ) {
                    Text(
                        text = "Try Again",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
