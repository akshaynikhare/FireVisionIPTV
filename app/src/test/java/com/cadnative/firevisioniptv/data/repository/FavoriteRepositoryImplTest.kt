package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.mapper.ChannelMapper
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.FavoriteLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for FavoriteRepositoryImpl.
 * 
 * Tests the offline-first behavior, background sync, error handling,
 * and conflict resolution of the favorite repository implementation.
 * 
 * Requirements: TR-006 (Network Performance - offline-first, background sync)
 */
class FavoriteRepositoryImplTest {
    
    private lateinit var repository: FavoriteRepositoryImpl
    private lateinit var localDataSource: FavoriteLocalDataSource
    private lateinit var apiService: FireVisionApiService
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
    
    private val testFavoriteEntity = FavoriteEntity(
        id = 1,
        channelId = "1",
        addedAt = System.currentTimeMillis(),
        displayOrder = 0
    )
    
    @Before
    fun setup() {
        localDataSource = mockk()
        apiService = mockk()
        channelMapper = ChannelMapper()
        
        repository = FavoriteRepositoryImpl(
            localDataSource = localDataSource,
            apiService = apiService,
            channelMapper = channelMapper,
            dispatcher = testDispatcher
        )
    }
    
    @Test
    fun `getFavoriteChannels returns local favorites immediately`() = runTest(testDispatcher) {
        // Given
        val favoriteChannels = listOf(testChannelEntity)
        every { localDataSource.getFavoriteChannels() } returns flowOf(favoriteChannels)
        
        // When
        val result = repository.getFavoriteChannels().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("Test Channel", data[0].name)
        assertEquals(true, data[0].isFavorite)
    }
    
    @Test
    fun `getFavoriteChannels handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { localDataSource.getFavoriteChannels() } throws exception
        
        // When
        val result = repository.getFavoriteChannels().first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `isFavorite returns true for favorite channel`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.isFavorite("1") } returns flowOf(true)
        
        // When
        val result = repository.isFavorite("1").first()
        
        // Then
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
    }
    
    @Test
    fun `isFavorite returns false for non-favorite channel`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.isFavorite("1") } returns flowOf(false)
        
        // When
        val result = repository.isFavorite("1").first()
        
        // Then
        assertTrue(result is Result.Success)
        assertFalse((result as Result.Success).data)
    }
    
    @Test
    fun `addFavorite adds channel to local database`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.isFavorite("1") } returns flowOf(false)
        coEvery { localDataSource.addFavorite(any()) } returns Unit
        every { localDataSource.getAllFavorites() } returns flowOf(listOf(testFavoriteEntity))
        coEvery { apiService.syncFavorites(any()) } returns Response.success(Unit)
        
        // When
        val result = repository.addFavorite("1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.addFavorite(match { it.channelId == "1" }) }
    }
    
    @Test
    fun `addFavorite skips if already favorite`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.isFavorite("1") } returns flowOf(true)
        
        // When
        val result = repository.addFavorite("1")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 0) { localDataSource.addFavorite(any()) }
    }
    
    @Test
    fun `removeFavorite removes channel from local database`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.removeFavorite("1") } returns Unit
        every { localDataSource.getAllFavorites() } returns flowOf(emptyList())
        coEvery { apiService.syncFavorites(any()) } returns Response.success(Unit)
        
        // When
        val result = repository.removeFavorite("1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.removeFavorite("1") }
    }
    
    @Test
    fun `toggleFavorite adds when not favorite`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.isFavorite("1") } returns flowOf(false)
        coEvery { localDataSource.addFavorite(any()) } returns Unit
        every { localDataSource.getAllFavorites() } returns flowOf(listOf(testFavoriteEntity))
        coEvery { apiService.syncFavorites(any()) } returns Response.success(Unit)
        
        // When
        val result = repository.toggleFavorite("1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.addFavorite(any()) }
    }
    
    @Test
    fun `toggleFavorite removes when favorite`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.isFavorite("1") } returns flowOf(true)
        coEvery { localDataSource.removeFavorite("1") } returns Unit
        every { localDataSource.getAllFavorites() } returns flowOf(emptyList())
        coEvery { apiService.syncFavorites(any()) } returns Response.success(Unit)
        
        // When
        val result = repository.toggleFavorite("1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.removeFavorite("1") }
    }
    
    @Test
    fun `syncFavorites sends local favorites to server`() = runTest(testDispatcher) {
        // Given
        val favorites = listOf(testFavoriteEntity)
        every { localDataSource.getAllFavorites() } returns flowOf(favorites)
        coEvery { apiService.syncFavorites(any()) } returns Response.success(Unit)
        
        // When
        val result = repository.syncFavorites()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { 
            apiService.syncFavorites(match { 
                it.channelIds == listOf("1")
            }) 
        }
    }
    
    @Test
    fun `syncFavorites handles network errors gracefully`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getAllFavorites() } returns flowOf(listOf(testFavoriteEntity))
        coEvery { apiService.syncFavorites(any()) } returns Response.error(
            500, 
            "Server error".toResponseBody()
        )
        
        // When
        val result = repository.syncFavorites()
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception.message?.contains("Sync failed") == true)
    }
    
    @Test
    fun `background sync failures do not affect local operations`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.isFavorite("1") } returns flowOf(false)
        coEvery { localDataSource.addFavorite(any()) } returns Unit
        every { localDataSource.getAllFavorites() } returns flowOf(listOf(testFavoriteEntity))
        coEvery { apiService.syncFavorites(any()) } throws Exception("Network error")
        
        // When
        val result = repository.addFavorite("1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - local operation succeeds despite sync failure
        assertTrue(result is Result.Success)
        coVerify { localDataSource.addFavorite(any()) }
    }
}
