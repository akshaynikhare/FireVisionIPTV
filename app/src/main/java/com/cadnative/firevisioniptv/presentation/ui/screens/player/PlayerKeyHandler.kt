package com.cadnative.firevisioniptv.presentation.ui.screens.player

import android.view.KeyEvent
import androidx.media3.exoplayer.ExoPlayer
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel

internal const val CHANNEL_SWITCH_DEBOUNCE_MS = 250L
private const val LONG_PRESS_THRESHOLD_MS = 600L
private const val NUMBER_BUFFER_MAX_DIGITS = 4

/** Executes a remappable key action (see [PlayerKeyAction]). */
internal fun performKeyAction(
    prefAction: String,
    forward: Boolean,
    exoPlayer: ExoPlayer,
    viewModel: PlayerViewModel,
    state: PlayerOverlayState
) {
    when (prefAction) {
        PlayerKeyAction.ZAP -> {
            val now = System.currentTimeMillis()
            if (now - state.lastChannelSwitchTime >= CHANNEL_SWITCH_DEBOUNCE_MS) {
                state.lastChannelSwitchTime = now
                if (forward) viewModel.nextChannel() else viewModel.previousChannel()
            }
        }
        PlayerKeyAction.LAST_CHANNEL -> viewModel.recallLastChannel()
        PlayerKeyAction.FAVORITE -> {
            viewModel.toggleFavorite()
            state.flashFavIndicator()
            state.revealFavButton()
        }
        PlayerKeyAction.PLAY_PAUSE -> togglePlayPause(exoPlayer, state)
    }
}

private fun togglePlayPause(exoPlayer: ExoPlayer, state: PlayerOverlayState) {
    val nowPlaying = !exoPlayer.isPlaying
    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    state.flashPlayPause(isPlaying = nowPlaying)
}

/**
 * Remote/keyboard input handling for the player. Returns true when the
 * event is consumed. Includes long-press OK detection and 0-9 digit entry.
 */
internal fun handlePlayerKeyEvent(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    uiState: PlayerUiState,
    exoPlayer: ExoPlayer,
    viewModel: PlayerViewModel,
    state: PlayerOverlayState,
    onNavigateToSettings: (() -> Unit)?,
    onNavigateToSearch: (() -> Unit)?
): Boolean {
    val action = keyEvent.nativeKeyEvent.action
    val keyCode = keyEvent.nativeKeyEvent.keyCode

    // ── Sleep timer "Still watching?" window: any key press keeps watching ──
    if (uiState.sleepTimerExpired) {
        if (action == KeyEvent.ACTION_DOWN) {
            viewModel.cancelSleepTimerExpiry()
            exoPlayer.play()
        }
        return true
    }
    if (action == KeyEvent.ACTION_DOWN) viewModel.onUserInteraction()

    // ── Long-press detection for DPAD_CENTER / ENTER (remappable action) ──
    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
        if (uiState.showChannelOverlay) return false
        when (action) {
            KeyEvent.ACTION_DOWN -> {
                if (state.centerKeyDownTime == 0L) {
                    state.centerKeyDownTime = System.currentTimeMillis()
                    state.longPressConsumed = false
                } else if (!state.longPressConsumed) {
                    val held = System.currentTimeMillis() - state.centerKeyDownTime
                    if (held >= LONG_PRESS_THRESHOLD_MS) {
                        state.longPressConsumed = true
                        performKeyAction(uiState.longOkAction, true, exoPlayer, viewModel, state)
                    }
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (!state.longPressConsumed) {
                    // Short press → open channel switcher
                    viewModel.showOverlay()
                }
                state.centerKeyDownTime = 0L
                state.longPressConsumed = false
                return true
            }
        }
    }

    if (action != KeyEvent.ACTION_DOWN) return false

    // Menu button: toggle channel overlay (works regardless of overlay state)
    if (keyCode == KeyEvent.KEYCODE_MENU) {
        if (uiState.showChannelOverlay) viewModel.hideOverlay() else viewModel.showOverlay()
        return true
    }

    if (keyCode == KeyEvent.KEYCODE_SETTINGS && onNavigateToSettings != null) {
        onNavigateToSettings()
        return true
    }

    if (keyCode == KeyEvent.KEYCODE_SEARCH && onNavigateToSearch != null) {
        onNavigateToSearch()
        return true
    }

    // Remaining keys only apply when overlay is NOT visible
    if (uiState.showChannelOverlay) return false

    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> {
            performKeyAction(uiState.keyUpDownAction, true, exoPlayer, viewModel, state)
            true
        }
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            performKeyAction(uiState.keyUpDownAction, false, exoPlayer, viewModel, state)
            true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            performKeyAction(uiState.keyLeftRightAction, true, exoPlayer, viewModel, state)
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> {
            performKeyAction(uiState.keyLeftRightAction, false, exoPlayer, viewModel, state)
            true
        }
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_CHANNEL_UP -> {
            performKeyAction(PlayerKeyAction.ZAP, true, exoPlayer, viewModel, state)
            true
        }
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
            performKeyAction(PlayerKeyAction.ZAP, false, exoPlayer, viewModel, state)
            true
        }
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
            if (state.numberBuffer.length < NUMBER_BUFFER_MAX_DIGITS) {
                state.numberBuffer += ('0' + (keyCode - KeyEvent.KEYCODE_0))
            }
            true
        }
        KeyEvent.KEYCODE_INFO -> {
            state.revealInfoBar()
            true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
            togglePlayPause(exoPlayer, state)
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
