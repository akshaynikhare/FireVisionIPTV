package com.cadnative.firevisioniptv.presentation.viewmodel

import com.cadnative.firevisioniptv.MainDispatcherRule
import com.cadnative.firevisioniptv.data.mapper.ChannelMapper
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteCategoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteCategoryEntity
import com.cadnative.firevisioniptv.data.source.remote.NetworkException
import com.cadnative.firevisioniptv.data.source.remote.UnauthorizedException
import com.cadnative.firevisioniptv.data.source.remote.ServerException
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsByCategoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.RefreshChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getChannelsUseCase: GetChannelsUseCase = mockk()
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase = mockk()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mockk()
    private val refreshChannelsUseCase: RefreshChannelsUseCase = mockk()
    private val channelUiMapper = ChannelUiMapper()
    private val channelMapper: ChannelMapper = mockk()
    private val epgRepository: EpgRepository = mockk()
    private val channelHealthDao: ChannelHealthDao = mockk()
    private val channelDao: ChannelDao = mockk()
    private val favoriteDao: FavoriteDao = mockk()
    private val playbackPositionDao: PlaybackPositionDao = mockk()
    private val favoriteCategoryDao: FavoriteCategoryDao = mockk()

    private val healthFlow = MutableStateFlow(emptyList<com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity>())

    private val testChannel = Channel(
        id = "ch1",
        name = "Test Channel",
        streamUrl = "http://test.com/stream.m3u8",
        logoUrl = "http://test.com/logo.png",
        category = "News",
        language = "English",
        country = "US"
    )

    @Before
    fun setup() {
        // Mock QRCodeWriter to throw WriterException so generateGuideQrCode() exits cleanly
        mockkConstructor(QRCodeWriter::class)
        every { anyConstructed<QRCodeWriter>().encode(any(), any(), any<Int>(), any<Int>()) } throws WriterException("mocked")

        // Default mocks for init
        coEvery { epgRepository.ensureLoaded() } returns Unit
        every { epgRepository.getNowNextIfCached(any()) } returns null
        every { channelHealthDao.getAllHealth() } returns healthFlow
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(listOf(testChannel)))
        coEvery { refreshChannelsUseCase(Unit) } returns Result.Success(Unit)

        // HomeData mocks
        every { playbackPositionDao.observeRecentlyWatchedIds(any()) } returns flowOf(emptyList())
        every { playbackPositionDao.observePopularCategoryIds(any()) } returns flowOf(emptyList())
        every { favoriteCategoryDao.getAllFavoriteCategoryNames() } returns flowOf(emptyList())
        every { channelDao.getAllChannels() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        unmockkConstructor(QRCodeWriter::class)
    }

    private fun createViewModel() = ChannelsViewModel(
        getChannelsUseCase = getChannelsUseCase,
        getChannelsByCategoryUseCase = getChannelsByCategoryUseCase,
        toggleFavoriteUseCase = toggleFavoriteUseCase,
        refreshChannelsUseCase = refreshChannelsUseCase,
        channelUiMapper = channelUiMapper,
        channelMapper = channelMapper,
        epgRepository = epgRepository,
        channelHealthDao = channelHealthDao,
        channelDao = channelDao,
        favoriteDao = favoriteDao,
        playbackPositionDao = playbackPositionDao,
        favoriteCategoryDao = favoriteCategoryDao
    )

    @Test
    fun `init loads channels successfully`() = runTest {
        val vm = createViewModel()
        // Use advanceTimeBy to get past debounce(500) on health flow
        advanceTimeBy(600)
        runCurrent()

        assertEquals(1, vm.uiState.value.channels.size)
        assertEquals("ch1", vm.uiState.value.channels[0].id)
    }

    @Test
    fun `init calls refresh`() = runTest {
        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        coVerify { refreshChannelsUseCase(Unit) }
    }

    @Test
    fun `loadChannels with category uses getChannelsByCategoryUseCase`() = runTest {
        val catChannel = testChannel.copy(id = "ch2", name = "Sports Channel", category = "Sports")
        every { getChannelsByCategoryUseCase("Sports") } returns flowOf(Result.Success(listOf(catChannel)))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        vm.loadChannels("Sports")
        advanceTimeBy(600)
        runCurrent()

        assertEquals("Sports", vm.uiState.value.selectedCategory)
        assertEquals(1, vm.uiState.value.channels.size)
        assertEquals("ch2", vm.uiState.value.channels[0].id)
    }

    @Test
    fun `loadChannels error sets error state`() = runTest {
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Error(NetworkException("No network")))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        assertEquals(ErrorType.NETWORK_ERROR, vm.uiState.value.errorType)
        assertTrue(vm.uiState.value.error?.contains("server") == true || vm.uiState.value.error?.contains("connect") == true)
    }

    @Test
    fun `loadChannels auth error sets AUTH_REQUIRED`() = runTest {
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Error(UnauthorizedException("unauthorized")))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        assertEquals(ErrorType.AUTH_REQUIRED, vm.uiState.value.errorType)
    }

    @Test
    fun `loadChannels server error sets SERVER_ERROR`() = runTest {
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Error(ServerException("server error")))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        assertEquals(ErrorType.SERVER_ERROR, vm.uiState.value.errorType)
    }

    @Test
    fun `toggleFavorite optimistically toggles`() = runTest {
        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Success(Unit)
        vm.toggleFavorite("ch1")
        runCurrent()

        assertTrue(vm.uiState.value.channels[0].isFavorite)
    }

    @Test
    fun `toggleFavorite reverts on error`() = runTest {
        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        coEvery { toggleFavoriteUseCase("ch1") } returns Result.Error(Exception("fail"))
        vm.toggleFavorite("ch1")
        runCurrent()

        assertFalse(vm.uiState.value.channels[0].isFavorite)
        assertTrue(vm.uiState.value.error != null)
    }

    @Test
    fun `toggleCategoryFavorite removes when already favorite`() = runTest {
        coEvery { favoriteCategoryDao.isFavorite("News") } returns true
        coEvery { favoriteCategoryDao.removeFavorite("News") } returns Unit

        val vm = createViewModel()
        runCurrent()

        vm.toggleCategoryFavorite("News")
        runCurrent()

        coVerify { favoriteCategoryDao.removeFavorite("News") }
    }

    @Test
    fun `toggleCategoryFavorite adds when not favorite`() = runTest {
        coEvery { favoriteCategoryDao.isFavorite("Sports") } returns false
        coEvery { favoriteCategoryDao.addFavorite(any()) } returns Unit

        val vm = createViewModel()
        runCurrent()

        vm.toggleCategoryFavorite("Sports")
        runCurrent()

        coVerify { favoriteCategoryDao.addFavorite(match { it.categoryName == "Sports" }) }
    }

    @Test
    fun `refresh success clears loading state`() = runTest {
        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        assertFalse(vm.uiState.value.isRefreshing)
        assertTrue(vm.uiState.value.isInitialLoadComplete)
    }

    @Test
    fun `refresh error with no content sets error`() = runTest {
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(emptyList()))
        coEvery { refreshChannelsUseCase(Unit) } returns Result.Error(NetworkException("offline"))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        assertEquals(ErrorType.NETWORK_ERROR, vm.uiState.value.errorType)
        assertTrue(vm.uiState.value.isInitialLoadComplete)
    }

    @Test
    fun `refresh error with existing content does not set error`() = runTest {
        coEvery { refreshChannelsUseCase(Unit) } returns Result.Error(NetworkException("offline"))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        // Has channels from successful loadChannels, so error should be cleared
        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.isInitialLoadComplete)
    }

    @Test
    fun `onResume skips first call`() = runTest {
        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        // First onResume should be skipped
        vm.onResume()
        // Verify refresh was only called once (from init)
        coVerify(exactly = 1) { refreshChannelsUseCase(Unit) }
    }

    @Test
    fun `onResume triggers refresh on second call`() = runTest {
        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        vm.onResume() // first — skipped
        vm.onResume() // second — should refresh
        runCurrent()

        coVerify(exactly = 2) { refreshChannelsUseCase(Unit) }
    }

    @Test
    fun `clearError resets error and errorType`() = runTest {
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Error(NetworkException("fail")))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        assertTrue(vm.uiState.value.error != null)

        vm.clearError()

        assertNull(vm.uiState.value.error)
        assertEquals(ErrorType.NONE, vm.uiState.value.errorType)
    }

    @Test
    fun `loadChannels extracts categories from all channels`() = runTest {
        val channels = listOf(
            testChannel.copy(id = "1", category = "News"),
            testChannel.copy(id = "2", category = "Sports"),
            testChannel.copy(id = "3", category = "News"),
            testChannel.copy(id = "4", category = "Movies")
        )
        every { getChannelsUseCase(Unit) } returns flowOf(Result.Success(channels))

        val vm = createViewModel()
        advanceTimeBy(600)
        runCurrent()

        assertEquals(listOf("Movies", "News", "Sports"), vm.uiState.value.categories)
    }

    @Test
    fun `observeFavoriteCategories updates state`() = runTest {
        val favCatFlow = MutableStateFlow(emptyList<String>())
        every { favoriteCategoryDao.getAllFavoriteCategoryNames() } returns favCatFlow

        val vm = createViewModel()
        runCurrent()

        favCatFlow.value = listOf("News", "Sports")
        runCurrent()

        assertEquals(setOf("News", "Sports"), vm.uiState.value.favoriteCategoryNames)
    }
}
