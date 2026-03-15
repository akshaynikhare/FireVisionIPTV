package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RefreshChannelsUseCase.
 *
 * Tests verify that:
 * - Use case correctly delegates to repository refresh operation
 * - Success results are properly returned
 * - Error results are properly returned
 * - Repository is called exactly once per invocation
 */
class RefreshChannelsUseCaseTest {
    
    private lateinit var channelRepository: ChannelRepository
    private lateinit var refreshChannelsUseCase: RefreshChannelsUseCase
    
    @Before
    fun setup() {
        channelRepository = mockk()
        refreshChannelsUseCase = RefreshChannelsUseCase(channelRepository)
    }
    
    @Test
    fun `invoke returns success result when refresh succeeds`() = runTest {
        // Given
        coEvery { channelRepository.refreshChannels() } returns Result.Success(Unit)
        
        // When
        val result = refreshChannelsUseCase(Unit)
        
        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke returns error result when refresh fails`() = runTest {
        // Given
        val expectedException = Exception("Network error")
        coEvery { channelRepository.refreshChannels() } returns Result.Error(expectedException)
        
        // When
        val result = refreshChannelsUseCase(Unit)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(expectedException, (result as Result.Error).exception)
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke returns error result when repository throws exception`() = runTest {
        // Given
        val expectedException = RuntimeException("Unexpected error")
        coEvery { channelRepository.refreshChannels() } throws expectedException
        
        // When
        val result = try {
            refreshChannelsUseCase(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
        
        // Then
        assertTrue(result is Result.Error)
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke calls repository refresh exactly once`() = runTest {
        // Given
        coEvery { channelRepository.refreshChannels() } returns Result.Success(Unit)
        
        // When
        refreshChannelsUseCase(Unit)
        
        // Then
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `multiple invocations call repository multiple times`() = runTest {
        // Given
        coEvery { channelRepository.refreshChannels() } returns Result.Success(Unit)
        
        // When
        refreshChannelsUseCase(Unit)
        refreshChannelsUseCase(Unit)
        refreshChannelsUseCase(Unit)
        
        // Then
        coVerify(exactly = 3) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke handles network timeout error`() = runTest {
        // Given
        val timeoutException = Exception("Connection timeout")
        coEvery { channelRepository.refreshChannels() } returns Result.Error(timeoutException)
        
        // When
        val result = refreshChannelsUseCase(Unit)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("Connection timeout", (result as Result.Error).exception.message)
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke handles server error`() = runTest {
        // Given
        val serverException = Exception("Server error: 500")
        coEvery { channelRepository.refreshChannels() } returns Result.Error(serverException)
        
        // When
        val result = refreshChannelsUseCase(Unit)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("Server error: 500", (result as Result.Error).exception.message)
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke handles authentication error`() = runTest {
        // Given
        val authException = Exception("Unauthorized: 401")
        coEvery { channelRepository.refreshChannels() } returns Result.Error(authException)
        
        // When
        val result = refreshChannelsUseCase(Unit)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("Unauthorized: 401", (result as Result.Error).exception.message)
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke succeeds after previous failure`() = runTest {
        // Given
        coEvery { channelRepository.refreshChannels() } returnsMany listOf(
            Result.Error(Exception("First attempt failed")),
            Result.Success(Unit)
        )
        
        // When
        val firstResult = refreshChannelsUseCase(Unit)
        val secondResult = refreshChannelsUseCase(Unit)
        
        // Then
        assertTrue(firstResult is Result.Error)
        assertTrue(secondResult is Result.Success)
        coVerify(exactly = 2) { channelRepository.refreshChannels() }
    }
    
    @Test
    fun `invoke with Unit parameter works correctly`() = runTest {
        // Given
        coEvery { channelRepository.refreshChannels() } returns Result.Success(Unit)
        
        // When
        val result = refreshChannelsUseCase(Unit)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)
        coVerify(exactly = 1) { channelRepository.refreshChannels() }
    }
}
