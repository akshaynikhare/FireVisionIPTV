package com.cadnative.firevisioniptv.presentation.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.cadnative.firevisioniptv.ComposeMainActivity
import com.cadnative.firevisioniptv.presentation.ui.player.ErrorRecoveryManager
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import com.cadnative.firevisioniptv.presentation.ui.screens.player.ASPECT_MODES
import com.cadnative.firevisioniptv.presentation.ui.screens.player.MobileChromeActions
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PipRemoteActionsEffect
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerFocusOwnerEffect
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerGestureActions
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerModals
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerOverlayTimers
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerOverlays
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerPlaybackListenerEffect
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerPortraitSections
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PlayerStateOverlays
import com.cadnative.firevisioniptv.presentation.ui.screens.player.PortraitSectionActions
import com.cadnative.firevisioniptv.presentation.ui.screens.player.TvBackgroundPauseEffect
import com.cadnative.firevisioniptv.presentation.ui.screens.player.VideoPlayer
import com.cadnative.firevisioniptv.presentation.ui.screens.player.handlePlayerRootKeyEvent
import com.cadnative.firevisioniptv.presentation.ui.screens.player.playerGestures
import com.cadnative.firevisioniptv.presentation.ui.screens.player.prepareChannelStream
import com.cadnative.firevisioniptv.presentation.ui.screens.player.rememberPlayerOrientationController
import com.cadnative.firevisioniptv.presentation.ui.screens.player.rememberPlayerOverlayState
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel

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
    onNavigateToGuide: (() -> Unit)? = null,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isMobile = isMobileDevice(context)
    val overlayState = rememberPlayerOverlayState()

    // Layout inputs: configChanges in the manifest means rotation recomposes
    // instead of recreating — the branch below is pure Compose.
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isInPip by ComposeMainActivity.isInPipMode
    val isPortraitMobile = isMobile && !isLandscape && !isInPip
    val orientation = rememberPlayerOrientationController(isMobile, isLandscape)
    val mainActivity = context as? ComposeMainActivity
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

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

    // Shared back action. On TV the BACK key never reaches BackHandler while a
    // node is focused (Compose consumes ACTION_DOWN to clear focus, so key
    // tracking never starts) — the root Box onKeyEvent below handles the key
    // directly. BackHandler stays for mobile gesture/system back.
    val onBackAction = {
        when {
            overlayState.screenLocked -> overlayState.revealLockChip()
            uiState.showChannelOverlay -> viewModel.hideOverlay()
            overlayState.controlsFocused -> overlayState.exitQuickActions()
            isMobile && isLandscape -> orientation.exitFullscreen()
            else -> onNavigateBack()
        }
    }
    BackHandler { onBackAction() }

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
            val prepared = prepareChannelStream(
                context, exoPlayer, errorRecoveryManager, channel,
                catchupStartMs, catchupDurationMin
            )
            if (!prepared) {
                // A diagnostic code, not copy: onStreamDead reasons are matched
                // on by StreamErrorMessageResolver and never shown verbatim, so
                // they must not be translated. See ErrorRecoveryManager.
                viewModel.onStreamDead("Invalid stream URL")
                return@let
            }
            // Zap / channel entry reveals the info bar; control chrome only on
            // mobile — the TV quick-actions bar shows only when summoned
            if (isMobile) overlayState.revealControls()
            overlayState.revealInfoBar()
        }
    }

    PlayerPlaybackListenerEffect(
        exoPlayer = exoPlayer,
        viewModel = viewModel,
        isMobile = isMobile,
        pipController = mainActivity?.pipController,
        errorRecoveryManager = errorRecoveryManager,
        getPlayerView = { playerViewRef },
        shouldCaptureThumbnail = { uiState.isPlaying || uiState.channel != null }
    )

    TvBackgroundPauseEffect(exoPlayer)

    PipRemoteActionsEffect(
        isMobile = isMobile,
        pipController = mainActivity?.pipController,
        viewModel = viewModel
    )

    // Auto-hide timers for the transient overlays
    PlayerOverlayTimers(
        state = overlayState,
        infoBarTimeoutMs = uiState.infoBarTimeoutSeconds * 1000L,
        onCommitChannelNumber = { viewModel.switchToChannelNumber(it) }
    )

    // Program boundary crossed: re-reveal the banner with the fresh now/next
    // (skipped while a panel is open or the compact bar is pinned — it updates in place)
    LaunchedEffect(uiState.programChangedToken) {
        if (uiState.programChangedToken > 0 &&
            !uiState.showChannelOverlay && !showTracksPanel &&
            !uiState.alwaysShowProgramBar
        ) {
            overlayState.revealInfoBar()
        }
    }

    val rootFocusRequester = remember { FocusRequester() }
    val quickActionsFocusRequester = remember { FocusRequester() }
    PlayerFocusOwnerEffect(
        showChannelOverlay = uiState.showChannelOverlay,
        showTracksPanel = showTracksPanel,
        state = overlayState,
        rootFocusRequester = rootFocusRequester,
        quickActionsFocusRequester = quickActionsFocusRequester
    )

    val haptic = LocalHapticFeedback.current

    val onToggleFavorite = {
        viewModel.toggleFavorite()
        overlayState.flashFavIndicator()
        overlayState.revealControls()
    }
    val onPlayPause = {
        val nowPlaying = !exoPlayer.isPlaying
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        overlayState.flashPlayPause(isPlaying = nowPlaying)
    }
    val onCycleAspect = {
        aspectModeIndex = (aspectModeIndex + 1) % ASPECT_MODES.size
        overlayState.revealControls()
    }
    val onCycleSleepTimer = { minutes: Int? ->
        viewModel.setSleepTimer(minutes)
        overlayState.revealControls()
    }
    val onShowTracks = { showTracksPanel = true }
    val onShowChannelList = { viewModel.showOverlay() }
    val onEnterPip: () -> Unit = { mainActivity?.pipController?.enterPip() }

    val gestureActions = PlayerGestureActions(
        onToggleChrome = {
            if (overlayState.showControls) overlayState.hideControls() else overlayState.revealControls()
        },
        onPlayPause = onPlayPause,
        onLongPressFavorite = onToggleFavorite,
        onZap = { forward -> if (forward) viewModel.nextChannel() else viewModel.previousChannel() },
        onPinchAspect = { zoomIn -> aspectModeIndex = if (zoomIn) 1 else 0 }, // Zoom / Fit
        onDismissOverlay = { viewModel.hideOverlay() },
        onCancelSleepExpiry = { viewModel.cancelSleepTimerExpiry() }
    )
    val mobileChromeActions = MobileChromeActions(
        onBack = onBackAction,
        onToggleFavorite = onToggleFavorite,
        onShowChannelList = onShowChannelList,
        onShowTracks = onShowTracks,
        onCycleAspect = onCycleAspect,
        onCycleSleepTimer = onCycleSleepTimer,
        onPrevChannel = { viewModel.previousChannel() },
        onNextChannel = { viewModel.nextChannel() },
        onEnterPip = onEnterPip,
        onEnterFullscreen = { orientation.enterFullscreen() },
        onExitFullscreen = { orientation.exitFullscreen() }
    )
    // Same category-filtered order the swipe/CH± zap uses
    val zapChannels = remember(uiState.overlayChannels, uiState.channel?.id) {
        viewModel.zapChannels()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                handlePlayerRootKeyEvent(
                    keyEvent = keyEvent,
                    uiState = uiState,
                    exoPlayer = exoPlayer,
                    viewModel = viewModel,
                    state = overlayState,
                    showTracksPanel = showTracksPanel,
                    isMobile = isMobile,
                    onCloseTracksPanel = { showTracksPanel = false },
                    onBack = onBackAction,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The video Box keeps the same tree position in both layouts —
            // only its size modifier flips, so the surface never detaches.
            Box(
                modifier = (
                    if (isPortraitMobile) Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    else Modifier.fillMaxSize()
                ).then(
                    if (isMobile && !isInPip) Modifier.playerGestures(
                        state = overlayState,
                        exoPlayer = exoPlayer,
                        activity = mainActivity,
                        audioManager = audioManager,
                        haptic = haptic,
                        overlayVisible = uiState.showChannelOverlay,
                        sleepTimerExpired = uiState.sleepTimerExpired,
                        actions = gestureActions
                    ) else Modifier
                )
            ) {
                if (uiState.channel != null && !uiState.isLoading && uiState.error == null) {
                    VideoPlayer(
                        exoPlayer = exoPlayer,
                        onPlayerViewCreated = { playerViewRef = it },
                        modifier = Modifier.fillMaxSize(),
                        resizeMode = ASPECT_MODES[aspectModeIndex].first
                    )
                }

                if (!isInPip) {
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
                        isPortrait = isPortraitMobile,
                        alwaysShowInfoBar = uiState.alwaysShowProgramBar,
                        aspectLabel = stringResource(ASPECT_MODES[aspectModeIndex].second),
                        quickActionsFocusRequester = quickActionsFocusRequester,
                        onToggleFavorite = onToggleFavorite,
                        onPlayPause = onPlayPause,
                        onCycleSleepTimer = onCycleSleepTimer,
                        onCycleAspect = onCycleAspect,
                        onShowTracks = onShowTracks,
                        onShowChannelList = onShowChannelList,
                        onShowGuide = onNavigateToGuide,
                        mobileChromeActions = if (isMobile) mobileChromeActions else null
                    )
                }
            }

            if (isPortraitMobile) {
                PlayerPortraitSections(
                    uiState = uiState,
                    zapChannels = zapChannels,
                    aspectLabel = stringResource(ASPECT_MODES[aspectModeIndex].second),
                    actions = PortraitSectionActions(
                        onToggleFavorite = onToggleFavorite,
                        onShowTracks = onShowTracks,
                        onCycleAspect = onCycleAspect,
                        onCycleSleepTimer = onCycleSleepTimer,
                        onEnterPip = onEnterPip,
                        onZapTo = { viewModel.switchChannel(it) }
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isInPip) {
            PlayerModals(
                uiState = uiState,
                state = overlayState,
                viewModel = viewModel,
                exoPlayer = exoPlayer,
                isMobile = isMobile,
                showTracksPanel = showTracksPanel,
                onCloseTracksPanel = { showTracksPanel = false }
            )
        }
    }
}
