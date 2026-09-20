package com.cadnative.firevisioniptv.presentation.ui.screens.player

import android.view.KeyEvent
import androidx.media3.exoplayer.ExoPlayer
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlayerKeyHandlerTest {

    private lateinit var viewModel: PlayerViewModel
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var state: PlayerOverlayState

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        exoPlayer = mockk(relaxed = true)
        state = PlayerOverlayState()
    }

    private fun composeKey(
        code: Int,
        action: Int = KeyEvent.ACTION_DOWN,
        repeat: Int = 0,
        heldMs: Long = 0L,
    ): androidx.compose.ui.input.key.KeyEvent {
        val native = mockk<KeyEvent>(relaxed = true)
        every { native.keyCode } returns code
        every { native.action } returns action
        every { native.repeatCount } returns repeat
        every { native.downTime } returns 0L
        every { native.eventTime } returns heldMs
        every { native.isLongPress } returns false
        return androidx.compose.ui.input.key.KeyEvent(native)
    }

    private fun handle(
        event: androidx.compose.ui.input.key.KeyEvent,
        uiState: PlayerUiState = PlayerUiState(),
    ): Boolean = handlePlayerKeyEvent(
        keyEvent = event,
        uiState = uiState,
        exoPlayer = exoPlayer,
        viewModel = viewModel,
        state = state,
        onNavigateToSettings = null,
        onNavigateToSearch = null
    )

    // ── Defaults (no direct zap on the D-pad) ────────────────────────

    @Test
    fun `dpad up summons quick actions by default instead of zapping`() {
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_DPAD_UP)))
        assertTrue(state.controlsFocusRequest > 0)
        verify(exactly = 0) { viewModel.nextChannel() }
    }

    @Test
    fun `dpad left summons quick actions by default`() {
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_DPAD_LEFT)))
        assertTrue(state.controlsFocusRequest > 0)
        verify(exactly = 0) { viewModel.previousChannel() }
    }

    @Test
    fun `dpad remapped to zap switches channel`() {
        val uiState = PlayerUiState(keyUpDownAction = PlayerKeyAction.ZAP)
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_DPAD_UP), uiState))
        verify(exactly = 1) { viewModel.nextChannel() }
    }

    // ── Zap dispatch + debounce (CH± hardware keys) ──────────────────

    @Test
    fun `channel up zaps forward`() {
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_CHANNEL_UP)))
        verify(exactly = 1) { viewModel.nextChannel() }
    }

    @Test
    fun `rapid zaps inside the debounce window switch only once`() {
        handle(composeKey(KeyEvent.KEYCODE_CHANNEL_UP))
        handle(composeKey(KeyEvent.KEYCODE_CHANNEL_UP))
        verify(exactly = 1) { viewModel.nextChannel() }
    }

    // ── Held-key repeats ─────────────────────────────────────────────

    @Test
    fun `held zap key repeats are consumed without switching`() {
        handle(composeKey(KeyEvent.KEYCODE_CHANNEL_UP))
        state.lastChannelSwitchTime = 0L // debounce window elapsed
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_CHANNEL_UP, repeat = 3)))
        verify(exactly = 1) { viewModel.nextChannel() }
    }

    @Test
    fun `held digit key fills the buffer once`() {
        handle(composeKey(KeyEvent.KEYCODE_5))
        handle(composeKey(KeyEvent.KEYCODE_5, repeat = 1))
        handle(composeKey(KeyEvent.KEYCODE_5, repeat = 2))
        assertEquals("5", state.numberBuffer)
    }

    @Test
    fun `held menu repeat does not re-bump the focus request`() {
        handle(composeKey(KeyEvent.KEYCODE_MENU))
        val request = state.controlsFocusRequest
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_MENU, repeat = 1)))
        assertEquals(request, state.controlsFocusRequest)
    }

    // ── Digit entry ──────────────────────────────────────────────────

    @Test
    fun `digit buffer caps at four digits`() {
        repeat(5) { handle(composeKey(KeyEvent.KEYCODE_1)) }
        assertEquals("1111", state.numberBuffer)
    }

    // ── Quick-actions bar focused ────────────────────────────────────

    @Test
    fun `vertical presses and menu exit the focused bar`() {
        for (code in listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_MENU
        )) {
            state.controlsFocused = true
            state.focusQuickActions()
            assertTrue(handle(composeKey(code)))
            assertEquals(0, state.controlsFocusRequest)
        }
    }

    @Test
    fun `horizontal presses fall through to focus traversal on the bar`() {
        state.controlsFocused = true
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_DPAD_LEFT)))
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_DPAD_RIGHT)))
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER)))
    }

    @Test
    fun `held exit key on the bar exits once and swallows repeats`() {
        state.controlsFocused = true
        state.focusQuickActions()
        val revealAfterFocus = state.controlsReveal
        handle(composeKey(KeyEvent.KEYCODE_DPAD_UP))
        val revealAfterExit = state.controlsReveal
        assertTrue(revealAfterExit > revealAfterFocus)
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_DPAD_UP, repeat = 1)))
        assertEquals(revealAfterExit, state.controlsReveal)
    }

    // ── OK short vs long press ───────────────────────────────────────

    @Test
    fun `short OK opens the channel overlay on release only`() {
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER)))
        verify(exactly = 0) { viewModel.showOverlay() }
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER, action = KeyEvent.ACTION_UP)))
        verify(exactly = 1) { viewModel.showOverlay() }
    }

    @Test
    fun `holding OK has no side effect and still opens the overlay once on release`() {
        handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER))
        handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER, repeat = 1, heldMs = 700L))
        handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER, repeat = 2, heldMs = 1500L))
        verify(exactly = 0) { viewModel.toggleFavorite() }
        verify(exactly = 0) { viewModel.showOverlay() }
        handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER, action = KeyEvent.ACTION_UP, heldMs = 1600L))
        verify(exactly = 1) { viewModel.showOverlay() }
    }

    // ── Sleep-timer "Still watching?" prompt ─────────────────────────

    @Test
    fun `sleep prompt consumes press and resumes on release`() {
        val uiState = PlayerUiState(sleepTimerExpired = true)
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER), uiState))
        verify(exactly = 0) { viewModel.cancelSleepTimerExpiry() }
        assertTrue(
            handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER, action = KeyEvent.ACTION_UP), uiState)
        )
        verify(exactly = 1) { viewModel.cancelSleepTimerExpiry() }
        verify(exactly = 1) { exoPlayer.play() }
        verify(exactly = 0) { viewModel.showOverlay() }
    }

    // ── Channel overlay open ─────────────────────────────────────────

    @Test
    fun `overlay owns the remote - directional keys pass to focus traversal`() {
        val uiState = PlayerUiState(showChannelOverlay = true)
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_DPAD_LEFT), uiState))
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_DPAD_RIGHT), uiState))
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_DPAD_CENTER), uiState))
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_CHANNEL_UP), uiState))
        assertFalse(handle(composeKey(KeyEvent.KEYCODE_1), uiState))
        verify(exactly = 0) { viewModel.nextChannel() }
    }

    @Test
    fun `menu closes the overlay`() {
        val uiState = PlayerUiState(showChannelOverlay = true)
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_MENU), uiState))
        verify(exactly = 1) { viewModel.hideOverlay() }
    }

    // ── Media keys ───────────────────────────────────────────────────

    @Test
    fun `info reveals the info bar`() {
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_INFO)))
        assertTrue(state.showInfoBar)
    }

    @Test
    fun `last channel key recalls the previous channel`() {
        assertTrue(handle(composeKey(KeyEvent.KEYCODE_LAST_CHANNEL)))
        verify(exactly = 1) { viewModel.recallLastChannel() }
    }
}
