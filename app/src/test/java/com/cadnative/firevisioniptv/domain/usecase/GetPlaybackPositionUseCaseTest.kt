package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.PlaybackState
import com.cadnative.firevisioniptv.domain.repository.PlaybackRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetPlaybackPositionUseCaseTest {

    private lateinit var playbackRepository: PlaybackRepository
    private lateinit var useCase: GetPlaybackPositionUseCase

    @Before
    fun setUp() {
        playbackRepository = mockk()
        useCase = GetPlaybackPositionUseCase(playbackRepository)
    }

    @Test
    fun `maps Long position to PlaybackState with correct channelId and position`() = runTest {
        val channelId = "ch99"
        every { playbackRepository.getPlaybackPosition(channelId) } returns flowOf(Result.Success(12345L))

        val result = useCase(channelId).first()

        assertTrue(result is Result.Success)
        val state = (result as Result.Success).data
        assertEquals(channelId, state?.channelId)
        assertEquals(12345L, state?.position)
        assertEquals(0L, state?.duration)
        assertEquals(false, state?.isPlaying)
    }

    @Test
    fun `maps null position to Result Success with null data`() = runTest {
        every { playbackRepository.getPlaybackPosition(any()) } returns flowOf(Result.Success(null))

        val result = useCase("ch1").first()

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }

    @Test
    fun `maps error to Result Error`() = runTest {
        val exception = RuntimeException("position fetch failed")
        every { playbackRepository.getPlaybackPosition(any()) } returns flowOf(Result.Error(exception))

        val result = useCase("ch1").first()

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `uses channelId as params for PlaybackState channelId`() = runTest {
        val channelId = "specific-channel-id"
        every { playbackRepository.getPlaybackPosition(channelId) } returns flowOf(Result.Success(500L))

        val result = useCase(channelId).first()

        val state = (result as Result.Success).data
        assertEquals(channelId, state?.channelId)
    }
}
