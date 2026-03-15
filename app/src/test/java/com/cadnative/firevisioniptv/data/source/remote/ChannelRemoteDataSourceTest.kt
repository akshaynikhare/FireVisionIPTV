package com.cadnative.firevisioniptv.data.source.remote

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.model.dto.ChannelDto
import com.cadnative.firevisioniptv.data.model.dto.ChannelsResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit tests for ChannelRemoteDataSource.
 * 
 * Tests verify proper error handling for network operations including:
 * - Successful API responses
 * - HTTP errors (4xx, 5xx)
 * - Network errors (IOException)
 * - Null response bodies
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelRemoteDataSourceTest {
    
    private lateinit var apiService: FireVisionApiService
    private lateinit var dataSource: ChannelRemoteDataSource
    
    @Before
    fun setup() {
        apiService = mockk()
        dataSource = ChannelRemoteDataSource(apiService, Dispatchers.Unconfined)
    }
    
    @Test
    fun `fetchChannels returns success when API call succeeds`() = runTest {
        // Given
        val channelDto = ChannelDto(
            id = "1",
            name = "Test Channel",
            url = "http://test.com/stream",
            tvgLogo = "http://test.com/logo.png",
            groupTitle = "Entertainment",
            tvgLanguage = "English",
            tvgCountry = "US",
            tvgId = "test-id",
            tvgName = "Test",
            isActive = true
        )
        val response = ChannelsResponse(channels = listOf(channelDto))
        coEvery { apiService.getChannels() } returns Response.success(response)
        
        // When
        val result = dataSource.fetchChannels()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Test Channel", result.getOrNull()?.first()?.name)
    }
    
    @Test
    fun `fetchChannels returns error when response body is null`() = runTest {
        // Given
        coEvery { apiService.getChannels() } returns Response.success(null)
        
        // When
        val result = dataSource.fetchChannels()
        
        // Then
        assertTrue(result.isError)
        assertEquals("Response body is null", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `fetchChannels returns error when API returns 404`() = runTest {
        // Given
        val errorResponse: Response<ChannelsResponse> = Response.error(
            404,
            "Not found".toResponseBody()
        )
        coEvery { apiService.getChannels() } returns errorResponse
        
        // When
        val result = dataSource.fetchChannels()
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is NotFoundException)
    }
    
    @Test
    fun `fetchChannels returns error when API returns 500`() = runTest {
        // Given
        val errorResponse: Response<ChannelsResponse> = Response.error(
            500,
            "Server error".toResponseBody()
        )
        coEvery { apiService.getChannels() } returns errorResponse
        
        // When
        val result = dataSource.fetchChannels()
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is ServerException)
    }
    
    @Test
    fun `fetchChannels returns error when network exception occurs`() = runTest {
        // Given
        coEvery { apiService.getChannels() } throws IOException("Network error")
        
        // When
        val result = dataSource.fetchChannels()
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
    
    @Test
    fun `fetchChannelById returns success when API call succeeds`() = runTest {
        // Given
        val channelDto = ChannelDto(
            id = "1",
            name = "Test Channel",
            url = "http://test.com/stream",
            tvgLogo = null,
            groupTitle = null,
            tvgLanguage = null,
            tvgCountry = null,
            tvgId = null,
            tvgName = null,
            isActive = true
        )
        coEvery { apiService.getChannelById("1") } returns Response.success(channelDto)
        
        // When
        val result = dataSource.fetchChannelById("1")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Test Channel", result.getOrNull()?.name)
    }
    
    @Test
    fun `fetchChannelById returns error when channel not found`() = runTest {
        // Given
        val errorResponse: Response<ChannelDto> = Response.error(
            404,
            "Not found".toResponseBody()
        )
        coEvery { apiService.getChannelById("999") } returns errorResponse
        
        // When
        val result = dataSource.fetchChannelById("999")
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is NotFoundException)
    }
}
