package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.PlaybackRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SavePlaybackPositionUseCaseTest {

    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var useCase: SavePlaybackPositionUseCase

    @Before
    fun setUp() {
        playbackRepository = mockk()
        useCase = SavePlaybackPositionUseCase(playbackRepository)
    }

    @Test
    fun `valid params delegates to repository`() = runTest {
        val params = SavePlaybackPositionUseCase.Params(channelId = "ch1", position = 5000L, duration = 90000L)
        coEvery { playbackRepository.savePlaybackPosition("ch1", 5000L, 90000L) } returns Result.Success(Unit)

        useCase(params)

        coVerify(exactly = 1) { playbackRepository.savePlaybackPosition("ch1", 5000L, 90000L) }
    }

    @Test
    fun `valid params returns Success`() = runTest {
        coEvery { playbackRepository.savePlaybackPosition(any(), any(), any()) } returns Result.Success(Unit)

        val result = useCase(SavePlaybackPositionUseCase.Params(channelId = "ch1", position = 1000L, duration = 5000L))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `negative position returns Error without calling repository`() = runTest {
        val params = SavePlaybackPositionUseCase.Params(channelId = "ch1", position = -1L, duration = 5000L)

        val result = useCase(params)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is IllegalArgumentException)
        coVerify(exactly = 0) { playbackRepository.savePlaybackPosition(any(), any(), any()) }
    }

    @Test
    fun `negative duration returns Error without calling repository`() = runTest {
        val params = SavePlaybackPositionUseCase.Params(channelId = "ch1", position = 1000L, duration = -1L)

        val result = useCase(params)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is IllegalArgumentException)
        coVerify(exactly = 0) { playbackRepository.savePlaybackPosition(any(), any(), any()) }
    }
}
