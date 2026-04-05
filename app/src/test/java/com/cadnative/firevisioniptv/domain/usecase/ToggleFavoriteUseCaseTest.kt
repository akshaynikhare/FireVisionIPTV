package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    @Before
    fun setUp() {
        favoriteRepository = mockk()
        useCase = ToggleFavoriteUseCase(favoriteRepository)
    }

    @Test
    fun `delegates to repository with channelId`() = runTest {
        val channelId = "ch42"
        coEvery { favoriteRepository.toggleFavorite(channelId) } returns Result.Success(Unit)

        useCase(channelId)

        coVerify(exactly = 1) { favoriteRepository.toggleFavorite(channelId) }
    }

    @Test
    fun `returns Success when repository succeeds`() = runTest {
        coEvery { favoriteRepository.toggleFavorite(any()) } returns Result.Success(Unit)

        val result = useCase("ch1")

        assertTrue(result is Result.Success)
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val exception = RuntimeException("toggle failed")
        coEvery { favoriteRepository.toggleFavorite(any()) } returns Result.Error(exception)

        val result = useCase("ch1")

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
