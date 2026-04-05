package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.SearchHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveSearchQueryUseCaseTest {

    private lateinit var searchHistoryRepository: SearchHistoryRepository
    private lateinit var useCase: SaveSearchQueryUseCase

    @Before
    fun setUp() {
        searchHistoryRepository = mockk()
        useCase = SaveSearchQueryUseCase(searchHistoryRepository)
    }

    @Test
    fun `delegates query to repository`() = runTest {
        val query = "premier league"
        coEvery { searchHistoryRepository.saveSearch(query) } returns Result.Success(Unit)

        useCase(query)

        coVerify(exactly = 1) { searchHistoryRepository.saveSearch(query) }
    }

    @Test
    fun `returns Success when repository succeeds`() = runTest {
        coEvery { searchHistoryRepository.saveSearch(any()) } returns Result.Success(Unit)

        val result = useCase("test query")

        assertTrue(result is Result.Success)
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val exception = RuntimeException("save failed")
        coEvery { searchHistoryRepository.saveSearch(any()) } returns Result.Error(exception)

        val result = useCase("test query")

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
