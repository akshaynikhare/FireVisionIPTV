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

class ClearSearchHistoryUseCaseTest {

    private lateinit var searchHistoryRepository: SearchHistoryRepository
    private lateinit var useCase: ClearSearchHistoryUseCase

    @Before
    fun setUp() {
        searchHistoryRepository = mockk()
        useCase = ClearSearchHistoryUseCase(searchHistoryRepository)
    }

    @Test
    fun `delegates to repository`() = runTest {
        coEvery { searchHistoryRepository.clearHistory() } returns Result.Success(Unit)

        useCase(Unit)

        coVerify(exactly = 1) { searchHistoryRepository.clearHistory() }
    }

    @Test
    fun `returns Success when repository succeeds`() = runTest {
        coEvery { searchHistoryRepository.clearHistory() } returns Result.Success(Unit)

        val result = useCase(Unit)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `propagates error from repository`() = runTest {
        val exception = RuntimeException("clear failed")
        coEvery { searchHistoryRepository.clearHistory() } returns Result.Error(exception)

        val result = useCase(Unit)

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
