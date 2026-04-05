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

class ReorderFavoritesUseCaseTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var useCase: ReorderFavoritesUseCase

    @Before
    fun setUp() {
        favoriteRepository = mockk()
        useCase = ReorderFavoritesUseCase(favoriteRepository)
    }

    @Test
    fun `delegates with correct channelId and newOrder`() = runTest {
        val params = ReorderFavoritesUseCase.Params(channelId = "ch5", newOrder = 3)
        coEvery { favoriteRepository.updateFavoriteOrder("ch5", 3) } returns Result.Success(Unit)

        useCase(params)

        coVerify(exactly = 1) { favoriteRepository.updateFavoriteOrder("ch5", 3) }
    }

    @Test
    fun `returns Success when repository succeeds`() = runTest {
        coEvery { favoriteRepository.updateFavoriteOrder(any(), any()) } returns Result.Success(Unit)

        val result = useCase(ReorderFavoritesUseCase.Params(channelId = "ch1", newOrder = 0))

        assertTrue(result is Result.Success)
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val exception = RuntimeException("reorder failed")
        coEvery { favoriteRepository.updateFavoriteOrder(any(), any()) } returns Result.Error(exception)

        val result = useCase(ReorderFavoritesUseCase.Params(channelId = "ch1", newOrder = 1))

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
