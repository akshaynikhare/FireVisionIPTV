package com.cadnative.firevisioniptv.presentation.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOverlayStateTest {

    @Test
    fun `reveal tokens start hidden`() {
        val state = PlayerOverlayState()
        assertFalse(state.showControls)
        assertFalse(state.showInfoBar)
        assertFalse(state.showFavIndicator)
        assertFalse(state.showPlayPauseFlash)
    }

    @Test
    fun `reveal bumps the token so timers restart while visible`() {
        val state = PlayerOverlayState()
        state.revealInfoBar()
        val first = state.infoBarReveal
        state.revealInfoBar()
        assertTrue(state.infoBarReveal > first)
        assertTrue(state.showInfoBar)
    }

    @Test
    fun `bump wraps at max value instead of overflowing`() {
        val state = PlayerOverlayState()
        state.controlsReveal = Int.MAX_VALUE
        state.revealControls()
        assertEquals(1, state.controlsReveal)
    }

    @Test
    fun `focusQuickActions reveals the bar and claims focus`() {
        val state = PlayerOverlayState()
        state.focusQuickActions()
        assertTrue(state.showControls)
        assertTrue(state.controlsFocusRequest > 0)
    }

    @Test
    fun `exitQuickActions drops the claim but lets the bar linger`() {
        val state = PlayerOverlayState()
        state.focusQuickActions()
        state.exitQuickActions()
        assertEquals(0, state.controlsFocusRequest)
        assertTrue(state.showControls)
    }

    @Test
    fun `lockScreen hides controls and arms the chip`() {
        val state = PlayerOverlayState()
        state.revealControls()
        state.lockScreen()
        assertTrue(state.screenLocked)
        assertFalse(state.showControls)
        assertTrue(state.showLockChip)
    }

    @Test
    fun `unlockScreen restores controls`() {
        val state = PlayerOverlayState()
        state.lockScreen()
        state.unlockScreen()
        assertFalse(state.screenLocked)
        assertTrue(state.showControls)
        assertFalse(state.showLockChip)
    }

    @Test
    fun `sleep timer steps cycle off-30-60-90-120-off`() {
        assertEquals(30, nextSleepTimerStep(null))
        assertEquals(60, nextSleepTimerStep(30))
        assertEquals(90, nextSleepTimerStep(60))
        assertEquals(120, nextSleepTimerStep(90))
        assertEquals(null, nextSleepTimerStep(120))
    }
}
