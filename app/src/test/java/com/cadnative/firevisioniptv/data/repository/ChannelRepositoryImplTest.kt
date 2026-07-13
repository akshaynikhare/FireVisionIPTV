package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.mapper.ChannelMapper
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.model.dto.ChannelDto
import com.cadnative.firevisioniptv.data.source.local.ChannelLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import com.cadnative.firevisioniptv.data.source.remote.ChannelRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ChannelRepositoryImpl.
 * 
 * Tests the offline-first behavior, error handling, and data flow
 * of the repository implementation.
 */
class ChannelRepositoryImplTest {
    
    private lateinit var repository: ChannelRepositoryImpl
    private lateinit var remoteDataSource: ChannelRemoteDataSource
    private lateinit var localDataSource: ChannelLocalDataSource
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var channelMapper: ChannelMapper
    private val testDispatcher = StandardTestDispatcher()
    
    private val testChannelEntity = ChannelEntity(
        id = "1",
        name = "Test Channel",
        streamUrl = "https://example.com/stream",
        logoUrl = "https://example.com/logo.png",
        categoryId = "sports",
        language = "en",
        country = "US",
        groupTitle = "Sports",
        tvgId = "test-1",
        tvgName = "Test Channel",
        isActive = true,
        lastUpdated = System.currentTimeMillis()
    )
    
    private val testChannelDto = ChannelDto(
        id = "1",
        name = "Test Channel",
        url = "https://example.com/stream",
        channelImg = null,
        tvgLogo = "https://example.com/logo.png",
        groupTitle = "Sports",
        tvgLanguage = "en",
        tvgCountry = "US",
        tvgId = "test-1",
        tvgName = "Test Channel",
        isActive = true
    )
    
    @Before
    fun setup() {
        remoteDataSource = mockk()
        localDataSource = mockk()
        favoriteDao = mockk()
        channelMapper = ChannelMapper()
        
        repository = ChannelRepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            favoriteDao = favoriteDao,
            channelMapper = channelMapper,
            m3uDataSource = mockk(relaxed = true),
            xtreamDataSource = mockk(relaxed = true),
            context = mockk(relaxed = true),
            dispatcher = testDispatcher
        )
    }
    
    @Test
    fun `getChannels returns local data immediately`() = runTest(testDispatcher) {
        // Given
        val channels = listOf(testChannelEntity)
        val favorites = emptyList<FavoriteEntity>()
        
        every { localDataSource.getAllChannels() } returns flowOf(channels)
        every { favoriteDao.getAllFavorites() } returns flowOf(favorites)
        
        // When
        val result = repository.getChannels().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("Test Channel", data[0].name)
        assertEquals(false, data[0].isFavorite)
    }
    
    @Test
    fun `getChannels marks favorites correctly`() = runTest(testDispatcher) {
        // Given
        val channels = listOf(testChannelEntity)
        val favorites = listOf(FavoriteEntity(channelId = "1"))
        
        every { localDataSource.getAllChannels() } returns flowOf(channels)
        every { favoriteDao.getAllFavorites() } returns flowOf(favorites)
        
        // When
        val result = repository.getChannels().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(true, data[0].isFavorite)
    }
    
    @Test
    fun `getChannels handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { localDataSource.getAllChannels() } returns flowOf()
        every { favoriteDao.getAllFavorites() } returns flow { throw exception }
        
        // When
        val result = repository.getChannels().first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
    }
    
    @Test
    fun `getChannelById returns channel with favorite status`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getChannelById("1") } returns flowOf(testChannelEntity)
        every { favoriteDao.isFavorite("1") } returns flowOf(true)
        
        // When
        val result = repository.getChannelById("1").first()
        
        // Then
        assertTrue(result is Result.Success)
        val channel = (result as Result.Success).data
        assertEquals("Test Channel", channel.name)
        assertEquals(true, channel.isFavorite)
    }
    
    @Test
    fun `getChannelById returns error when channel not found`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getChannelById("999") } returns flowOf(null)
        every { favoriteDao.isFavorite("999") } returns flowOf(false)
        
        // When
        val result = repository.getChannelById("999").first()
        
        // Then
        assertTrue(result is Result.Error)
    }
    
    @Test
    fun `getChannelsByCategory filters by category`() = runTest(testDispatcher) {
        // Given
        val channels = listOf(testChannelEntity)
        val favorites = emptyList<FavoriteEntity>()
        
        every { localDataSource.getChannelsByCategory("sports") } returns flowOf(channels)
        every { favoriteDao.getAllFavorites() } returns flowOf(favorites)
        
        // When
        val result = repository.getChannelsByCategory("sports").first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("sports", data[0].category)
    }
    
    @Test
    fun `searchChannels returns matching channels`() = runTest(testDispatcher) {
        // Given
        val channels = listOf(testChannelEntity)
        val favorites = emptyList<FavoriteEntity>()
        
        every { localDataSource.searchChannels("Test") } returns flowOf(channels)
        every { favoriteDao.getAllFavorites() } returns flowOf(favorites)
        
        // When
        val result = repository.searchChannels("Test").first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertTrue(data[0].name.contains("Test"))
    }
    
    @Test
    fun `refreshChannels fetches from remote and updates local cache`() = runTest(testDispatcher) {
        // Given
        val remoteDtos = listOf(testChannelDto)
        coEvery { remoteDataSource.fetchChannels() } returns Result.Success(remoteDtos)
        coEvery { localDataSource.replaceAllChannels(any()) } returns Unit
        
        // When
        val result = repository.refreshChannels()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { remoteDataSource.fetchChannels() }
        coVerify { localDataSource.replaceAllChannels(any()) }
    }
    
    @Test
    fun `refreshChannels handles remote errors`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Network error")
        coEvery { remoteDataSource.fetchChannels() } returns Result.Error(exception)
        
        // When
        val result = repository.refreshChannels()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
        coVerify(exactly = 0) { localDataSource.replaceAllChannels(any()) }
    }
    
    @Test
    fun `addToFavorites adds channel to favorites`() = runTest(testDispatcher) {
        // Given
        coEvery { favoriteDao.addFavorite(any()) } returns Unit
        
        // When
        val result = repository.addToFavorites("1")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { favoriteDao.addFavorite(match { it.channelId == "1" }) }
    }
    
    @Test
    fun `addToFavorites handles errors`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        coEvery { favoriteDao.addFavorite(any()) } throws exception
        
        // When
        val result = repository.addToFavorites("1")
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
    }
    
    @Test
    fun `removeFromFavorites removes channel from favorites`() = runTest(testDispatcher) {
        // Given
        coEvery { favoriteDao.removeFavorite("1") } returns Unit
        
        // When
        val result = repository.removeFromFavorites("1")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { favoriteDao.removeFavorite("1") }
    }
    
    @Test
    fun `removeFromFavorites handles errors`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        coEvery { favoriteDao.removeFavorite("1") } throws exception
        
        // When
        val result = repository.removeFromFavorites("1")
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
    }
    
    @Test
    fun `getFavoriteChannels returns only favorites`() = runTest(testDispatcher) {
        // Given
        val favoriteChannels = listOf(testChannelEntity)
        every { favoriteDao.getFavoriteChannels() } returns flowOf(favoriteChannels)
        
        // When
        val result = repository.getFavoriteChannels().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals(true, data[0].isFavorite)
    }
    
    @Test
    fun `getFavoriteChannels handles errors`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { favoriteDao.getFavoriteChannels() } returns flow { throw exception }
        
        // When
        val result = repository.getFavoriteChannels().first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
    }
}
