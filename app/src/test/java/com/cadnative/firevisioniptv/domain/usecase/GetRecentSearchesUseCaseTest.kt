package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.SearchHistoryRepository
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

class GetRecentSearchesUseCaseTest {

    private lateinit var searchHistoryRepository: SearchHistoryRepository
    private lateinit var useCase: GetRecentSearchesUseCase

    @Before
    fun setUp() {
        searchHistoryRepository = mockk()
        useCase = GetRecentSearchesUseCase(searchHistoryRepository)
    }

    @Test
    fun `delegates to repository with limit param`() = runTest {
        val limit = 5
        every { searchHistoryRepository.getRecentSearches(limit) } returns flowOf(Result.Success(emptyList()))

        useCase(limit).first()

        verify(exactly = 1) { searchHistoryRepository.getRecentSearches(limit) }
    }

    @Test
    fun `emits list of strings on success`() = runTest {
        val queries = listOf("football", "news channel", "bbc")
        every { searchHistoryRepository.getRecentSearches(any()) } returns flowOf(Result.Success(queries))

        val result = useCase(10).first()

        assertTrue(result is Result.Success)
        assertEquals(queries, (result as Result.Success).data)
    }

    @Test
    fun `emits error from repository`() = runTest {
        val exception = RuntimeException("db error")
        every { searchHistoryRepository.getRecentSearches(any()) } returns flowOf(Result.Error(exception))

        val result = useCase(10).first()

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
