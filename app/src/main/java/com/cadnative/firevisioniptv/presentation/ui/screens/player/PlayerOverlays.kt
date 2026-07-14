package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_EXIT
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.OverlayToast
import com.cadnative.firevisioniptv.presentation.ui.components.PlayerInfoBar
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BodyOverlay
import com.cadnative.firevisioniptv.presentation.ui.theme.DisplayNumberChip
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimLight
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeLarge
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium

/**
 * All transient overlays layered above the video: info bar, toasts,
 * number chip, favorite button/indicator, play/pause flash, mobile controls.
 */
@Composable
internal fun BoxScope.PlayerOverlays(
    uiState: PlayerUiState,
    state: PlayerOverlayState,
    isMobile: Boolean,
    isPortrait: Boolean,
    alwaysShowInfoBar: Boolean,
    aspectLabel: String,
    quickActionsFocusRequester: FocusRequester,
    onToggleFavorite: () -> Unit,
    onCycleSleepTimer: (Int?) -> Unit,
    onCycleAspect: () -> Unit,
    onShowTracks: () -> Unit,
    onShowChannelList: () -> Unit,
    onShowGuide: (() -> Unit)? = null,
    mobileChromeActions: MobileChromeActions? = null
) {
    val playbackHealthy = !uiState.isStreamDead && !uiState.isRecovering
    val controlsVisible = uiState.channel != null &&
            !uiState.showChannelOverlay &&
            playbackHealthy &&
            state.showControls &&
            !state.screenLocked
    val canShowInfoBar = uiState.channel != null &&
            !uiState.showChannelOverlay &&
            playbackHealthy &&
            !state.screenLocked &&
            // Portrait's detail section below the video owns now/next
            !isPortrait
    // Full bar is revealed transiently on zap/INFO; the compact bar stays pinned
    // when enabled and the full bar isn't currently showing.
    val fullInfoBarVisible = state.showInfoBar && canShowInfoBar
    val pinnedInfoBarVisible = alwaysShowInfoBar && !state.showInfoBar && canShowInfoBar
    val infoBarVisible = fullInfoBarVisible || pinnedInfoBarVisible
    // Keep bottom-center toasts above the info bar when both are on screen
    val toastBottomPadding = if (infoBarVisible) 96.dp else 32.dp

    // Now/next info bar — revealed on zap or INFO key, auto-hides
    AnimatedVisibility(
        visible = fullInfoBarVisible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        PlayerInfoBar(
            channel = uiState.channel,
            nowPlaying = uiState.nowPlaying,
            nextProgram = uiState.nextProgram,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Pinned compact live-EPG strip — always on when enabled in Settings
    AnimatedVisibility(
        visible = pinnedInfoBarVisible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        PlayerInfoBar(
            channel = uiState.channel,
            nowPlaying = uiState.nowPlaying,
            nextProgram = uiState.nextProgram,
            modifier = Modifier.fillMaxWidth(),
            compact = true
        )
    }

    // Play/pause flash — brief centered icon after a play/pause action
    AnimatedVisibility(
        visible = state.showPlayPauseFlash && uiState.channel != null,
        enter = fadeIn(tween(DURATION_FAST, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Icon(
            imageVector = if (state.playPauseFlashPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = null,
            tint = OnVideo,
            modifier = Modifier
                .clip(CircleShape)
                .background(ScrimHeavy)
                .padding(18.dp)
                .size(44.dp)
        )
    }

    // Favorite indicator — centered brief toast after toggle
    AnimatedVisibility(
        visible = state.showFavIndicator && uiState.channel != null,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        val isFav = uiState.channel?.isFavorite == true
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(ScrimHeavy)
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (isFav) MaterialTheme.colorScheme.error else OnVideo,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (isFav) "Added to Favorites" else "Removed from Favorites",
                style = BodyOverlay,
                color = OnVideo
            )
        }
    }

    // Sleep timer countdown chip — small, top-left, only in the final minute
    val sleepRemaining = uiState.sleepTimerRemainingSeconds
    AnimatedVisibility(
        visible = sleepRemaining != null && sleepRemaining in 1..60 && !uiState.sleepTimerExpired,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(32.dp)
    ) {
        OverlayToast("Sleep in ${sleepRemaining ?: 0}s")
    }

    // Sleep timer expired — "Still watching?" prompt with a cancel window
    AnimatedVisibility(
        visible = uiState.sleepTimerExpired,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        OverlayToast("Still watching? Press any button to continue")
    }

    // Channel number entry — top-right while typing
    if (state.numberBuffer.isNotEmpty()) {
        Text(
            text = state.numberBuffer,
            style = DisplayNumberChip,
            color = OnVideo,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)
                .clip(ShapeMedium)
                .background(ScrimLight)
                .padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }

    // Key hints — bottom-center on the first few launches
    AnimatedVisibility(
        visible = state.showKeyHints && !uiState.showChannelOverlay,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = toastBottomPadding)
    ) {
        OverlayToast("OK: channels   ▲▼ ◀▶: zap   MENU: options   Hold OK: favorite   ⏪: last ch   0-9: channel #")
    }

    // Live quick-actions bar (TV) — bottom-left, auto-hides with the other
    // transient controls. Lifts above the info bar so it never overlaps the
    // channel logo. On mobile the dedicated MobilePlayerControls own the chrome.
    AnimatedVisibility(
        visible = (controlsVisible || (state.controlsFocused && uiState.channel != null &&
                !uiState.showChannelOverlay && playbackHealthy)) && !isMobile,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 24.dp, bottom = if (infoBarVisible) 120.dp else 24.dp)
    ) {
        PlayerQuickActions(
            isFavorite = uiState.channel?.isFavorite == true,
            sleepTimerMinutes = uiState.sleepTimerMinutes,
            aspectLabel = aspectLabel,
            firstActionFocusRequester = quickActionsFocusRequester,
            onFocusStateChanged = { state.controlsFocused = it },
            onToggleFavorite = onToggleFavorite,
            onCycleSleepTimer = onCycleSleepTimer,
            onCycleAspect = onCycleAspect,
            onShowTracks = onShowTracks,
            onShowChannelList = onShowChannelList,
            onShowGuide = onShowGuide
        )
    }

    // Mobile chrome (top bar + control row in landscape, fullscreen button in
    // portrait) plus the gesture-layer overlays.
    if (isMobile && mobileChromeActions != null) {
        PlayerMobileChrome(
            uiState = uiState,
            state = state,
            isPortrait = isPortrait,
            controlsVisible = controlsVisible,
            actions = mobileChromeActions
        )
        GestureLevelIndicator(state)
        ZapOverlay(
            state = state,
            channel = uiState.channel,
            nowTitle = uiState.nowPlaying?.title ?: uiState.channel?.nowProgramTitle
        )
        LockChip(state)
    }

    BufferingOverlay(uiState)
}

