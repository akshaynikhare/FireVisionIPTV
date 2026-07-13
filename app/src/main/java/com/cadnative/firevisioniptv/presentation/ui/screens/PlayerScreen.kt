package com.cadnative.firevisioniptv.presentation.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.cadnative.firevisioniptv.ComposeMainActivity
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.presentation.ui.components.ChannelOverlay
import com.cadnative.firevisioniptv.presentation.ui.player.ErrorRecoveryManager
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import com.cadnative.firevisioniptv.presentation.ui.player.isTvDevice
import com.cadnative.firevisioniptv.presentation.ui.screens.player.ASPECT_MODES
import com.cadnative.firevisioniptv.presentation.ui.screens.player.CHANNEL_SWITCH_DEBOUNCE_MS
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerOverlayTimers
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerOverlays
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerStateOverlays
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerTracksPanel
import com.cadnative.firevisioniptv.presentation.ui.screens.player.VideoPlayer
import com.cadnative.firevisioniptv.presentation.ui.screens.player.capturePlayerThumbnail
import com.cadnative.firevisioniptv.presentation.ui.screens.player.handlePlayerKeyEvent
import com.cadnative.firevisioniptv.presentation.ui.screens.player.rememberPlayerOverlayState
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * Full-screen video player. Stateful root: owns the ExoPlayer lifecycle,
 * error recovery wiring, and state collection. Input handling and overlay
 * composables live in `screens/player/`.
 */
