package com.cadnative.firevisioniptv.presentation.viewmodel

import com.cadnative.firevisioniptv.MainDispatcherRule
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteCategoryDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteCategoryEntity
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.usecase.GetFavoriteChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReorderFavoritesUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getFavoriteChannelsUseCase: GetFavoriteChannelsUseCase = mockk()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mockk()
    private val reorderFavoritesUseCase: ReorderFavoritesUseCase = mockk()
    private val channelUiMapper = ChannelUiMapper()
    private val channelHealthDao: ChannelHealthDao = mockk()
    private val channelDao: ChannelDao = mockk()
    private val favoriteCategoryDao: FavoriteCategoryDao = mockk()

    private fun createChannel(id: String, name: String = "Channel $id") = Channel(
        id = id, name = name, streamUrl = "http://stream/$id",
        logoUrl = "http://logo/$id", category = "News",
        language = "en", country = "US", isFavorite = true
    )

    private fun createChannelEntity(id: String, categoryId: String = "News") = ChannelEntity(
        id = id, name = "Channel $id", streamUrl = "http://stream/$id",
        logoUrl = "http://logo/$id", categoryId = categoryId,
        language = "en", country = "US", groupTitle = null, tvgId = null, tvgName = null
    )

    private fun setupDefaultMocks(
        favorites: List<Channel> = listOf(createChannel("ch1"), createChannel("ch2")),
        health: List<ChannelHealthEntity> = emptyList(),
        channelEntities: List<ChannelEntity> = emptyList(),
        favCategories: List<FavoriteCategoryEntity> = emptyList()
    ) {
        every { getFavoriteChannelsUseCase(Unit) } returns flowOf(Result.Success(favorites))
        every { channelHealthDao.getAllHealth() } returns flowOf(health)
        every { channelDao.getAllChannels() } returns flowOf(channelEntities)
        every { favoriteCategoryDao.getAllFavoriteCategories() } returns flowOf(favCategories)
    }

    private fun createViewModel() = FavoritesViewModel(
        getFavoriteChannelsUseCase, toggleFavoriteUseCase, reorderFavoritesUseCase,
        channelUiMapper, channelHealthDao, channelDao, favoriteCategoryDao
    )

    @Test
    fun `init loads favorites into uiState`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.favorites.size)
        assertEquals("ch1", state.favorites[0].id)
        assertEquals("ch2", state.favorites[1].id)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `init shows error when use case fails`() = runTest {
        every { getFavoriteChannelsUseCase(Unit) } returns flowOf(Result.Error(Exception("Network error")))
        every { channelHealthDao.getAllHealth() } returns flowOf(emptyList())
        every { channelDao.getAllChannels() } returns flowOf(emptyList())
        every { favoriteCategoryDao.getAllFavoriteCategories() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Network error", viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init loads favorite categories`() = runTest {
        val channelEntities = listOf(
            createChannelEntity("ch1", categoryId = "Sports"),
            createChannelEntity("ch2", categoryId = "Sports")
        )
        val favCategories = listOf(FavoriteCategoryEntity(id = 1, categoryName = "Sports"))
        setupDefaultMocks(channelEntities = channelEntities, favCategories = favCategories)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val categories = viewModel.uiState.value.favoriteCategories
        assertEquals(1, categories.size)
        assertEquals("Sports", categories[0].name)
        assertEquals(2, categories[0].channelCount)
        assertTrue(categories[0].isFavorite)
    }

    @Test
    fun `removeFavorite optimistically removes channel`() = runTest {
        setupDefaultMocks()
        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.removeFavorite("ch1")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.favorites.size)
        assertEquals("ch2", viewModel.uiState.value.favorites[0].id)
        coVerify(exactly = 1) { toggleFavoriteUseCase("ch1") }
    }

    @Test
    fun `removeFavorite reloads favorites on failure`() = runTest {
        setupDefaultMocks()
        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Error(Exception("Failed"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.removeFavorite("ch1")
        advanceUntilIdle()

        // After error, loadFavorites() is called which reloads from use case
        // The favorites should be restored (reload re-fetches from mock)
        assertEquals(2, viewModel.uiState.value.favorites.size)
        coVerify(exactly = 1) { toggleFavoriteUseCase("ch1") }
    }

    @Test
    fun `reorderFavorite moves item to new position`() = runTest {
        setupDefaultMocks(favorites = listOf(createChannel("ch1"), createChannel("ch2"), createChannel("ch3")))
        coEvery { reorderFavoritesUseCase(any()) } returns Result.Success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.reorderFavorite("ch1", 2)
        advanceUntilIdle()

        val ids = viewModel.uiState.value.favorites.map { it.id }
        assertEquals(listOf("ch2", "ch3", "ch1"), ids)
        coVerify { reorderFavoritesUseCase(ReorderFavoritesUseCase.Params("ch1", 2)) }
    }

    @Test
    fun `reorderFavorite reloads favorites on failure`() = runTest {
        setupDefaultMocks(favorites = listOf(createChannel("ch1"), createChannel("ch2")))
        coEvery { reorderFavoritesUseCase(any()) } returns Result.Error(Exception("Reorder failed"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.reorderFavorite("ch1", 1)
        advanceUntilIdle()

        // After error, loadFavorites() is called restoring original order
        assertEquals(2, viewModel.uiState.value.favorites.size)
        coVerify(exactly = 1) { reorderFavoritesUseCase(any()) }
    }

    @Test
    fun `reorderFavorite ignores invalid position`() = runTest {
        setupDefaultMocks(favorites = listOf(createChannel("ch1"), createChannel("ch2")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.reorderFavorite("ch1", 5)
        advanceUntilIdle()

        assertEquals(listOf("ch1", "ch2"), viewModel.uiState.value.favorites.map { it.id })
    }

    @Test
    fun `moveFavoriteUp moves item up by one`() = runTest {
        setupDefaultMocks(favorites = listOf(createChannel("ch1"), createChannel("ch2")))
        coEvery { reorderFavoritesUseCase(any()) } returns Result.Success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.moveFavoriteUp("ch2")
        advanceUntilIdle()

        assertEquals(listOf("ch2", "ch1"), viewModel.uiState.value.favorites.map { it.id })
    }

    @Test
    fun `moveFavoriteUp does nothing when already at top`() = runTest {
        setupDefaultMocks(favorites = listOf(createChannel("ch1"), createChannel("ch2")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.moveFavoriteUp("ch1")
        advanceUntilIdle()

        assertEquals(listOf("ch1", "ch2"), viewModel.uiState.value.favorites.map { it.id })
    }

    @Test
    fun `moveFavoriteDown moves item down by one`() = runTest {
        setupDefaultMocks(favorites = listOf(createChannel("ch1"), createChannel("ch2")))
        coEvery { reorderFavoritesUseCase(any()) } returns Result.Success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.moveFavoriteDown("ch1")
        advanceUntilIdle()

        assertEquals(listOf("ch2", "ch1"), viewModel.uiState.value.favorites.map { it.id })
    }

    @Test
    fun `moveFavoriteDown does nothing when already at bottom`() = runTest {
        setupDefaultMocks(favorites = listOf(createChannel("ch1"), createChannel("ch2")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.moveFavoriteDown("ch2")
        advanceUntilIdle()

        assertEquals(listOf("ch1", "ch2"), viewModel.uiState.value.favorites.map { it.id })
    }

    @Test
    fun `removeFavoriteCategory removes from state and calls DAO`() = runTest {
        val favCategories = listOf(
            FavoriteCategoryEntity(id = 1, categoryName = "Sports"),
            FavoriteCategoryEntity(id = 2, categoryName = "News")
        )
        val channelEntities = listOf(
            createChannelEntity("ch1", "Sports"),
            createChannelEntity("ch2", "News")
        )
        setupDefaultMocks(channelEntities = channelEntities, favCategories = favCategories)
        coEvery { favoriteCategoryDao.removeFavorite(any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.removeFavoriteCategory("Sports")
        advanceUntilIdle()

        val remaining = viewModel.uiState.value.favoriteCategories.map { it.name }
        assertTrue("Sports" !in remaining)
        coVerify { favoriteCategoryDao.removeFavorite("Sports") }
    }

    @Test
    fun `clearError clears error state`() = runTest {
        every { getFavoriteChannelsUseCase(Unit) } returns flowOf(Result.Error(Exception("err")))
        every { channelHealthDao.getAllHealth() } returns flowOf(emptyList())
        every { channelDao.getAllChannels() } returns flowOf(emptyList())
        every { favoriteCategoryDao.getAllFavoriteCategories() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("err", viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `retryLoadFavorites reloads data`() = runTest {
        val favoritesFlow = MutableStateFlow<Result<List<Channel>>>(Result.Error(Exception("fail")))
        every { getFavoriteChannelsUseCase(Unit) } returns favoritesFlow
        every { channelHealthDao.getAllHealth() } returns flowOf(emptyList())
        every { channelDao.getAllChannels() } returns flowOf(emptyList())
        every { favoriteCategoryDao.getAllFavoriteCategories() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("fail", viewModel.uiState.value.error)

        favoritesFlow.value = Result.Success(listOf(createChannel("ch1")))
        viewModel.retryLoadFavorites()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.favorites.size)
    }
}
