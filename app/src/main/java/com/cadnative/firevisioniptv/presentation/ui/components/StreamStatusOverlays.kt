package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.animation.animateFadeIn
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.TextDim
import com.cadnative.firevisioniptv.presentation.ui.theme.TextPrimary
import com.cadnative.firevisioniptv.presentation.ui.theme.TextSecondary

@Composable
fun RecoveringOverlay(
    attempt: Int,
    maxAttempts: Int,
    modifier: Modifier = Modifier
) {
    val statusMessage = when {
        attempt <= 1 -> "Checking stream source..."
        attempt == maxAttempts -> "Last attempt..."
        else -> "Trying alternate connection..."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.animateFadeIn()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = Amber,
                strokeWidth = 3.dp
            )
            Text(
                text = "Reconnecting... ($attempt/$maxAttempts)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        }
    }
}

@Composable
fun DeadStreamOverlay(
    title: String,
    explanation: String,
    countdown: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    onDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .padding(40.dp)
                .animateFadeIn()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Stream unavailable",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            if (explanation.isNotEmpty()) {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp)
                )
            }
            if (countdown > 0) {
                Text(
                    text = "Going back in $countdown...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Press any button to stay",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }
}
