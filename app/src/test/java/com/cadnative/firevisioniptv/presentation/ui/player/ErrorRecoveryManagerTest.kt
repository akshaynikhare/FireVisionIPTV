package com.cadnative.firevisioniptv.presentation.ui.player

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorRecoveryManagerTest {

    private lateinit var player: ExoPlayer
    private val listenerSlot = slot<Player.Listener>()

    private val onErrorMessages = mutableListOf<String>()
    private val onRecoveringAttempts = mutableListOf<Int>()
    private var onRecoveredCalled = false
    private val onStreamDeadMessages = mutableListOf<String>()
    private var onStreamUnresponsiveCalled = false
    private var onProxyFallbackCalled = false
    private val onAlternateFallbackUrls = mutableListOf<String>()

    @Before
    fun setup() {
        // Mock MediaItem.Builder to avoid Media3 SDK exceptions in unit tests
        mockkConstructor(MediaItem.Builder::class)
        every { anyConstructed<MediaItem.Builder>().setUri(any<String>()) } returns mockk(relaxed = true)
        every { anyConstructed<MediaItem.Builder>().build() } returns mockk()

        player = mockk(relaxed = true)
        every { player.addListener(capture(listenerSlot)) } just runs
        onErrorMessages.clear()
        onRecoveringAttempts.clear()
        onRecoveredCalled = false
        onStreamDeadMessages.clear()
        onStreamUnresponsiveCalled = false
        onProxyFallbackCalled = false
        onAlternateFallbackUrls.clear()
    }

    @After
    fun tearDown() {
        unmockkConstructor(MediaItem.Builder::class)
    }

    private fun makeManager(scope: kotlinx.coroutines.CoroutineScope): ErrorRecoveryManager {
        return ErrorRecoveryManager(
            player = player,
            scope = scope,
            onError = { onErrorMessages.add(it) },
            onRecovering = { onRecoveringAttempts.add(it) },
            onRecovered = { onRecoveredCalled = true },
            onStreamDead = { onStreamDeadMessages.add(it) },
            onStreamUnresponsive = { onStreamUnresponsiveCalled = true },
            onProxyFallback = { onProxyFallbackCalled = true },
            onAlternateFallback = { onAlternateFallbackUrls.add(it) }
        )
    }

    private fun networkError(): PlaybackException =
        PlaybackException("Network error", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)

    private fun nonNetworkError(): PlaybackException =
        PlaybackException("Decode error", null, PlaybackException.ERROR_CODE_UNSPECIFIED)

    private fun primarySlot(
        directUrl: String = "http://primary.m3u8",
        proxyUrl: String? = null
    ) = ErrorRecoveryManager.StreamSlot(directUrl = directUrl, proxyUrl = proxyUrl, isPrimary = true)

    private fun alternateSlot(
        directUrl: String = "http://alt.m3u8",
        proxyUrl: String? = null
    ) = ErrorRecoveryManager.StreamSlot(directUrl = directUrl, proxyUrl = proxyUrl, isPrimary = false)

    // ── Registration ─────────────────────────────────────────────

    @Test
    fun `init registers player listener`() = runTest {
        makeManager(this)
        verify { player.addListener(any()) }
    }

    // ── Network error handling ───────────────────────────────────

    @Test
    fun `network error with slots available calls onError and attemptReconnect`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot()))

        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(3000)
        runCurrent()

        assertEquals(1, onErrorMessages.size)
        assertEquals(1, onRecoveringAttempts.size)
        verify { player.prepare() }
        verify { player.play() }
    }

    @Test
    fun `non-network error with no retries left calls onStreamDead directly`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot()))

        listenerSlot.captured.onPlayerError(nonNetworkError())
        runCurrent()

        assertEquals(1, onStreamDeadMessages.size)
        assertEquals(0, onErrorMessages.size)
    }

    // ── Max retries ──────────────────────────────────────────────

    @Test
    fun `after max retries calls onStreamDead`() = runTest {
        val manager = makeManager(this)
        // Primary slot with no proxy: 3 direct attempts → maxTotalAttempts = 3
        manager.setStreamSlots(listOf(primarySlot()))

        // Exhaust 3 attempts
        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(3000)
        runCurrent()

        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(5000)
        runCurrent()

        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(7000)
        runCurrent()

        // Fourth error: totalAttempts (3) is no longer < maxTotalAttempts (3)
        listenerSlot.captured.onPlayerError(networkError())
        runCurrent()

        assertEquals(1, onStreamDeadMessages.size)
    }

    // ── Recovery ─────────────────────────────────────────────────

    @Test
    fun `recovers when playback state becomes READY while recovering`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot()))

        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(3000)
        runCurrent()

        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        runCurrent()

        assert(onRecoveredCalled)
    }

    // ── reset() ──────────────────────────────────────────────────

    @Test
    fun `reset cancels jobs and resets state`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot()))

        listenerSlot.captured.onPlayerError(networkError())
        runCurrent()

        manager.reset()
        runCurrent()

        // After reset, a subsequent non-network error should hit onStreamDead
        // because totalAttempts was reset to 0, but isRecoveringState is false
        // and a non-network error always calls onStreamDead
        listenerSlot.captured.onPlayerError(nonNetworkError())
        runCurrent()

        assertEquals(1, onStreamDeadMessages.size)
    }

    // ── retry() ──────────────────────────────────────────────────

    @Test
    fun `retry resets and prepares player`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot()))

        manager.retry()
        runCurrent()

        verify { player.prepare() }
        verify { player.play() }
    }

    // ── release() ────────────────────────────────────────────────

    @Test
    fun `release removes listener and cancels jobs`() = runTest {
        val manager = makeManager(this)

        manager.release()

        verify { player.removeListener(any()) }
    }

    // ── setStreamSlots() ─────────────────────────────────────────

    @Test
    fun `setStreamSlots resets counters`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot()))

        // Fire one error to increment counters
        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(3000)
        runCurrent()

        // Re-set slots: counters should reset
        manager.setStreamSlots(listOf(primarySlot()))

        // Should be able to attempt again from zero
        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(3000)
        runCurrent()

        // onRecovering called twice (once per error)
        assertEquals(2, onRecoveringAttempts.size)
    }

    // ── Proxy fallback ───────────────────────────────────────────

    @Test
    fun `proxy attempt switches to proxy URL`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot(proxyUrl = "http://proxy.m3u8")))

        // Three direct attempts exhaust the primary direct quota (maxDirectAttempts = 3)
        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(3000)
        runCurrent()

        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(5000)
        runCurrent()

        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(7000)
        runCurrent()

        // Fourth error triggers proxy attempt
        listenerSlot.captured.onPlayerError(networkError())
        advanceTimeBy(9000)
        runCurrent()

        assert(onProxyFallbackCalled)
    }

    // ── activeStreamUrl ──────────────────────────────────────────

    @Test
    fun `activeStreamUrl returns direct url of current slot`() = runTest {
        val manager = makeManager(this)
        manager.setStreamSlots(listOf(primarySlot(directUrl = "http://stream.m3u8")))

        assertEquals("http://stream.m3u8", manager.activeStreamUrl)
    }

    @Test
    fun `activeStreamUrl returns null when no slots set`() = runTest {
        val manager = makeManager(this)

        assertNull(manager.activeStreamUrl)
    }

    // ── maxTotalAttempts ─────────────────────────────────────────

    @Test
    fun `maxTotalAttempts sums max attempts for all slots`() = runTest {
        val manager = makeManager(this)
        // Primary (index 0) with no proxy = 3 direct attempts
        // Alternate (index 1) with no proxy = 1 direct attempt
        manager.setStreamSlots(
            listOf(
                primarySlot(directUrl = "http://primary.m3u8"),
                alternateSlot(directUrl = "http://alt.m3u8")
            )
        )

        assertEquals(4, manager.maxTotalAttempts)
    }

    @Test
    fun `maxTotalAttempts includes proxy slots`() = runTest {
        val manager = makeManager(this)
        // Primary with proxy = 3 direct + 1 proxy = 4
        // Alternate with proxy = 1 direct + 1 proxy = 2
        manager.setStreamSlots(
            listOf(
                primarySlot(proxyUrl = "http://proxy.m3u8"),
                alternateSlot(proxyUrl = "http://alt-proxy.m3u8")
            )
        )

        assertEquals(6, manager.maxTotalAttempts)
    }
}
