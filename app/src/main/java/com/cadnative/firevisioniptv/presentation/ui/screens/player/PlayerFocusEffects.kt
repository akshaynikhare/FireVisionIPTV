package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

// How long to wait before re-asking for quick-actions focus when the first
// request landed while the bar's entrance animation was still placing it.
private const val QUICK_ACTIONS_FOCUS_RETRY_MS = 100L

/**
 * Single focus owner for the player: channel overlay > tracks panel > quick-actions
 * bar > root box. Modals claim their own first row internally, so this stands down
 * while one is open and drops any stale bar-focus claim. The only caller of
 * requestFocus at the screen's root level.
 */
@Composable
internal fun PlayerFocusOwnerEffect(
    showChannelOverlay: Boolean,
    showTracksPanel: Boolean,
    state: PlayerOverlayState,
    rootFocusRequester: FocusRequester,
    quickActionsFocusRequester: FocusRequester
) {
    LaunchedEffect(showChannelOverlay, showTracksPanel, state.controlsFocusRequest) {
        when {
            showChannelOverlay || showTracksPanel -> {
                state.controlsFocusRequest = 0
                // A modal taking over means the bar is gone; drop any stale
                // focus claim so the key handler can't misroute D-pad input.
                state.controlsFocused = false
            }
            state.controlsFocusRequest > 0 -> {
                // The bar's AnimatedVisibility flips visible this same frame, so the
                // first request can land before the node is placed. requestFocus()
                // returns Unit and only throws when the requester owns no node at all:
                // a node that exists but cannot take focus yet makes it a silent no-op,
                // so a call that didn't throw is no proof the bar is focused. Confirm
                // against the bar's own onFocusChanged and retry once after a frame,
                // otherwise the bar sits visible with focus still on the player and
                // D-pad input keeps going to the player behind it.
                runCatching { quickActionsFocusRequester.requestFocus() }
                withFrameNanos { }
                if (!state.controlsFocused) {
                    delay(QUICK_ACTIONS_FOCUS_RETRY_MS)
                    runCatching { quickActionsFocusRequester.requestFocus() }
                    withFrameNanos { }
                }
                if (!state.controlsFocused) state.controlsFocusRequest = 0
            }
            else -> runCatching { rootFocusRequester.requestFocus() }
        }
    }
}
