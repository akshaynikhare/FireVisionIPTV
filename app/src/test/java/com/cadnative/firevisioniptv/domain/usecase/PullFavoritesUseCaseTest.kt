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

class PullFavoritesUseCaseTest {

    private lateinit var repository: FavoriteRepository
    private lateinit var useCase: PullFavoritesUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = PullFavoritesUseCase(repository)
    }

    @Test
    fun `invoke delegates to pullFavoritesFromServer`() = runTest {
        coEvery { repository.pullFavoritesFromServer() } returns Result.Success(Unit)

        val result = useCase(Unit)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.pullFavoritesFromServer() }
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        val exception = Exception("Not authorized")
        coEvery { repository.pullFavoritesFromServer() } returns Result.Error(exception)

        val result = useCase(Unit)

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `calls repository exactly once`() = runTest {
        coEvery { repository.pullFavoritesFromServer() } returns Result.Success(Unit)

        useCase(Unit)
        useCase(Unit)

        coVerify(exactly = 2) { repository.pullFavoritesFromServer() }
    }

    @Test
    fun `does not call other favorite repository methods`() = runTest {
        coEvery { repository.pullFavoritesFromServer() } returns Result.Success(Unit)

        useCase(Unit)

        coVerify(exactly = 0) { repository.syncFavorites() }
        coVerify(exactly = 0) { repository.addFavorite(any()) }
        coVerify(exactly = 0) { repository.removeFavorite(any()) }
    }
}
