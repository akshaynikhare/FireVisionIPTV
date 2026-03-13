package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Custom player controls with auto-hide functionality.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    channelName: String,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    autoHideDelay: Long = 5000L
) {
    var isVisible by remember { mutableStateOf(true) }

    // Auto-hide controls after delay
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(autoHideDelay)
            isVisible = false
        }
    }

    // Show controls on any interaction
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!isVisible) {
                    Modifier
                } else {
                    Modifier
                }
            )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar with channel info and back button
                TopControlBar(
                    channelName = channelName,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Center play/pause button
                CenterControls(
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TopControlBar(
    channelName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = channelName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
    }
}

@Composable
private fun CenterControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        IconButton(
            onClick = onNext,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
