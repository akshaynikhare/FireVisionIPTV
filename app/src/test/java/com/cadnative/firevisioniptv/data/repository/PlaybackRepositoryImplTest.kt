package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.PlaybackLocalDataSource
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PlaybackRepositoryImpl.
 * 
 * Tests the playback position management, error handling,
 * and validation of the playback repository implementation.
 * 
 * Requirements: 
 * - TR-006 (Network Performance - offline-first architecture)
 * - US-004.4 (Playback position saved and restored)
 */
class PlaybackRepositoryImplTest {
    
    private lateinit var repository: PlaybackRepositoryImpl
    private lateinit var localDataSource: PlaybackLocalDataSource
    private val testDispatcher = StandardTestDispatcher()
    
    private val testPlaybackPosition = PlaybackPositionEntity(
        channelId = "channel-1",
        position = 30000L, // 30 seconds
        duration = 120000L, // 2 minutes
        lastPlayed = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        localDataSource = mockk()
        
        repository = PlaybackRepositoryImpl(
            localDataSource = localDataSource,
            dispatcher = testDispatcher
        )
    }
    
    @Test
    fun `getPlaybackPosition returns position for existing channel`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getPosition("channel-1") } returns flowOf(testPlaybackPosition)
        
        // When
        val result = repository.getPlaybackPosition("channel-1").first()
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(30000L, (result as Result.Success).data)
    }
    
    @Test
    fun `getPlaybackPosition returns null for non-existing channel`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getPosition("channel-2") } returns flowOf(null)
        
        // When
        val result = repository.getPlaybackPosition("channel-2").first()
        
        // Then
        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }
    
    @Test
    fun `getPlaybackPosition handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { localDataSource.getPosition("channel-1") } throws exception
        
        // When
        val result = repository.getPlaybackPosition("channel-1").first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `savePlaybackPosition saves valid position to database`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.savePosition(any()) } returns Unit
        
        // When
        val result = repository.savePlaybackPosition(
            channelId = "channel-1",
            position = 30000L,
            duration = 120000L
        )
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { 
            localDataSource.savePosition(match { 
                it.channelId == "channel-1" &&
                it.position == 30000L &&
                it.duration == 120000L
            }) 
        }
    }
    
    @Test
    fun `savePlaybackPosition rejects negative position`() = runTest(testDispatcher) {
        // When
        val result = repository.savePlaybackPosition(
            channelId = "channel-1",
            position = -1000L,
            duration = 120000L
        )
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is IllegalArgumentException)
        assertTrue(result.exception.message?.contains("non-negative") == true)
        coVerify(exactly = 0) { localDataSource.savePosition(any()) }
    }
    
    @Test
    fun `savePlaybackPosition rejects negative duration`() = runTest(testDispatcher) {
        // When
        val result = repository.savePlaybackPosition(
            channelId = "channel-1",
            position = 30000L,
            duration = -1000L
        )
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is IllegalArgumentException)
        assertTrue(result.exception.message?.contains("non-negative") == true)
        coVerify(exactly = 0) { localDataSource.savePosition(any()) }
    }
    
    @Test
    fun `savePlaybackPosition rejects position exceeding duration`() = runTest(testDispatcher) {
        // When
        val result = repository.savePlaybackPosition(
            channelId = "channel-1",
            position = 150000L,
            duration = 120000L
        )
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is IllegalArgumentException)
        assertTrue(result.exception.message?.contains("cannot exceed duration") == true)
        coVerify(exactly = 0) { localDataSource.savePosition(any()) }
    }
    
    @Test
    fun `savePlaybackPosition accepts position equal to duration`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.savePosition(any()) } returns Unit
        
        // When
        val result = repository.savePlaybackPosition(
            channelId = "channel-1",
            position = 120000L,
            duration = 120000L
        )
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.savePosition(any()) }
    }
    
    @Test
    fun `savePlaybackPosition handles database errors`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database write error")
        coEvery { localDataSource.savePosition(any()) } throws exception
        
        // When
        val result = repository.savePlaybackPosition(
            channelId = "channel-1",
            position = 30000L,
            duration = 120000L
        )
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `deletePlaybackPosition removes position from database`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.deletePosition("channel-1") } returns Unit
        
        // When
        val result = repository.deletePlaybackPosition("channel-1")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.deletePosition("channel-1") }
    }
    
    @Test
    fun `deletePlaybackPosition handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Delete error")
        coEvery { localDataSource.deletePosition("channel-1") } throws exception
        
        // When
        val result = repository.deletePlaybackPosition("channel-1")
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `getAllPlaybackPositions returns map of positions`() = runTest(testDispatcher) {
        // Given
        val positions = listOf(
            PlaybackPositionEntity("channel-1", 30000L, 120000L, System.currentTimeMillis()),
            PlaybackPositionEntity("channel-2", 60000L, 180000L, System.currentTimeMillis())
        )
        every { localDataSource.getAllPositions() } returns flowOf(positions)
        
        // When
        val result = repository.getAllPlaybackPositions().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.size)
        assertEquals(30000L, data["channel-1"])
        assertEquals(60000L, data["channel-2"])
    }
    
    @Test
    fun `getAllPlaybackPositions returns empty map when no positions`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getAllPositions() } returns flowOf(emptyList())
        
        // When
        val result = repository.getAllPlaybackPositions().first()
        
        // Then
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }
    
    @Test
    fun `getAllPlaybackPositions handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { localDataSource.getAllPositions() } throws exception
        
        // When
        val result = repository.getAllPlaybackPositions().first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `clearOldPositions deletes old positions keeping recent ones`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.deleteOldPositions(50) } returns Unit
        
        // When
        val result = repository.clearOldPositions(50)
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.deleteOldPositions(50) }
    }
    
    @Test
    fun `clearOldPositions uses default keep count`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.deleteOldPositions(100) } returns Unit
        
        // When
        val result = repository.clearOldPositions()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.deleteOldPositions(100) }
    }
    
    @Test
    fun `clearOldPositions rejects negative keep count`() = runTest(testDispatcher) {
        // When
        val result = repository.clearOldPositions(-10)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is IllegalArgumentException)
        assertTrue(result.exception.message?.contains("non-negative") == true)
        coVerify(exactly = 0) { localDataSource.deleteOldPositions(any()) }
    }
    
    @Test
    fun `clearOldPositions handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Delete error")
        coEvery { localDataSource.deleteOldPositions(any()) } throws exception
        
        // When
        val result = repository.clearOldPositions(50)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
