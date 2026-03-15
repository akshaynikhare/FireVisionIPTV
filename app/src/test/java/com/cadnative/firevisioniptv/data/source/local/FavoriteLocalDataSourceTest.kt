package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FavoriteLocalDataSource.
 * 
 * Tests the wrapping of FavoriteDao operations and proper dispatcher usage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteLocalDataSourceTest {
    
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var favoriteLocalDataSource: FavoriteLocalDataSource
    
    @Before
    fun setup() {
        favoriteDao = mockk()
        favoriteLocalDataSource = FavoriteLocalDataSource(favoriteDao, Dispatchers.Unconfined)
    }
    
    @Test
    fun `getFavoriteChannels returns flow from dao`() = runTest {
        // Given
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Favorite Channel",
                streamUrl = "http://example.com/1",
                logoUrl = null,
                categoryId = "cat1",
                language = "en",
                country = "US",
                groupTitle = "Group 1",
                tvgId = "tvg1",
                tvgName = "TVG 1",
                isActive = true
            )
        )
        every { favoriteDao.getFavoriteChannels() } returns flowOf(channels)
        
        // When
        val result = favoriteLocalDataSource.getFavoriteChannels().first()
        
        // Then
        assertEquals(channels, result)
        coVerify { favoriteDao.getFavoriteChannels() }
    }
    
    @Test
    fun `isFavorite returns true when channel is favorite`() = runTest {
        // Given
        val channelId = "1"
        every { favoriteDao.isFavorite(channelId) } returns flowOf(true)
        
        // When
        val result = favoriteLocalDataSource.isFavorite(channelId).first()
        
        // Then
        assertTrue(result)
        coVerify { favoriteDao.isFavorite(channelId) }
    }
    
    @Test
    fun `isFavorite returns false when channel is not favorite`() = runTest {
        // Given
        val channelId = "999"
        every { favoriteDao.isFavorite(channelId) } returns flowOf(false)
        
        // When
        val result = favoriteLocalDataSource.isFavorite(channelId).first()
        
        // Then
        assertFalse(result)
        coVerify { favoriteDao.isFavorite(channelId) }
    }
    
    @Test
    fun `addFavorite calls dao`() = runTest {
        // Given
        val favorite = FavoriteEntity(channelId = "1", addedAt = System.currentTimeMillis(), displayOrder = 0)
        coEvery { favoriteDao.addFavorite(favorite) } returns Unit
        
        // When
        favoriteLocalDataSource.addFavorite(favorite)
        
        // Then
        coVerify { favoriteDao.addFavorite(favorite) }
    }
    
    @Test
    fun `removeFavorite calls dao`() = runTest {
        // Given
        val channelId = "1"
        coEvery { favoriteDao.removeFavorite(channelId) } returns Unit
        
        // When
        favoriteLocalDataSource.removeFavorite(channelId)
        
        // Then
        coVerify { favoriteDao.removeFavorite(channelId) }
    }
    
    @Test
    fun `updateFavoriteOrder calls dao`() = runTest {
        // Given
        val channelId = "1"
        val order = 5
        coEvery { favoriteDao.updateFavoriteOrder(channelId, order) } returns Unit
        
        // When
        favoriteLocalDataSource.updateFavoriteOrder(channelId, order)
        
        // Then
        coVerify { favoriteDao.updateFavoriteOrder(channelId, order) }
    }
    
    @Test
    fun `getAllFavorites returns flow from dao`() = runTest {
        // Given
        val favorites = listOf(
            FavoriteEntity(id = 1, channelId = "1", addedAt = System.currentTimeMillis(), displayOrder = 0)
        )
        every { favoriteDao.getAllFavorites() } returns flowOf(favorites)
        
        // When
        val result = favoriteLocalDataSource.getAllFavorites().first()
        
        // Then
        assertEquals(favorites, result)
        coVerify { favoriteDao.getAllFavorites() }
    }
    
    @Test
    fun `deleteAllFavorites calls dao`() = runTest {
        // Given
        coEvery { favoriteDao.deleteAllFavorites() } returns Unit
        
        // When
        favoriteLocalDataSource.deleteAllFavorites()
        
        // Then
        coVerify { favoriteDao.deleteAllFavorites() }
    }
}
