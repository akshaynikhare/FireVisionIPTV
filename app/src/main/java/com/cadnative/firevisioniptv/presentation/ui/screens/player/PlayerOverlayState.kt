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
private const val PLAY_PAUSE_FLASH_MS = 800L
private const val GESTURE_INDICATOR_HIDE_MS = 700L
private const val ZAP_OVERLAY_HIDE_MS = 1500L
private const val LOCK_CHIP_HIDE_MS = 3000L

/** Which gesture-zone indicator is showing on the mobile player. */
internal enum class GestureIndicatorType { BRIGHTNESS, VOLUME }

internal data class GestureIndicator(val type: GestureIndicatorType, val level: Float)

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

    // Mobile gesture layer (indicator pills, zap flash, screen lock)
    var gestureIndicator by mutableStateOf<GestureIndicator?>(null)
    var gestureIndicatorReveal by mutableIntStateOf(0)
    var zapOverlayReveal by mutableIntStateOf(0)
    var screenLocked by mutableStateOf(false)
    var lockChipReveal by mutableIntStateOf(0)
    var unlockArmed by mutableStateOf(false)

    // Non-observable input bookkeeping (key handler only)
    var lastChannelSwitchTime = 0L
    var longPressConsumed = false

    val showControls get() = controlsReveal > 0
    val showFavIndicator get() = favIndicatorToken > 0
    val showInfoBar get() = infoBarReveal > 0
    val showPlayPauseFlash get() = playPauseFlashToken > 0
    val showGestureIndicator get() = gestureIndicatorReveal > 0
    val showZapOverlay get() = zapOverlayReveal > 0
    val showLockChip get() = lockChipReveal > 0

    fun revealControls() { controlsReveal = bump(controlsReveal) }
    fun hideControls() { controlsReveal = 0 }
    fun showGestureLevel(type: GestureIndicatorType, level: Float) {
        gestureIndicator = GestureIndicator(type, level.coerceIn(0f, 1f))
        gestureIndicatorReveal = bump(gestureIndicatorReveal)
    }
    fun flashZapOverlay() { zapOverlayReveal = bump(zapOverlayReveal) }
    fun revealLockChip() { lockChipReveal = bump(lockChipReveal) }
    fun lockScreen() {
        screenLocked = true
        unlockArmed = false
        controlsReveal = 0
        revealLockChip()
    }
    fun unlockScreen() {
        screenLocked = false
        unlockArmed = false
        lockChipReveal = 0
        revealControls()
    }
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
    infoBarTimeoutMs: Long,
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
    // Keyed on the timeout too: changing the setting mid-display restarts the countdown
    LaunchedEffect(state.infoBarReveal, infoBarTimeoutMs) {
        if (state.infoBarReveal > 0) {
            delay(infoBarTimeoutMs)
            state.infoBarReveal = 0
        }
    }
    LaunchedEffect(state.playPauseFlashToken) {
        if (state.playPauseFlashToken > 0) {
            delay(PLAY_PAUSE_FLASH_MS)
            state.playPauseFlashToken = 0
        }
    }
    LaunchedEffect(state.gestureIndicatorReveal) {
        if (state.gestureIndicatorReveal > 0) {
            delay(GESTURE_INDICATOR_HIDE_MS)
            state.gestureIndicatorReveal = 0
        }
    }
    LaunchedEffect(state.zapOverlayReveal) {
        if (state.zapOverlayReveal > 0) {
            delay(ZAP_OVERLAY_HIDE_MS)
            state.zapOverlayReveal = 0
        }
    }
    LaunchedEffect(state.lockChipReveal) {
        if (state.lockChipReveal > 0) {
            delay(LOCK_CHIP_HIDE_MS)
            state.lockChipReveal = 0
            state.unlockArmed = false
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
