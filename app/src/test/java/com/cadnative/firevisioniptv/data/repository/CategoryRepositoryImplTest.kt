package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.mapper.CategoryMapper
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.model.dto.CategoryDto
import com.cadnative.firevisioniptv.data.source.local.CategoryLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
import com.cadnative.firevisioniptv.data.source.remote.CategoryRemoteDataSource
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CategoryRepositoryImpl.
 * 
 * Tests the offline-first behavior, error handling, and data flow
 * of the repository implementation.
 */
class CategoryRepositoryImplTest {
    
    private lateinit var repository: CategoryRepositoryImpl
    private lateinit var remoteDataSource: CategoryRemoteDataSource
    private lateinit var localDataSource: CategoryLocalDataSource
    private lateinit var categoryMapper: CategoryMapper
    private val testDispatcher = StandardTestDispatcher()
    
    private val testCategoryEntity = CategoryEntity(
        id = "sports",
        name = "Sports",
        displayOrder = 1,
        channelCount = 10
    )
    
    private val testCategoryDto = CategoryDto(
        id = "sports",
        name = "Sports",
        displayOrder = 1,
        channelCount = 10
    )
    
    @Before
    fun setup() {
        remoteDataSource = mockk()
        localDataSource = mockk()
        categoryMapper = CategoryMapper()
        
        repository = CategoryRepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            categoryMapper = categoryMapper,
            dispatcher = testDispatcher
        )
    }
    
    @Test
    fun `getCategories returns local data immediately`() = runTest(testDispatcher) {
        // Given
        val categories = listOf(testCategoryEntity)
        every { localDataSource.getAllCategories() } returns flowOf(categories)
        
        // When
        val result = repository.getCategories().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("Sports", data[0].name)
        assertEquals(10, data[0].channelCount)
    }
    
    @Test
    fun `getCategories handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { localDataSource.getAllCategories() } returns flow { throw exception }
        
        // When
        val result = repository.getCategories().first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
    }
    
    @Test
    fun `getCategories returns empty list when no categories`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getAllCategories() } returns flowOf(emptyList())
        
        // When
        val result = repository.getCategories().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertTrue(data.isEmpty())
    }
    
    @Test
    fun `getCategoryById returns category when found`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getCategoryById("sports") } returns flowOf(testCategoryEntity)
        
        // When
        val result = repository.getCategoryById("sports").first()
        
        // Then
        assertTrue(result is Result.Success)
        val category = (result as Result.Success).data
        assertEquals("Sports", category.name)
        assertEquals("sports", category.id)
    }
    
    @Test
    fun `getCategoryById returns error when category not found`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getCategoryById("nonexistent") } returns flowOf(null)
        
        // When
        val result = repository.getCategoryById("nonexistent").first()
        
        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).exception
        assertTrue(error.message?.contains("Category not found") == true)
    }
    
    @Test
    fun `getCategoryById handles database errors`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { localDataSource.getCategoryById("sports") } returns flow { throw exception }
        
        // When
        val result = repository.getCategoryById("sports").first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
    }
    
    @Test
    fun `refreshCategories fetches from remote and updates local cache`() = runTest(testDispatcher) {
        // Given
        val remoteDtos = listOf(testCategoryDto)
        coEvery { remoteDataSource.fetchCategories() } returns Result.Success(remoteDtos)
        coEvery { localDataSource.replaceAllCategories(any()) } returns Unit
        
        // When
        val result = repository.refreshCategories()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { remoteDataSource.fetchCategories() }
        coVerify { localDataSource.replaceAllCategories(any()) }
    }
    
    @Test
    fun `refreshCategories handles remote errors`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Network error")
        coEvery { remoteDataSource.fetchCategories() } returns Result.Error(exception)
        
        // When
        val result = repository.refreshCategories()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
        coVerify(exactly = 0) { localDataSource.replaceAllCategories(any()) }
    }
    
    @Test
    fun `refreshCategories handles empty remote response`() = runTest(testDispatcher) {
        // Given
        coEvery { remoteDataSource.fetchCategories() } returns Result.Success(emptyList())
        coEvery { localDataSource.replaceAllCategories(emptyList()) } returns Unit
        
        // When
        val result = repository.refreshCategories()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.replaceAllCategories(emptyList()) }
    }
    
    @Test
    fun `refreshCategories handles database errors during update`() = runTest(testDispatcher) {
        // Given
        val remoteDtos = listOf(testCategoryDto)
        val exception = Exception("Database write error")
        coEvery { remoteDataSource.fetchCategories() } returns Result.Success(remoteDtos)
        coEvery { localDataSource.replaceAllCategories(any()) } throws exception
        
        // When
        val result = repository.refreshCategories()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception.message, (result as Result.Error).exception.message)
    }
    
    @Test
    fun `getCategories returns multiple categories in correct order`() = runTest(testDispatcher) {
        // Given
        val categories = listOf(
            CategoryEntity("sports", "Sports", 1, 10),
            CategoryEntity("news", "News", 2, 5),
            CategoryEntity("movies", "Movies", 3, 20)
        )
        every { localDataSource.getAllCategories() } returns flowOf(categories)
        
        // When
        val result = repository.getCategories().first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(3, data.size)
        assertEquals("Sports", data[0].name)
        assertEquals("News", data[1].name)
        assertEquals("Movies", data[2].name)
    }
}
