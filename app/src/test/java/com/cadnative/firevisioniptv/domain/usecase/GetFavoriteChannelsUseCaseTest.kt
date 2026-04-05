package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetFavoriteChannelsUseCaseTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var useCase: GetFavoriteChannelsUseCase

    @Before
    fun setUp() {
        favoriteRepository = mockk()
        useCase = GetFavoriteChannelsUseCase(favoriteRepository)
    }

    @Test
    fun `delegates to repository`() = runTest {
        every { favoriteRepository.getFavoriteChannels() } returns flowOf(Result.Success(emptyList()))

        useCase(Unit).first()

        verify(exactly = 1) { favoriteRepository.getFavoriteChannels() }
    }

    @Test
    fun `emits success list from repository`() = runTest {
        val channels = listOf(
            Channel(
                id = "ch1", name = "Channel 1", streamUrl = "http://stream.example.com/1.m3u8",
                logoUrl = null, category = "News", language = "en", country = "US"
            )
        )
        every { favoriteRepository.getFavoriteChannels() } returns flowOf(Result.Success(channels))

        val result = useCase(Unit).first()

        assertTrue(result is Result.Success)
        assertEquals(channels, (result as Result.Success).data)
    }

    @Test
    fun `emits error from repository`() = runTest {
        val exception = RuntimeException("fetch failed")
        every { favoriteRepository.getFavoriteChannels() } returns flowOf(Result.Error(exception))

        val result = useCase(Unit).first()

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
