package com.cadnative.firevisioniptv.data.source.remote

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.model.dto.CategoriesResponse
import com.cadnative.firevisioniptv.data.model.dto.CategoryDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Unit tests for CategoryRemoteDataSource.
 * 
 * Tests verify proper error handling for network operations including:
 * - Successful API responses
 * - HTTP errors (4xx, 5xx)
 * - Network errors (IOException)
 * - Null response bodies
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRemoteDataSourceTest {
    
    private lateinit var apiService: FireVisionApiService
    private lateinit var dataSource: CategoryRemoteDataSource
    
    @Before
    fun setup() {
        apiService = mockk()
        dataSource = CategoryRemoteDataSource(apiService, Dispatchers.Unconfined)
    }
    
    @Test
    fun `fetchCategories returns success when API call succeeds`() = runTest {
        // Given
        val categoryDto = CategoryDto(
            id = "1",
            name = "Entertainment",
            displayOrder = 1,
            channelCount = 10
        )
        val response = CategoriesResponse(categories = listOf(categoryDto))
        coEvery { apiService.getCategories() } returns Response.success(response)
        
        // When
        val result = dataSource.fetchCategories()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Entertainment", result.getOrNull()?.first()?.name)
    }
    
    @Test
    fun `fetchCategories returns error when response body is null`() = runTest {
        // Given
        coEvery { apiService.getCategories() } returns Response.success(null)
        
        // When
        val result = dataSource.fetchCategories()
        
        // Then
        assertTrue(result.isError)
        assertEquals("Response body is null", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `fetchCategories returns error when API returns 500`() = runTest {
        // Given
        val errorResponse: Response<CategoriesResponse> = Response.error(
            500,
            "Server error".toResponseBody()
        )
        coEvery { apiService.getCategories() } returns errorResponse
        
        // When
        val result = dataSource.fetchCategories()
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is ServerException)
    }
    
    @Test
    fun `fetchCategories returns error when network exception occurs`() = runTest {
        // Given
        coEvery { apiService.getCategories() } throws IOException("Network error")
        
        // When
        val result = dataSource.fetchCategories()
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
    
    @Test
    fun `fetchCategories returns empty list when API returns empty categories`() = runTest {
        // Given
        val response = CategoriesResponse(categories = emptyList())
        coEvery { apiService.getCategories() } returns Response.success(response)
        
        // When
        val result = dataSource.fetchCategories()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }
}
