package com.cabletech.iptvplayer.player

import com.cabletech.iptvplayer.core.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {

    private val channel = Channel(
        id = "ch1",
        name = "Test Channel",
        streamUrl = "http://stream.example.com/live",
    )

    @Test
    fun `Idle state has no channel`() {
        val state: PlayerState = PlayerState.Idle
        assertTrue(state is PlayerState.Idle)
    }

    @Test
    fun `Loading state carries channel`() {
        val state = PlayerState.Loading(channel)
        assertEquals(channel, state.channel)
    }

    @Test
    fun `Error state tracks retry count`() {
        val state = PlayerState.Error(channel, "Timeout", retryCount = 2)
        assertEquals(2, state.retryCount)
        assertEquals("Timeout", state.message)
    }

    @Test
    fun `AspectRatio values are distinct`() {
        val modes = AspectRatio.entries.map { it.scalingMode }.toSet()
        assertEquals(AspectRatio.entries.size, modes.size)
    }
}
