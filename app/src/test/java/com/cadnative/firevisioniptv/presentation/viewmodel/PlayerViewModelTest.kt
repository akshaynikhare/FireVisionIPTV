package com.cadnative.firevisioniptv.presentation.viewmodel

import com.cadnative.firevisioniptv.MainDispatcherRule
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.domain.model.PlaybackState
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.domain.service.AnalyticsHelper
import com.cadnative.firevisioniptv.domain.service.ChannelThumbnailExtractor
import com.cadnative.firevisioniptv.domain.usecase.GetChannelByIdUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsByCategoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetGuideProgramsUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetPlaybackPositionUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReportStreamPlayUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReportStreamStatusUseCase
import com.cadnative.firevisioniptv.domain.usecase.SavePlaybackPositionUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getChannelByIdUseCase: GetChannelByIdUseCase = mockk()
    private val getChannelsUseCase: GetChannelsUseCase = mockk()
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase = mockk()
    private val savePlaybackPositionUseCase: SavePlaybackPositionUseCase = mockk()
    private val getPlaybackPositionUseCase: GetPlaybackPositionUseCase = mockk()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mockk()
    private val reportStreamStatusUseCase: ReportStreamStatusUseCase = mockk()
    private val reportStreamPlayUseCase: ReportStreamPlayUseCase = mockk()
    private val channelUiMapper = ChannelUiMapper()
    private val channelHealthDao: ChannelHealthDao = mockk(relaxed = true)
    private val thumbnailExtractor: ChannelThumbnailExtractor = mockk(relaxed = true)
    private val epgRepository: EpgRepository = mockk(relaxed = true)
    private val getGuideProgramsUseCase: GetGuideProgramsUseCase = mockk()
    private val playerFactory: com.cadnative.firevisioniptv.presentation.ui.player.PlayerFactory = mockk(relaxed = true)
    private val analyticsHelper: AnalyticsHelper = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk {
        every { getBackExitProtection() } returns flowOf(true)
        every { getPlayerKeyUpDownAction() } returns flowOf(PlayerKeyAction.ZAP)
        every { getPlayerKeyLeftRightAction() } returns flowOf(PlayerKeyAction.ZAP)
        every { getPlayerLongOkAction() } returns flowOf(PlayerKeyAction.FAVORITE)
        every { getSleepTimerDefaultMinutes() } returns flowOf(0)
        every { getAlwaysShowProgramBar() } returns flowOf(false)
        every { getInfoBarTimeoutSeconds() } returns flowOf(4)
    }

    private lateinit var viewModel: PlayerViewModel

    private fun createChannel(
        id: String = "ch1",
        name: String = "Test Channel",
        category: String = "News",
        isFavorite: Boolean = false,
        tvgId: String? = null
    ) = Channel(
        id = id, name = name, streamUrl = "http://stream/$id",
        logoUrl = "http://logo/$id", category = category,
        language = "en", country = "US", tvgId = tvgId, isFavorite = isFavorite
    )

    @Before
    fun setup() {
        coEvery { savePlaybackPositionUseCase(any()) } returns Result.Success(Unit)
        coEvery { reportStreamStatusUseCase(any()) } returns Result.Success(Unit)
        coEvery { reportStreamPlayUseCase(any()) } returns Result.Success(Unit)
        every { getPlaybackPositionUseCase(any()) } returns flowOf(Result.Success(null))
        every { channelHealthDao.getAllHealth() } returns flowOf(emptyList())
        coEvery { epgRepository.getNowNext(any()) } returns Pair(null, null)
        every { epgRepository.getNowNextIfCached(any()) } returns null
        coEvery { getGuideProgramsUseCase(any()) } returns emptyMap()

        viewModel = PlayerViewModel(
            getChannelByIdUseCase, getChannelsUseCase, getChannelsByCategoryUseCase,
            savePlaybackPositionUseCase, getPlaybackPositionUseCase, toggleFavoriteUseCase,
            reportStreamStatusUseCase, reportStreamPlayUseCase, channelUiMapper,
            channelHealthDao, thumbnailExtractor, epgRepository, getGuideProgramsUseCase,
            analyticsHelper, userPreferencesRepository, playerFactory
        )
    }

    // ── Channel Loading ──────────────────────────────────────────

    @Test
    fun `loadChannel sets channel in state on success`() = runTest {
        val channel = createChannel()
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ch1", state.channel?.id)
        assertEquals("Test Channel", state.channel?.name)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadChannel sets error on failure`() = runTest {
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Error(Exception("Not found")))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        assertEquals("Not found", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadChannel restores playback position`() = runTest {
        val channel = createChannel()
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))
        every { getPlaybackPositionUseCase("ch1") } returns flowOf(
            Result.Success(PlaybackState(channelId = "ch1", position = 5000L, duration = 30000L, isPlaying = false))
        )

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        assertEquals(5000L, viewModel.uiState.value.position)
        assertEquals(30000L, viewModel.uiState.value.duration)
    }

    // ── Playback State ───────────────────────────────────────────

    @Test
    fun `updatePlaybackState updates state`() = runTest {
        // isPlaying=true triggers an infinite while(true) save loop,
        // so use isPlaying=false to test state update without hanging
        viewModel.updatePlaybackState(isPlaying = false, position = 1000L, duration = 5000L)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isPlaying)
        assertEquals(1000L, state.position)
        assertEquals(5000L, state.duration)
    }

    @Test
    fun `updateBufferingState updates buffering flag`() = runTest {
        viewModel.updateBufferingState(true)
        assertTrue(viewModel.uiState.value.isBuffering)

        viewModel.updateBufferingState(false)
        assertFalse(viewModel.uiState.value.isBuffering)
    }

    // ── Favorites ────────────────────────────────────────────────

    @Test
    fun `toggleFavorite optimistically toggles and calls use case`() = runTest {
        val channel = createChannel(isFavorite = false)
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))
        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Success(Unit)

        viewModel.loadChannel("ch1")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.channel!!.isFavorite)

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.channel!!.isFavorite)
        coVerify { toggleFavoriteUseCase("ch1") }
    }

    @Test
    fun `toggleFavorite reverts on error`() = runTest {
        val channel = createChannel(isFavorite = false)
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))
        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Error(Exception("fail"))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.channel!!.isFavorite)
    }

    // ── Channel Overlay ──────────────────────────────────────────

    @Test
    fun `showOverlay sets overlay visible`() = runTest {
        val channel = createChannel()
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))
        every { getChannelsByCategoryUseCase("News") } returns flowOf(Result.Success(listOf(channel)))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        viewModel.showOverlay()
        // Don't use advanceUntilIdle — resetAutoHideTimer has a 7s delay
        runCurrent()

        assertTrue(viewModel.uiState.value.showChannelOverlay)
    }

    @Test
    fun `hideOverlay hides overlay`() = runTest {
        viewModel.hideOverlay()
        assertFalse(viewModel.uiState.value.showChannelOverlay)
    }

    @Test
    fun `loadChannelList loads channels by category`() = runTest {
        val channels = listOf(createChannel("ch1", category = "Sports"), createChannel("ch2", category = "Sports"))
        every { getChannelsByCategoryUseCase("Sports") } returns flowOf(Result.Success(channels))

        viewModel.loadChannelList("Sports")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.overlayChannels.size)
        assertEquals("Sports", viewModel.uiState.value.overlaySelectedCategory)
    }

    @Test
    fun `loadChannelList loads all channels when no category`() = runTest {
        val channels = listOf(createChannel("ch1"), createChannel("ch2"))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(channels))

        viewModel.loadChannelList(null)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.overlayChannels.size)
        assertNull(viewModel.uiState.value.overlaySelectedCategory)
    }

    // ── Next / Previous Channel ──────────────────────────────────

    @Test
    fun `nextChannel switches to next channel`() = runTest {
        val channels = listOf(createChannel("ch1"), createChannel("ch2"), createChannel("ch3"))
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channels[0]))
        every { getChannelByIdUseCase("ch2") } returns flowOf(Result.Success(channels[1]))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(channels))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()
        viewModel.loadChannelList(null)
        advanceUntilIdle()

        viewModel.nextChannel()
        advanceUntilIdle()

        assertEquals("ch2", viewModel.uiState.value.channel?.id)
    }

    @Test
    fun `previousChannel goes to last when at first`() = runTest {
        val channels = listOf(createChannel("ch1"), createChannel("ch2"), createChannel("ch3"))
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channels[0]))
        every { getChannelByIdUseCase("ch3") } returns flowOf(Result.Success(channels[2]))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(channels))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()
        viewModel.loadChannelList(null)
        advanceUntilIdle()

        viewModel.previousChannel()
        advanceUntilIdle()

        assertEquals("ch3", viewModel.uiState.value.channel?.id)
    }

    // ── Switch Channel ───────────────────────────────────────────

    @Test
    fun `switchChannel to same channel just hides overlay`() = runTest {
        val channel = createChannel()
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        viewModel.switchChannel("ch1")
        runCurrent()

        assertFalse(viewModel.uiState.value.showChannelOverlay)
        assertEquals("ch1", viewModel.uiState.value.channel?.id)
    }

    @Test
    fun `switchChannel to different channel loads new channel`() = runTest {
        val ch1 = createChannel("ch1")
        val ch2 = createChannel("ch2", name = "Channel 2")
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(ch1))
        every { getChannelByIdUseCase("ch2") } returns flowOf(Result.Success(ch2))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(ch1, ch2)))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        viewModel.switchChannel("ch2")
        advanceUntilIdle()

        assertEquals("ch2", viewModel.uiState.value.channel?.id)
        assertFalse(viewModel.uiState.value.showChannelOverlay)
    }

    // ── Stream Recovery ──────────────────────────────────────────

    @Test
    fun `onPlaybackError sets error state`() = runTest {
        viewModel.onPlaybackError("Stream failed")

        assertEquals("Stream failed", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `clearError clears error`() = runTest {
        viewModel.onPlaybackError("err")
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onRecovering sets recovery state`() = runTest {
        viewModel.onRecovering(2)

        val state = viewModel.uiState.value
        assertTrue(state.isRecovering)
        assertEquals(2, state.recoveryAttempt)
        assertNull(state.error)
        assertFalse(state.isStreamDead)
    }

    @Test
    fun `onRecovered clears recovery state`() = runTest {
        viewModel.onRecovering(1)
        viewModel.onRecovered()

        val state = viewModel.uiState.value
        assertFalse(state.isRecovering)
        assertEquals(0, state.recoveryAttempt)
        assertFalse(state.isStreamDead)
        assertEquals(0, state.deadStreamCountdown)
    }

    @Test
    fun `onProxyFallback sets proxy flag`() = runTest {
        viewModel.onProxyFallback()
        assertTrue(viewModel.uiState.value.isUsingProxy)
    }

    @Test
    fun `onAlternateFallback sets stream URL and clears proxy`() = runTest {
        viewModel.onProxyFallback()
        viewModel.onAlternateFallback("http://alt.stream")

        val state = viewModel.uiState.value
        assertEquals("http://alt.stream", state.activeStreamUrl)
        assertFalse(state.isUsingProxy)
    }

    @Test
    fun `onStreamDead sets dead state and reports status`() = runTest {
        val channel = createChannel()
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        viewModel.onStreamDead("Connection refused")
        // Use runCurrent instead of advanceUntilIdle to avoid countdown loop
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isStreamDead)
        assertFalse(state.isRecovering)
        assertFalse(state.isPlaying)
        assertEquals("Stream Unavailable", state.deadStreamTitle)
        coVerify { reportStreamStatusUseCase(any()) }
    }

    @Test
    fun `cancelDeadStreamCountdown stops countdown`() = runTest {
        viewModel.cancelDeadStreamCountdown()

        val state = viewModel.uiState.value
        assertEquals(0, state.deadStreamCountdown)
        assertFalse(state.shouldNavigateBack)
    }

    @Test
    fun `onNavigatedBack clears flag`() = runTest {
        viewModel.onNavigatedBack()
        assertFalse(viewModel.uiState.value.shouldNavigateBack)
    }

    // ── Overlay Favorite Toggle ──────────────────────────────────

    @Test
    fun `toggleOverlayFavorite toggles channel in overlay list`() = runTest {
        val channels = listOf(createChannel("ch1", isFavorite = false))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(channels))
        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Success(Unit)

        viewModel.loadChannelList(null)
        advanceUntilIdle()

        viewModel.toggleOverlayFavorite("ch1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.overlayChannels[0].isFavorite)
    }

    @Test
    fun `toggleOverlayFavorite reverts on error`() = runTest {
        val channels = listOf(createChannel("ch1", isFavorite = false))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(channels))
        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Error(Exception("fail"))

        viewModel.loadChannelList(null)
        advanceUntilIdle()

        viewModel.toggleOverlayFavorite("ch1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.overlayChannels[0].isFavorite)
    }

    // ── Sleep Timer ──────────────────────────────────────────────

    @Test
    fun `setSleepTimer starts countdown and expires into still watching flow`() = runTest {
        viewModel.setSleepTimer(1)
        runCurrent()

        assertEquals(1, viewModel.uiState.value.sleepTimerMinutes)
        assertEquals(60, viewModel.uiState.value.sleepTimerRemainingSeconds)

        // Final minute ticks every second
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(30, viewModel.uiState.value.sleepTimerRemainingSeconds)

        advanceTimeBy(31_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.sleepTimerExpired)
        assertFalse(viewModel.uiState.value.sleepTimerNavigateBack)

        // Cancel window lapses → navigate back
        advanceTimeBy(61_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.sleepTimerNavigateBack)
    }

    @Test
    fun `setSleepTimer null cancels timer`() = runTest {
        viewModel.setSleepTimer(30)
        runCurrent()
        assertEquals(30, viewModel.uiState.value.sleepTimerMinutes)

        viewModel.setSleepTimer(null)
        runCurrent()

        assertNull(viewModel.uiState.value.sleepTimerMinutes)
        assertNull(viewModel.uiState.value.sleepTimerRemainingSeconds)
    }

    @Test
    fun `cancelSleepTimerExpiry clears expiry within cancel window`() = runTest {
        viewModel.setSleepTimer(1)
        advanceTimeBy(61_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.sleepTimerExpired)

        viewModel.cancelSleepTimerExpiry()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.sleepTimerExpired)
        assertNull(state.sleepTimerMinutes)
        assertFalse(state.sleepTimerNavigateBack)

        // Cancelled window must not fire navigate-back later
        advanceTimeBy(61_000)
        runCurrent()
        assertFalse(viewModel.uiState.value.sleepTimerNavigateBack)
    }

    @Test
    fun `onSleepTimerNavigatedBack clears flag`() = runTest {
        viewModel.onSleepTimerNavigatedBack()
        assertFalse(viewModel.uiState.value.sleepTimerNavigateBack)
    }

    // ── Recent Channels (recall stack) ───────────────────────────

    private fun stubChannels(vararg ids: String) {
        val channels = ids.map { createChannel(it, name = "Channel $it") }
        channels.forEach { ch ->
            every { getChannelByIdUseCase(ch.id) } returns flowOf(Result.Success(ch))
        }
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(channels))
    }

    @Test
    fun `recents stack keeps most recent first capped at three`() = runTest {
        stubChannels("ch1", "ch2", "ch3", "ch4", "ch5")
        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        listOf("ch2", "ch3", "ch4", "ch5").forEach {
            viewModel.switchChannel(it)
            advanceUntilIdle()
        }

        // Watched 1→2→3→4→5: recents = [4, 3, 2], capped at 3, current excluded
        assertEquals(listOf("ch4", "ch3", "ch2"), viewModel.uiState.value.recentChannels.map { it.id })
        assertEquals("ch4", viewModel.uiState.value.lastChannel?.id)
    }

    @Test
    fun `recents never contain the switch target and dedupe revisits`() = runTest {
        stubChannels("ch1", "ch2", "ch3")
        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        viewModel.switchChannel("ch2")
        advanceUntilIdle()
        viewModel.switchChannel("ch1") // back to a channel already in recents
        advanceUntilIdle()

        assertEquals(listOf("ch2"), viewModel.uiState.value.recentChannels.map { it.id })
    }

    @Test
    fun `recallLastChannel flips back to previous channel`() = runTest {
        stubChannels("ch1", "ch2")
        viewModel.loadChannel("ch1")
        advanceUntilIdle()
        viewModel.switchChannel("ch2")
        advanceUntilIdle()

        viewModel.recallLastChannel()
        advanceUntilIdle()

        assertEquals("ch1", viewModel.uiState.value.channel?.id)
        assertEquals("ch2", viewModel.uiState.value.lastChannel?.id)
    }

    // ── Program Boundary Watcher ─────────────────────────────────

    private fun program(title: String, startsInSec: Long, endsInSec: Long) = EpgProgram(
        channelEpgId = "epg1",
        title = title,
        description = null,
        startTime = Instant.now().plusSeconds(startsInSec),
        endTime = Instant.now().plusSeconds(endsInSec),
        icon = null
    )

    @Test
    fun `program boundary refetches now-next and bumps token`() = runTest {
        val progA = program("Program A", -600, 600)
        val progB = program("Program B", 600, 1200)
        val channel = createChannel(tvgId = "epg1")
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))
        coEvery { epgRepository.getNowNext("epg1") } returnsMany listOf(
            Pair(progA, progB), // initial fetch
            Pair(progB, null)   // refetch at the boundary
        )

        viewModel.loadChannel("ch1")
        runCurrent()
        assertEquals("Program A", viewModel.uiState.value.nowPlaying?.title)
        assertEquals(0, viewModel.uiState.value.programChangedToken)

        // Cross progA's end (+2s grace)
        advanceTimeBy(603_000)
        runCurrent()

        assertEquals("Program B", viewModel.uiState.value.nowPlaying?.title)
        assertEquals(1, viewModel.uiState.value.programChangedToken)
    }

    @Test
    fun `boundary does not fire after switching channels`() = runTest {
        val progA = program("Program A", -600, 600)
        val ch1 = createChannel("ch1", tvgId = "epg1")
        val ch2 = createChannel("ch2")
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(ch1))
        every { getChannelByIdUseCase("ch2") } returns flowOf(Result.Success(ch2))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(ch1, ch2)))
        coEvery { epgRepository.getNowNext("epg1") } returns Pair(progA, null)

        viewModel.loadChannel("ch1")
        runCurrent()
        viewModel.switchChannel("ch2")
        runCurrent()

        advanceTimeBy(700_000)
        runCurrent()

        assertEquals(0, viewModel.uiState.value.programChangedToken)
    }

    // ── Dead Stream Countdown ────────────────────────────────────

    @Test
    fun `dead stream countdown triggers navigate back after timeout`() = runTest {
        val channel = createChannel()
        every { getChannelByIdUseCase("ch1") } returns flowOf(Result.Success(channel))
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(channel)))

        viewModel.loadChannel("ch1")
        advanceUntilIdle()

        viewModel.onStreamDead("error")
        // Advance past the 5-second countdown (5 x 1000ms ticks)
        advanceTimeBy(6000)
        runCurrent()

        assertTrue(viewModel.uiState.value.shouldNavigateBack)
    }
}
