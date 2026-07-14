package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_TILE
import com.cadnative.firevisioniptv.presentation.ui.animation.animateFadeIn
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.UNKNOWN,
    onPairDevice: (() -> Unit)? = null
) {
    val icon = when (errorType) {
        ErrorType.AUTH_REQUIRED -> Icons.Default.Link
        ErrorType.NETWORK_ERROR -> Icons.Default.WifiOff
        else -> Icons.Default.Warning
    }

    // Contextual headline so the state reads as a clear problem, not just a raw
    // message dump. The message itself carries the detail below it.
    val title = when (errorType) {
        ErrorType.AUTH_REQUIRED -> "Device not paired"
        ErrorType.NETWORK_ERROR -> "Can't reach the server"
        else -> "Something went wrong"
    }

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
            // Error-tinted medallion.
            Box(
                modifier = Modifier
                    .size(Dimens.StateMedallionSize)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Dimens.IconLarge)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.Space1))

            if (errorType == ErrorType.AUTH_REQUIRED && onPairDevice != null) {
                var pairFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onPairDevice,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .tvFocusVisuals(
                            focused = pairFocused,
                            shape = MaterialTheme.shapes.medium,
                            focusedScale = FOCUS_SCALE_TILE
                        )
                        .onFocusChanged { pairFocused = it.isFocused }
                ) {
                    Text(
                        text = "Pair Now",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.Space1))
            }

            var retryFocused by remember { mutableStateOf(false) }
            val retryBorder = if (retryFocused) {
                BorderStroke(2.dp, FocusBorder)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(
                onClick = onRetry,
                border = retryBorder,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .tvFocusVisuals(
                        focused = retryFocused,
                        shape = MaterialTheme.shapes.medium,
                        focusedScale = FOCUS_SCALE_TILE
                    )
                    .onFocusChanged { retryFocused = it.isFocused }
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
