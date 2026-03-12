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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FavoriteLocalDataSource.
 * 
 * Tests the wrapper functionality around FavoriteDao to ensure
 * proper delegation and Flow-based query exposure.
 */
class FavoriteLocalDataSourceTest {
    
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var favoriteLocalDataSource: FavoriteLocalDataSource
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        favoriteDao = mockk()
        favoriteLocalDataSource = FavoriteLocalDataSource(favoriteDao, testDispatcher)
    }
    
    @Test
    fun `getFavoriteChannels returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val favoriteChannels = listOf(
            ChannelEntity(
                id = "1",
                name = "Favorite Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = "http://example.com/logo1.png",
                categoryId = "cat1",
                language = "en",
                country = "US",
                groupTitle = "Group 1",
                tvgId = "tvg1",
                tvgName = "TVG 1",
                isActive = true
            ),
            ChannelEntity(
                id = "2",
                name = "Favorite Channel 2",
                streamUrl = "http://example.com/2",
                logoUrl = "http://example.com/logo2.png",
                categoryId = "cat2",
                language = "es",
                country = "ES",
                groupTitle = "Group 2",
                tvgId = "tvg2",
                tvgName = "TVG 2",
                isActive = true
            )
        )
        every { favoriteDao.getFavoriteChannels() } returns flowOf(favoriteChannels)
        
        // When
        val result = favoriteLocalDataSource.getFavoriteChannels().first()
        
        // Then
        assertEquals(favoriteChannels, result)
    }
    
    @Test
    fun `isFavorite returns true when channel is favorite`() = runTest(testDispatcher) {
        // Given
        val channelId = "1"
        every { favoriteDao.isFavorite(channelId) } returns flowOf(true)
        
        // When
        val result = favoriteLocalDataSource.isFavorite(channelId).first()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isFavorite returns false when channel is not favorite`() = runTest(testDispatcher) {
        // Given
        val channelId = "1"
        every { favoriteDao.isFavorite(channelId) } returns flowOf(false)
        
        // When
        val result = favoriteLocalDataSource.isFavorite(channelId).first()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `getAllFavorites returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val favorites = listOf(
            FavoriteEntity(
                id = 1,
                channelId = "1",
                addedAt = System.currentTimeMillis(),
                displayOrder = 0
            ),
            FavoriteEntity(
                id = 2,
                channelId = "2",
                addedAt = System.currentTimeMillis(),
                displayOrder = 1
            )
        )
        every { favoriteDao.getAllFavorites() } returns flowOf(favorites)
        
        // When
        val result = favoriteLocalDataSource.getAllFavorites().first()
        
        // Then
        assertEquals(favorites, result)
    }
    
    @Test
    fun `addFavorite delegates to dao`() = runTest(testDispatcher) {
        // Given
        val favorite = FavoriteEntity(
            id = 1,
            channelId = "1",
            addedAt = System.currentTimeMillis(),
            displayOrder = 0
        )
        coEvery { favoriteDao.addFavorite(favorite) } returns Unit
        
        // When
        favoriteLocalDataSource.addFavorite(favorite)
        
        // Then
        coVerify { favoriteDao.addFavorite(favorite) }
    }
    
    @Test
    fun `removeFavorite delegates to dao`() = runTest(testDispatcher) {
        // Given
        val channelId = "1"
        coEvery { favoriteDao.removeFavorite(channelId) } returns Unit
        
        // When
        favoriteLocalDataSource.removeFavorite(channelId)
        
        // Then
        coVerify { favoriteDao.removeFavorite(channelId) }
    }
    
    @Test
    fun `updateFavoriteOrder delegates to dao`() = runTest(testDispatcher) {
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
    fun `deleteAllFavorites delegates to dao`() = runTest(testDispatcher) {
        // Given
        coEvery { favoriteDao.deleteAllFavorites() } returns Unit
        
        // When
        favoriteLocalDataSource.deleteAllFavorites()
        
        // Then
        coVerify { favoriteDao.deleteAllFavorites() }
    }
}
