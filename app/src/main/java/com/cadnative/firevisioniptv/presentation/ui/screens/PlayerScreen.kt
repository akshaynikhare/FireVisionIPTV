package com.cadnative.firevisioniptv.presentation.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_ENTRANCE
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_EXIT
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.ChannelOverlay
import com.cadnative.firevisioniptv.presentation.ui.components.DeadStreamOverlay
import com.cadnative.firevisioniptv.presentation.ui.components.ErrorState
import com.cadnative.firevisioniptv.presentation.ui.components.LoadingIndicator
import com.cadnative.firevisioniptv.presentation.ui.components.RecoveringOverlay
import com.cadnative.firevisioniptv.presentation.ui.player.ErrorRecoveryManager
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

/**
 * Player screen with ExoPlayer integration for IPTV live streaming.
 *
 * Default ExoPlayer controls (seek bar, play/pause, forward/rewind) are
 * disabled — they are not applicable to live streams. Channel switching
 * is handled via the [ChannelOverlay].
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
    val coroutineScope = rememberCoroutineScope()

    // Auto-navigate back when stream is confirmed dead
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.onNavigatedBack()
            onNavigateBack()
        }
    }

    // Back press state machine:
    // 1st back → show overlay
    // 2nd back → hide overlay
    // 3rd back within 2s → exit player
    var recentlyDismissedOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(recentlyDismissedOverlay) {
        if (recentlyDismissedOverlay) {
            delay(2000L)
            recentlyDismissedOverlay = false
        }
    }

    BackHandler {
        when {
            uiState.showChannelOverlay -> {
                viewModel.hideOverlay()
                recentlyDismissedOverlay = true
            }
            recentlyDismissedOverlay -> {
                onNavigateBack()
            }
            else -> {
                viewModel.showOverlay()
            }
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    // Wire ErrorRecoveryManager for auto-reconnect and dead-stream detection
    val errorRecoveryManager = remember(exoPlayer) {
        ErrorRecoveryManager(
            player = exoPlayer,
            scope = coroutineScope,
            onError = { message -> viewModel.onPlaybackError(message) },
            onRecovering = { attempt -> viewModel.onRecovering(attempt) },
            onRecovered = { viewModel.onRecovered() },
            onStreamDead = { message -> viewModel.onStreamDead(message) }
        )
    }

    // Load channel
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    // Set up media item when channel loads (also handles channel switching)
    LaunchedEffect(uiState.channel) {
        uiState.channel?.let { channel ->
            val url = channel.streamUrl
            if (url.isNullOrEmpty()) {
                viewModel.onStreamDead("Invalid stream URL")
                return@let
            }
            errorRecoveryManager.reset()
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Playback state listener (error handling is done by ErrorRecoveryManager)
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.updatePlaybackState(
                        isPlaying = false,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.updatePlaybackState(
                    isPlaying = isPlaying,
                    position = exoPlayer.currentPosition,
                    duration = exoPlayer.duration.coerceAtLeast(0)
                )
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            errorRecoveryManager.release()
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video player — always present when channel is loaded and no initial error
        if (uiState.channel != null && !uiState.isLoading && uiState.error == null) {
            VideoPlayer(
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading / initial error states (full-screen replacements)
        val contentState = when {
            uiState.isLoading -> "loading"
            uiState.error != null && !uiState.isStreamDead && !uiState.isRecovering -> "error"
            else -> "none"
        }

        Crossfade(
            targetState = contentState,
            animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
            label = "playerState"
        ) { state ->
            when (state) {
                "loading" -> LoadingIndicator(message = "Loading channel...")
                "error" -> ErrorState(
                    message = uiState.error ?: "Failed to load channel",
                    onRetry = {
                        viewModel.clearError()
                        viewModel.loadChannel(channelId)
                    }
                )
                else -> { /* Player is rendered separately above */ }
            }
        }

        // Recovery overlay (semi-transparent over the player)
        AnimatedVisibility(
            visible = uiState.isRecovering,
            enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_NORMAL, easing = EaseOutQuart))
        ) {
            RecoveringOverlay(attempt = uiState.recoveryAttempt, maxAttempts = 5)
        }

        // Dead stream overlay with countdown
        AnimatedVisibility(
            visible = uiState.isStreamDead,
            enter = fadeIn(tween(DURATION_ENTRANCE, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart))
        ) {
            DeadStreamOverlay(
                message = uiState.deadStreamMessage,
                countdown = uiState.deadStreamCountdown,
                onDismiss = { viewModel.cancelDeadStreamCountdown() }
            )
        }

        // Favorite button overlay — hidden when channel overlay or dead-stream is visible
        if (uiState.channel != null && !uiState.showChannelOverlay && !uiState.isStreamDead && !uiState.isRecovering) {
            val isFavorite = uiState.channel?.isFavorite == true
            IconButton(
                onClick = { viewModel.toggleFavorite() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isFavorite) Color.Red else Color.White
                )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
                )
            }
        }

        // Channel overlay
        ChannelOverlay(
            isVisible = uiState.showChannelOverlay,
            currentChannel = uiState.channel,
            channels = uiState.overlayChannels,
            categories = uiState.overlayCategories,
            selectedCategory = uiState.overlaySelectedCategory,
            isLoadingChannels = uiState.overlayIsLoadingChannels,
            isSwitchingChannel = uiState.isSwitchingChannel,
            onChannelClick = { viewModel.switchChannel(it) },
            onCategorySelected = { viewModel.loadChannelList(it) },
            onFavoriteClick = { viewModel.toggleOverlayFavorite(it) },
            onInteraction = { viewModel.resetAutoHideTimer() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VideoPlayer(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
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
