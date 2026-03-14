package com.cadnative.firevisioniptv.presentation.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cadnative.firevisioniptv.presentation.ui.components.ErrorState
import com.cadnative.firevisioniptv.presentation.ui.components.LoadingIndicator
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel

/**
 * Player screen with ExoPlayer integration for HLS streaming.
 */
@Composable
fun PlayerScreen(
    channelId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Initialize player
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                // Configure for live streaming
                playWhenReady = true
            }
    }

    // Load channel
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    // Set up media item when channel loads
    LaunchedEffect(uiState.channel) {
        uiState.channel?.let { channel ->
            val url = channel.streamUrl
            if (url.isNullOrEmpty()) return@let
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Clean up player on dispose
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> {
                LoadingIndicator(message = "Loading channel...")
            }
            uiState.error != null -> {
                ErrorState(
                    message = uiState.error ?: "Failed to load channel",
                    onRetry = { viewModel.loadChannel(channelId) }
                )
            }
            uiState.channel != null -> {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    onBack = onNavigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VideoPlayer(
    exoPlayer: ExoPlayer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                keepScreenOn = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}