@Composable
fun PlayerScreen(
    channelId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    catchupStartMs: Long = 0L,
    catchupDurationMin: Int = 0,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isMobile = isMobileDevice(context)
    val overlayState = rememberPlayerOverlayState()

    // Auto-navigate back when stream is confirmed dead
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.onNavigatedBack()
            onNavigateBack()
        }
    }

    // Sleep timer auto-off: leave the player when the cancel window lapses
    LaunchedEffect(uiState.sleepTimerNavigateBack) {
        if (uiState.sleepTimerNavigateBack) {
            viewModel.onSleepTimerNavigatedBack()
            onNavigateBack()
        }
    }

    BackHandler {
        when {
            uiState.showChannelOverlay -> viewModel.hideOverlay()
            uiState.backExitProtection && !isMobile && !overlayState.backPressedOnce ->
                overlayState.backPressedOnce = true
            else -> onNavigateBack()
        }
    }

    val exoPlayer = remember { viewModel.createPlayer() }

    // Keep a ref to PlayerView for thumbnail capture
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Aspect/zoom mode index into ASPECT_MODES (Fit → Zoom → Fill)
    var aspectModeIndex by remember { mutableIntStateOf(0) }

    // Audio/subtitle track selection panel visibility
    var showTracksPanel by remember { mutableStateOf(false) }

    // Sleep timer expiry: pause playback while the "Still watching?" prompt shows
    LaunchedEffect(uiState.sleepTimerExpired) {
        if (uiState.sleepTimerExpired) exoPlayer.pause()
    }

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

    // Track player active state for PiP
    DisposableEffect(Unit) {
        ComposeMainActivity.isPlayerActive = true
        onDispose {
            ComposeMainActivity.isPlayerActive = false
            ComposeMainActivity.isPlayerPlaying = false
        }
    }

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

            // Catch-up: play the Xtream timeshift archive URL for a past program.
            val catchupUrl = if (catchupStartMs > 0) {
                buildCatchupUrl(context, channel.id, catchupStartMs, catchupDurationMin)
            } else null

            if (catchupUrl != null) {
                // Single archive stream — no live alternates/proxy.
                errorRecoveryManager.setStreamSlots(
                    listOf(ErrorRecoveryManager.StreamSlot(catchupUrl, null, isPrimary = true))
                )
                exoPlayer.setMediaItem(MediaItem.Builder().setUri(catchupUrl).build())
            } else {
                // Build stream slots: primary + alternates, each with optional proxy
                val serverUrl = AppPreferences.getServerUrl(context).trimEnd('/')
                val tvCode = AppPreferences.getTvCode(context)
                val canProxy = tvCode.isNotEmpty() && !AppPreferences.isDemoMode(context)

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

                exoPlayer.setMediaItem(MediaItem.Builder().setUri(url).build())
            }
            exoPlayer.prepare()

            // Reveal transient chrome on every zap / channel entry
            overlayState.revealFavButton()
            overlayState.revealInfoBar()
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
                ComposeMainActivity.isPlayerPlaying = isPlaying
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

    // On TV devices: pause playback when backgrounded, resume when foregrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasPlayingBeforeStop by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner, exoPlayer) {
        if (!isTvDevice(context)) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        wasPlayingBeforeStop = exoPlayer.isPlaying
                        exoPlayer.pause()
                    }
                    Lifecycle.Event.ON_START -> {
                        if (wasPlayingBeforeStop) exoPlayer.play()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    // Immersive mode + reset orientation when leaving player on mobile
    DisposableEffect(isMobile) {
        if (isMobile) {
            (context as? Activity)?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            if (isMobile) {
                (context as? Activity)?.let { act ->
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    act.window?.let { window ->
                        val controller = WindowInsetsControllerCompat(window, window.decorView)
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }
    }

    // Auto-hide timers for the transient overlays
    PlayerOverlayTimers(
        state = overlayState,
        isMobile = isMobile,
        onCommitChannelNumber = { viewModel.switchToChannelNumber(it) }
    )

    // Focus requester so the player Box captures remote key events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Re-grab focus when the channel overlay closes
    LaunchedEffect(uiState.showChannelOverlay) {
        if (!uiState.showChannelOverlay) focusRequester.requestFocus()
    }
    // Re-grab focus when the tracks panel closes
    LaunchedEffect(showTracksPanel) {
        if (!showTracksPanel) focusRequester.requestFocus()
    }

    val haptic = LocalHapticFeedback.current

    val onToggleFavorite = {
        viewModel.toggleFavorite()
        overlayState.flashFavIndicator()
        overlayState.revealFavButton()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .focusRequester(focusRequester)
            .focusable()
            .then(
                if (isMobile) Modifier.pointerInput(uiState.showChannelOverlay) {
                    var totalDrag = 0f
                    coroutineScope {
                        launch {
                            detectTapGestures(
                                onTap = {
                                    if (uiState.showChannelOverlay) {
                                        viewModel.hideOverlay()
                                    } else {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        overlayState.revealFavButton()
                                    }
                                },
                                onLongPress = {
                                    if (!uiState.showChannelOverlay) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleFavorite()
                                    }
                                }
                            )
                        }
                        launch {
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (!uiState.showChannelOverlay) {
                                        val now = System.currentTimeMillis()
                                        if (kotlin.math.abs(totalDrag) > 100f &&
                                            now - overlayState.lastChannelSwitchTime >= CHANNEL_SWITCH_DEBOUNCE_MS
                                        ) {
                                            overlayState.lastChannelSwitchTime = now
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (totalDrag > 0) viewModel.previousChannel() else viewModel.nextChannel()
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                            )
                        }
                    }
                } else Modifier
            )
            .onKeyEvent { keyEvent ->
                // While the tracks panel is open, let its focusable rows handle keys.
                if (showTracksPanel) return@onKeyEvent false
                handlePlayerKeyEvent(
                    keyEvent = keyEvent,
                    uiState = uiState,
                    exoPlayer = exoPlayer,
                    viewModel = viewModel,
                    state = overlayState,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
    ) {
        if (uiState.channel != null && !uiState.isLoading && uiState.error == null) {
            VideoPlayer(
                exoPlayer = exoPlayer,
                onPlayerViewCreated = { playerViewRef = it },
                modifier = Modifier.fillMaxSize(),
                resizeMode = ASPECT_MODES[aspectModeIndex].first
            )
        }

        PlayerStateOverlays(
            uiState = uiState,
            maxRecoveryAttempts = errorRecoveryManager.maxTotalAttempts,
            onRetry = {
                viewModel.clearError()
                viewModel.loadChannel(channelId)
            },
            onDismissDeadStream = { viewModel.cancelDeadStreamCountdown() }
        )

        PlayerOverlays(
            uiState = uiState,
            state = overlayState,
            isMobile = isMobile,
            alwaysShowInfoBar = uiState.alwaysShowProgramBar,
            aspectLabel = ASPECT_MODES[aspectModeIndex].second,
            onToggleFavorite = onToggleFavorite,
            onCycleSleepTimer = { minutes ->
                viewModel.setSleepTimer(minutes)
                overlayState.revealFavButton()
            },
            onCycleAspect = {
                aspectModeIndex = (aspectModeIndex + 1) % ASPECT_MODES.size
                overlayState.revealFavButton()
            },
            onShowTracks = { showTracksPanel = true },
            onShowChannelList = { viewModel.showOverlay() }
        )

        if (showTracksPanel) {
            PlayerTracksPanel(
                exoPlayer = exoPlayer,
                onDismiss = { showTracksPanel = false }
            )
        }

        ChannelOverlay(
            isVisible = uiState.showChannelOverlay,
            currentChannel = uiState.channel,
            lastChannel = uiState.lastChannel,
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
            onDismiss = { viewModel.hideOverlay() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Build an Xtream catch-up (timeshift) URL for a past program:
 * `{host}/timeshift/{user}/{pass}/{durationMin}/{yyyy-MM-dd:HH-mm}/{streamId}.m3u8`.
 * Returns null when the source isn't Xtream or credentials are missing.
 */
private fun buildCatchupUrl(
    context: android.content.Context,
    channelId: String,
    startMs: Long,
    durationMin: Int
): String? {
    if (!channelId.startsWith("xtream-")) return null
    val host = AppPreferences.getXtreamHost(context).trimEnd('/')
    val user = AppPreferences.getXtreamUser(context)
    val pass = AppPreferences.getXtreamPass(context)
    if (host.isBlank() || user.isBlank()) return null
    val streamId = channelId.removePrefix("xtream-")
    val duration = durationMin.coerceAtLeast(1)
    val start = java.time.Instant.ofEpochMilli(startMs)
        .atZone(java.time.ZoneOffset.UTC)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm"))
    return "$host/timeshift/$user/$pass/$duration/$start/$streamId.m3u8"
}
