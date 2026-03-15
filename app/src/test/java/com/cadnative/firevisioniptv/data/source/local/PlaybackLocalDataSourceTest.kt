package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.entity.PlaybackPositionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PlaybackLocalDataSource.
 * 
 * Tests the local data source wrapper for playback position operations,
 * ensuring proper delegation to the DAO and dispatcher usage.
 * 
 * Requirements: TR-006 (Network Performance - offline-first architecture)
 */
class PlaybackLocalDataSourceTest {
    
    private lateinit var dataSource: PlaybackLocalDataSource
    private lateinit var dao: PlaybackPositionDao
    private val testDispatcher = StandardTestDispatcher()
    
    private val testPosition = PlaybackPositionEntity(
        channelId = "channel-1",
        position = 30000L,
        duration = 120000L,
        lastPlayed = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        dao = mockk()
        dataSource = PlaybackLocalDataSource(dao, testDispatcher)
    }
    
    @Test
    fun `getPosition delegates to DAO`() = runTest(testDispatcher) {
        // Given
        every { dao.getPosition("channel-1") } returns flowOf(testPosition)
        
        // When
        val result = dataSource.getPosition("channel-1").first()
        
        // Then
        assertEquals(testPosition, result)
    }
    
    @Test
    fun `getPosition returns null when not found`() = runTest(testDispatcher) {
        // Given
        every { dao.getPosition("channel-2") } returns flowOf(null)
        
        // When
        val result = dataSource.getPosition("channel-2").first()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getAllPositions delegates to DAO`() = runTest(testDispatcher) {
        // Given
        val positions = listOf(testPosition)
        every { dao.getAllPositions() } returns flowOf(positions)
        
        // When
        val result = dataSource.getAllPositions().first()
        
        // Then
        assertEquals(positions, result)
    }
    
    @Test
    fun `savePosition delegates to DAO on IO dispatcher`() = runTest(testDispatcher) {
        // Given
        coEvery { dao.savePosition(testPosition) } returns Unit
        
        // When
        dataSource.savePosition(testPosition)
        
        // Then
        coVerify { dao.savePosition(testPosition) }
    }
    
    @Test
    fun `deletePosition delegates to DAO on IO dispatcher`() = runTest(testDispatcher) {
        // Given
        coEvery { dao.deletePosition("channel-1") } returns Unit
        
        // When
        dataSource.deletePosition("channel-1")
        
        // Then
        coVerify { dao.deletePosition("channel-1") }
    }
    
    @Test
    fun `deleteAllPositions delegates to DAO on IO dispatcher`() = runTest(testDispatcher) {
        // Given
        coEvery { dao.deleteAllPositions() } returns Unit
        
        // When
        dataSource.deleteAllPositions()
        
        // Then
        coVerify { dao.deleteAllPositions() }
    }
    
    @Test
    fun `deleteOldPositions delegates to DAO with correct count`() = runTest(testDispatcher) {
        // Given
        coEvery { dao.deleteOldPositions(50) } returns Unit
        
        // When
        dataSource.deleteOldPositions(50)
        
        // Then
        coVerify { dao.deleteOldPositions(50) }
    }
    
    @Test
    fun `deleteOldPositions uses default count`() = runTest(testDispatcher) {
        // Given
        coEvery { dao.deleteOldPositions(100) } returns Unit
        
        // When
        dataSource.deleteOldPositions()
        
        // Then
        coVerify { dao.deleteOldPositions(100) }
    }
}
