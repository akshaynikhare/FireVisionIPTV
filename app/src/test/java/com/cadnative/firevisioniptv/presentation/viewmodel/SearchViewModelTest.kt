package com.cadnative.firevisioniptv.presentation.viewmodel

import com.cadnative.firevisioniptv.MainDispatcherRule
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.model.SearchFilter
import com.cadnative.firevisioniptv.domain.usecase.ClearSearchHistoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetRecentSearchesUseCase
import com.cadnative.firevisioniptv.domain.usecase.SaveSearchQueryUseCase
import com.cadnative.firevisioniptv.domain.usecase.SearchChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchChannelsUseCase: SearchChannelsUseCase = mockk()
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase = mockk()
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase = mockk()
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase = mockk()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mockk()
    private val channelUiMapper = ChannelUiMapper()
    private val channelHealthDao: ChannelHealthDao = mockk()

    private val testChannel = Channel(
        id = "ch1",
        name = "Test Channel",
        streamUrl = "http://test.com/stream.m3u8",
        logoUrl = "http://test.com/logo.png",
        category = "News",
        isFavorite = false,
        language = "English",
        country = "US"
    )

    private val testHealthList = listOf(
        ChannelHealthEntity(channelId = "ch1", status = "healthy", lastCheckedAt = System.currentTimeMillis())
    )

    @Before
    fun setup() {
        // Default mocks for init flows
        every { getRecentSearchesUseCase(10) } returns flowOf(Result.Success(listOf("recent1", "recent2")))
        every { channelHealthDao.getAllHealth() } returns flowOf(emptyList())
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(emptyList()))
    }

    private fun createViewModel() = SearchViewModel(
        searchChannelsUseCase = searchChannelsUseCase,
        saveSearchQueryUseCase = saveSearchQueryUseCase,
        getRecentSearchesUseCase = getRecentSearchesUseCase,
        clearSearchHistoryUseCase = clearSearchHistoryUseCase,
        toggleFavoriteUseCase = toggleFavoriteUseCase,
        channelUiMapper = channelUiMapper,
        channelHealthDao = channelHealthDao
    )

    @Test
    fun `init loads recent searches`() = runTest {
        val vm = createViewModel()
        runCurrent()

        assertEquals(listOf("recent1", "recent2"), vm.uiState.value.recentSearches)
    }

    @Test
    fun `init with recent searches error does not crash`() = runTest {
        every { getRecentSearchesUseCase(10) } returns flowOf(Result.Error(Exception("db error")))

        val vm = createViewModel()
        runCurrent()

        assertTrue(vm.uiState.value.recentSearches.isEmpty())
    }

    @Test
    fun `onQueryChange updates query in state`() = runTest {
        val vm = createViewModel()
        runCurrent()

        vm.onQueryChange("test")
        assertEquals("test", vm.uiState.value.query)
    }

    @Test
    fun `onQueryChange with blank clears results`() = runTest {
        val vm = createViewModel()
        runCurrent()

        // First set some query
        vm.onQueryChange("test")
        // Then clear it
        vm.onQueryChange("")

        assertTrue(vm.uiState.value.results.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `debounced search triggers after 300ms`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(listOf(testChannel)))
        every { channelHealthDao.getAllHealth() } returns flowOf(testHealthList)
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.onQueryChange("news")

        // Before debounce — no search yet
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)

        // After debounce
        advanceTimeBy(301)
        runCurrent()

        assertEquals(1, vm.uiState.value.results.size)
        assertEquals("ch1", vm.uiState.value.results[0].id)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `search error sets error state`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Error(Exception("Network error")))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.onQueryChange("broken")
        advanceTimeBy(301)
        runCurrent()

        assertEquals("Network error", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `searchFromHistory bypasses debounce`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(listOf(testChannel)))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.searchFromHistory("news")
        runCurrent()

        // Search executed immediately without waiting 300ms
        assertEquals("news", vm.uiState.value.query)
        assertEquals(1, vm.uiState.value.results.size)
    }

    @Test
    fun `addFilter updates active filters and re-runs search`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(listOf(testChannel)))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        // Set a query first via history (immediate)
        vm.searchFromHistory("test")
        runCurrent()

        // Add filter — should re-run search
        val filter = SearchFilter.ByCategory("News")
        vm.addFilter(filter)
        runCurrent()

        assertEquals(listOf(filter), vm.uiState.value.activeFilters)
        coVerify(atLeast = 2) { saveSearchQueryUseCase(any()) } // initial + re-run
    }

    @Test
    fun `addFilter with empty query does not search`() = runTest {
        val vm = createViewModel()
        runCurrent()

        vm.addFilter(SearchFilter.ByCategory("News"))

        assertEquals(1, vm.uiState.value.activeFilters.size)
        // No search triggered since query is blank
        coVerify(exactly = 0) { saveSearchQueryUseCase(any()) }
    }

    @Test
    fun `removeFilter updates filters and re-runs search`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(emptyList()))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        val filter = SearchFilter.ByLanguage("English")
        vm.addFilter(filter)
        vm.searchFromHistory("test")
        runCurrent()

        vm.removeFilter(filter)
        runCurrent()

        assertTrue(vm.uiState.value.activeFilters.isEmpty())
    }

    @Test
    fun `clearFilters removes all filters`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(emptyList()))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.addFilter(SearchFilter.ByCategory("News"))
        vm.addFilter(SearchFilter.ByLanguage("English"))
        vm.searchFromHistory("test")
        runCurrent()

        vm.clearFilters()
        runCurrent()

        assertTrue(vm.uiState.value.activeFilters.isEmpty())
    }

    @Test
    fun `clearHistory clears recent searches`() = runTest {
        coEvery { clearSearchHistoryUseCase(Unit) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()
        assertEquals(2, vm.uiState.value.recentSearches.size)

        vm.clearHistory()
        runCurrent()

        assertTrue(vm.uiState.value.recentSearches.isEmpty())
        coVerify { clearSearchHistoryUseCase(Unit) }
    }

    @Test
    fun `toggleFavorite optimistically toggles and reverts on error`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(listOf(testChannel)))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.searchFromHistory("test")
        runCurrent()

        // Verify initial state
        assertFalse(vm.uiState.value.results[0].isFavorite)

        // Toggle — should optimistically update
        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Error(Exception("fail"))
        vm.toggleFavorite("ch1")
        runCurrent()

        // Reverted because of error
        assertFalse(vm.uiState.value.results[0].isFavorite)
    }

    @Test
    fun `toggleFavorite success keeps toggled state`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Success(listOf(testChannel)))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.searchFromHistory("test")
        runCurrent()

        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Success(Unit)
        vm.toggleFavorite("ch1")
        runCurrent()

        assertTrue(vm.uiState.value.results[0].isFavorite)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        every { searchChannelsUseCase(any()) } returns flowOf(Result.Error(Exception("err")))
        coEvery { saveSearchQueryUseCase(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.searchFromHistory("fail")
        runCurrent()
        assertEquals("err", vm.uiState.value.error)

        vm.clearError()
        assertNull(vm.uiState.value.error)
    }
}
