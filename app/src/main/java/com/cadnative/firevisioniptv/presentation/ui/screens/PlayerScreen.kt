package com.cadnative.firevisioniptv.presentation.ui.screens

import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
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
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.presentation.ui.player.ErrorRecoveryManager
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.net.URLEncoder

private const val FAV_BUTTON_AUTO_HIDE_MS = 5000L
private const val CHANNEL_SWITCH_DEBOUNCE_MS = 250L

@Composable
fun PlayerScreen(
    channelId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null,
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

    // Back press state machine
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

    // Favorite button auto-hide
    var showFavButton by remember { mutableStateOf(true) }
    LaunchedEffect(showFavButton) {
        if (showFavButton) {
            delay(FAV_BUTTON_AUTO_HIDE_MS)
            showFavButton = false
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

    // Keep a ref to PlayerView for thumbnail capture
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Wire ErrorRecoveryManager with proxy and alternate fallback
    val errorRecoveryManager = remember(exoPlayer) {
        ErrorRecoveryManager(
            player = exoPlayer,
            scope = coroutineScope,
            onError = { message -> viewModel.onPlaybackError(message) },
            onRecovering = { attempt -> viewModel.onRecovering(attempt) },
            onRecovered = { viewModel.onRecovered() },
            onStreamDead = { message -> viewModel.onStreamDead(message) },
            onStreamUnresponsive = { viewModel.onStreamUnresponsive() },
            onProxyFallback = { viewModel.onProxyFallback() },
            onAlternateFallback = { streamUrl -> viewModel.onAlternateFallback(streamUrl) }
        )
    }

    // Load channel
    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    // Set up media item when channel loads
    LaunchedEffect(uiState.channel) {
        uiState.channel?.let { channel ->
            val url = channel.streamUrl
            if (url.isNullOrEmpty()) {
                viewModel.onStreamDead("Invalid stream URL")
                return@let
            }
            errorRecoveryManager.reset()

            // Build stream slots: primary + alternates, each with optional proxy
            val serverUrl = AppPreferences.getServerUrl(context).trimEnd('/')
            val tvCode = AppPreferences.getTvCode(context)
            val canProxy = tvCode.isNotEmpty() && tvCode != AppPreferences.DEFAULT_TV_CODE

            fun buildProxyUrl(streamUrl: String): String? {
                if (!canProxy) return null
                return "$serverUrl/api/v1/tv/stream/$tvCode?url=${URLEncoder.encode(streamUrl, "UTF-8")}"
            }

            val slots = mutableListOf<ErrorRecoveryManager.StreamSlot>()
            slots.add(ErrorRecoveryManager.StreamSlot(url, buildProxyUrl(url), isPrimary = true))
            channel.alternateStreamUrls.take(3).forEach { altUrl ->
                slots.add(ErrorRecoveryManager.StreamSlot(altUrl, buildProxyUrl(altUrl), isPrimary = false))
            }
            errorRecoveryManager.setStreamSlots(slots)

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            // Reset fav button visibility on channel switch
            showFavButton = true
        }
    }

    // Playback state listener
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
            // Capture thumbnail before releasing player
            if (uiState.isPlaying || uiState.channel != null) {
                capturePlayerThumbnail(playerViewRef)?.let { bitmap ->
                    viewModel.saveThumbnailFromPlayer(bitmap)
                }
            }
            exoPlayer.removeListener(listener)
            errorRecoveryManager.release()
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Debounce state for channel switching
    var lastChannelSwitchTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false

                // Menu button: toggle channel overlay (works regardless of overlay state)
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU) {
                    if (uiState.showChannelOverlay) viewModel.hideOverlay() else viewModel.showOverlay()
                    return@onKeyEvent true
                }

                // Settings button: navigate to settings
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_SETTINGS && onNavigateToSettings != null) {
                    onNavigateToSettings.invoke()
                    return@onKeyEvent true
                }

                // Remaining keys only apply when overlay is NOT visible
                if (uiState.showChannelOverlay) return@onKeyEvent false

                val now = System.currentTimeMillis()
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MEDIA_NEXT,
                    KeyEvent.KEYCODE_CHANNEL_UP -> {
                        if (now - lastChannelSwitchTime >= CHANNEL_SWITCH_DEBOUNCE_MS) {
                            lastChannelSwitchTime = now
                            viewModel.nextChannel()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        if (now - lastChannelSwitchTime >= CHANNEL_SWITCH_DEBOUNCE_MS) {
                            lastChannelSwitchTime = now
                            viewModel.previousChannel()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        // Toggle play/pause and show favorite button
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        showFavButton = true
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        exoPlayer.play()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        exoPlayer.pause()
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Video player
        if (uiState.channel != null && !uiState.isLoading && uiState.error == null) {
            VideoPlayer(
                exoPlayer = exoPlayer,
                onPlayerViewCreated = { playerViewRef = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading / initial error states
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
                else -> { }
            }
        }

        // Recovery overlay
        AnimatedVisibility(
            visible = uiState.isRecovering,
            enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_NORMAL, easing = EaseOutQuart))
        ) {
            RecoveringOverlay(attempt = uiState.recoveryAttempt, maxAttempts = errorRecoveryManager.maxTotalAttempts)
        }

        // Dead stream overlay
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

        // Favorite button — bottom-left, auto-hides, focus border, press animation
        AnimatedVisibility(
            visible = uiState.channel != null
                    && !uiState.showChannelOverlay
                    && !uiState.isStreamDead
                    && !uiState.isRecovering
                    && showFavButton,
            enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            FavoriteButton(
                isFavorite = uiState.channel?.isFavorite == true,
                onClick = {
                    viewModel.toggleFavorite()
                    showFavButton = true // reset auto-hide timer
                }
            )
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
            nowProgram = uiState.nowPlaying,
            nextProgram = uiState.nextProgram,
            onChannelClick = { viewModel.switchChannel(it) },
            onCategorySelected = { viewModel.loadChannelList(it) },
            onFavoriteClick = { viewModel.toggleOverlayFavorite(it) },
            onInteraction = { viewModel.resetAutoHideTimer() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.82f
            isFocused -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favBtnScale"
    )

    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 3f else 0f,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "favBtnBorder"
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (borderWidth > 0f) Modifier.border(
                    BorderStroke(borderWidth.dp, Amber),
                    CircleShape
                ) else Modifier
            )
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                shape = CircleShape
            ),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    exoPlayer: ExoPlayer,
    onPlayerViewCreated: (PlayerView) -> Unit,
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
                onPlayerViewCreated(this)
            }
        },
        modifier = modifier
    )
}

/**
 * Capture a bitmap from the PlayerView's TextureView surface.
 * Checks that the TextureView is still available before attempting capture,
 * since the surface may already be released during dispose.
 */
@OptIn(UnstableApi::class)
private fun capturePlayerThumbnail(playerView: PlayerView?): Bitmap? {
    return try {
        val textureView = playerView?.videoSurfaceView as? TextureView
        if (textureView != null && textureView.isAvailable) {
            textureView.bitmap
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
