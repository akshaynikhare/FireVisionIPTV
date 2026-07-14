package com.cadnative.firevisioniptv.presentation.ui.player

import androidx.media3.exoplayer.ExoPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManagerTest {

    private lateinit var player: ExoPlayer
    private val savedPositions = mutableListOf<Pair<Long, Long>>()

    @Before
    fun setup() {
        player = mockk(relaxed = true)
        savedPositions.clear()
    }

    private fun makeManager(scope: kotlinx.coroutines.CoroutineScope): PlaybackManager {
        return PlaybackManager(
            player = player,
            scope = scope,
            onSavePosition = { pos, dur -> savedPositions.add(pos to dur) }
        )
    }

    // ── startPositionSaving ──────────────────────────────────────

    @Test
    fun `startPositionSaving calls onSavePosition after 5 seconds when playing`() = runTest {
        every { player.isPlaying } returns true
        every { player.currentPosition } returns 1000L
        every { player.duration } returns 30000L

        val manager = makeManager(this)
        manager.startPositionSaving()

        advanceTimeBy(5001)
        runCurrent()

        manager.stopPositionSaving()

        assertEquals(1, savedPositions.size)
        assertEquals(1000L to 30000L, savedPositions[0])
    }

    @Test
    fun `startPositionSaving does NOT call onSavePosition when not playing`() = runTest {
        every { player.isPlaying } returns false
        every { player.duration } returns 30000L

        val manager = makeManager(this)
        manager.startPositionSaving()

        advanceTimeBy(5001)
        runCurrent()

        manager.stopPositionSaving()

        assertEquals(0, savedPositions.size)
    }

    @Test
    fun `startPositionSaving does NOT call onSavePosition when duration is 0`() = runTest {
        every { player.isPlaying } returns true
        every { player.currentPosition } returns 1000L
        every { player.duration } returns 0L

        val manager = makeManager(this)
        manager.startPositionSaving()

        advanceTimeBy(5001)
        runCurrent()

        manager.stopPositionSaving()

        assertEquals(0, savedPositions.size)
    }

    // ── stopPositionSaving ───────────────────────────────────────

    @Test
    fun `stopPositionSaving cancels job`() = runTest {
        every { player.isPlaying } returns true
        every { player.currentPosition } returns 1000L
        every { player.duration } returns 30000L

        val manager = makeManager(this)
        manager.startPositionSaving()
        manager.stopPositionSaving()

        advanceTimeBy(6000)
        runCurrent()

        // No save should have fired because we stopped before the 5s tick
        assertEquals(0, savedPositions.size)
    }

    // ── restorePosition ──────────────────────────────────────────

    @Test
    fun `restorePosition seeks to position when duration greater than 0`() = runTest {
        every { player.duration } returns 30000L

        val manager = makeManager(this)
        manager.restorePosition(5000L)

        verify { player.seekTo(5000L) }
    }

    @Test
    fun `restorePosition does NOT seek when position is 0`() = runTest {
        every { player.duration } returns 30000L

        val manager = makeManager(this)
        manager.restorePosition(0L)

        verify(exactly = 0) { player.seekTo(any()) }
    }

    @Test
    fun `restorePosition does NOT seek when player duration is 0 or less`() = runTest {
        every { player.duration } returns 0L

        val manager = makeManager(this)
        manager.restorePosition(5000L)

        verify(exactly = 0) { player.seekTo(any()) }
    }

    // ── switchChannel ────────────────────────────────────────────

    @Test
    fun `switchChannel replaces media item in place, prepares and plays`() = runTest {
        val manager = makeManager(this)
        manager.switchChannel("http://new-stream.m3u8")
        runCurrent()

        // Faster zapping: renderers are reused, so no stop()/clearMediaItems() teardown.
        verify(exactly = 0) { player.stop() }
        verify(exactly = 0) { player.clearMediaItems() }
        verify { player.setMediaItem(any()) }
        verify { player.prepare() }
        verify { player.play() }
    }

    @Test
    fun `switchChannel seeks to savedPosition when greater than 0`() = runTest {
        val manager = makeManager(this)
        manager.switchChannel("http://new-stream.m3u8", savedPosition = 8000L)
        runCurrent()

        verify { player.seekTo(8000L) }
    }

    @Test
    fun `switchChannel does NOT seek when savedPosition is 0`() = runTest {
        val manager = makeManager(this)
        manager.switchChannel("http://new-stream.m3u8", savedPosition = 0L)
        runCurrent()

        verify(exactly = 0) { player.seekTo(any()) }
    }

    // ── release ──────────────────────────────────────────────────

    @Test
    fun `release calls stopPositionSaving`() = runTest {
        every { player.isPlaying } returns true
        every { player.currentPosition } returns 1000L
        every { player.duration } returns 30000L

        val manager = makeManager(this)
        manager.startPositionSaving()
        manager.release()

        advanceTimeBy(6000)
        runCurrent()

        // No saves should occur after release
        assertEquals(0, savedPositions.size)
    }
}
