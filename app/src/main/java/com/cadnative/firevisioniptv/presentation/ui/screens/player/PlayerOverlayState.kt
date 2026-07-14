package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.cadnative.firevisioniptv.data.AppPreferences
import kotlinx.coroutines.delay

private const val CONTROLS_AUTO_HIDE_MS = 5000L
private const val FAV_INDICATOR_DURATION_MS = 2000L
private const val NUMBER_COMMIT_MS = 2000L
private const val KEY_HINT_DURATION_MS = 4000L
private const val INFO_BAR_AUTO_HIDE_MS = 4000L
private const val PLAY_PAUSE_FLASH_MS = 800L

/**
 * Transient player overlay state. Reveal/flash fields are incrementing
 * tokens so their auto-hide timers restart even while already visible.
 */
@Stable
internal class PlayerOverlayState {
    var controlsReveal by mutableIntStateOf(1)
    var favIndicatorToken by mutableIntStateOf(0)
    var infoBarReveal by mutableIntStateOf(0)
    var playPauseFlashToken by mutableIntStateOf(0)
    var playPauseFlashPlaying by mutableStateOf(true)
    var numberBuffer by mutableStateOf("")
    var showKeyHints by mutableStateOf(false)
    var controlsFocused by mutableStateOf(false)          // derived from bar onFocusChanged
    var controlsFocusRequest by mutableIntStateOf(0)      // >0 → bar claims D-pad focus

    // Non-observable input bookkeeping (key handler only)
    var lastChannelSwitchTime = 0L
    var longPressConsumed = false

    val showControls get() = controlsReveal > 0
    val showFavIndicator get() = favIndicatorToken > 0
    val showInfoBar get() = infoBarReveal > 0
    val showPlayPauseFlash get() = playPauseFlashToken > 0

    fun revealControls() { controlsReveal = bump(controlsReveal) }
    fun focusQuickActions() {
        revealControls()
        controlsFocusRequest = bump(controlsFocusRequest)
    }
    fun exitQuickActions() {
        controlsFocusRequest = 0
        revealControls()  // linger briefly, then auto-hide
    }
    fun revealInfoBar() { infoBarReveal = bump(infoBarReveal) }
    fun flashFavIndicator() { favIndicatorToken = bump(favIndicatorToken) }
    fun flashPlayPause(isPlaying: Boolean) {
        playPauseFlashPlaying = isPlaying
        playPauseFlashToken = bump(playPauseFlashToken)
    }

    private fun bump(value: Int) = if (value == Int.MAX_VALUE) 1 else value + 1
}

@Composable
internal fun rememberPlayerOverlayState(): PlayerOverlayState = remember { PlayerOverlayState() }

/** Auto-hide timers and one-shot behaviors for [PlayerOverlayState]. */
@Composable
internal fun PlayerOverlayTimers(
    state: PlayerOverlayState,
    isMobile: Boolean,
    onCommitChannelNumber: (Int) -> Unit
) {
    val context = LocalContext.current

    // Suspended while the quick-actions bar has focus; focus loss restarts a full timeout
    LaunchedEffect(state.controlsReveal, state.controlsFocused) {
        if (state.controlsReveal > 0 && !state.controlsFocused) {
            delay(CONTROLS_AUTO_HIDE_MS)
            state.controlsReveal = 0
        }
    }
    LaunchedEffect(state.favIndicatorToken) {
        if (state.favIndicatorToken > 0) {
            delay(FAV_INDICATOR_DURATION_MS)
            state.favIndicatorToken = 0
        }
    }
    LaunchedEffect(state.infoBarReveal) {
        if (state.infoBarReveal > 0) {
            delay(INFO_BAR_AUTO_HIDE_MS)
            state.infoBarReveal = 0
        }
    }
    LaunchedEffect(state.playPauseFlashToken) {
        if (state.playPauseFlashToken > 0) {
            delay(PLAY_PAUSE_FLASH_MS)
            state.playPauseFlashToken = 0
        }
    }
    // Direct channel number entry commits after a short pause
    LaunchedEffect(state.numberBuffer) {
        if (state.numberBuffer.isNotEmpty()) {
            delay(NUMBER_COMMIT_MS)
            state.numberBuffer.toIntOrNull()?.let(onCommitChannelNumber)
            state.numberBuffer = ""
        }
    }
    // One-line key hints on the first few launches (TV only)
    LaunchedEffect(Unit) {
        if (!isMobile && AppPreferences.getPlayerHintCount(context) < AppPreferences.PLAYER_HINT_MAX_SHOWS) {
            AppPreferences.incrementPlayerHintCount(context)
            state.showKeyHints = true
            delay(KEY_HINT_DURATION_MS)
            state.showKeyHints = false
        }
    }
}
