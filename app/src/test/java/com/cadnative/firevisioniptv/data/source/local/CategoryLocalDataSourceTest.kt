package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.CategoryDao
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CategoryLocalDataSource.
 * 
 * Tests the wrapping of CategoryDao operations and proper dispatcher usage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryLocalDataSourceTest {
    
    private lateinit var categoryDao: CategoryDao
    private lateinit var categoryLocalDataSource: CategoryLocalDataSource
    
    @Before
    fun setup() {
        categoryDao = mockk()
        categoryLocalDataSource = CategoryLocalDataSource(categoryDao, Dispatchers.Unconfined)
    }
    
    @Test
    fun `getAllCategories returns flow from dao`() = runTest {
        // Given
        val categories = listOf(
            CategoryEntity(id = "1", name = "Category 1", displayOrder = 0, channelCount = 10)
        )
        every { categoryDao.getAllCategories() } returns flowOf(categories)
        
        // When
        val result = categoryLocalDataSource.getAllCategories().first()
        
        // Then
        assertEquals(categories, result)
        coVerify { categoryDao.getAllCategories() }
    }
    
    @Test
    fun `getCategoryById returns flow from dao`() = runTest {
        // Given
        val categoryId = "1"
        val category = CategoryEntity(id = categoryId, name = "Category 1", displayOrder = 0, channelCount = 10)
        every { categoryDao.getCategoryById(categoryId) } returns flowOf(category)
        
        // When
        val result = categoryLocalDataSource.getCategoryById(categoryId).first()
        
        // Then
        assertEquals(category, result)
        coVerify { categoryDao.getCategoryById(categoryId) }
    }
    
    @Test
    fun `getCategoryById returns null when not found`() = runTest {
        // Given
        val categoryId = "999"
        every { categoryDao.getCategoryById(categoryId) } returns flowOf(null)
        
        // When
        val result = categoryLocalDataSource.getCategoryById(categoryId).first()
        
        // Then
        assertNull(result)
        coVerify { categoryDao.getCategoryById(categoryId) }
    }
    
    @Test
    fun `insertCategory calls dao`() = runTest {
        // Given
        val category = CategoryEntity(id = "1", name = "Category 1", displayOrder = 0, channelCount = 10)
        coEvery { categoryDao.insertCategory(category) } returns Unit
        
        // When
        categoryLocalDataSource.insertCategory(category)
        
        // Then
        coVerify { categoryDao.insertCategory(category) }
    }
    
    @Test
    fun `insertCategories calls dao`() = runTest {
        // Given
        val categories = listOf(
            CategoryEntity(id = "1", name = "Category 1", displayOrder = 0, channelCount = 10)
        )
        coEvery { categoryDao.insertCategories(categories) } returns Unit
        
        // When
        categoryLocalDataSource.insertCategories(categories)
        
        // Then
        coVerify { categoryDao.insertCategories(categories) }
    }
    
    @Test
    fun `updateCategory calls dao`() = runTest {
        // Given
        val category = CategoryEntity(id = "1", name = "Updated Category", displayOrder = 1, channelCount = 15)
        coEvery { categoryDao.updateCategory(category) } returns Unit
        
        // When
        categoryLocalDataSource.updateCategory(category)
        
        // Then
        coVerify { categoryDao.updateCategory(category) }
    }
    
    @Test
    fun `deleteCategory calls dao`() = runTest {
        // Given
        val category = CategoryEntity(id = "1", name = "Category 1", displayOrder = 0, channelCount = 10)
        coEvery { categoryDao.deleteCategory(category) } returns Unit
        
        // When
        categoryLocalDataSource.deleteCategory(category)
        
        // Then
        coVerify { categoryDao.deleteCategory(category) }
    }
    
    @Test
    fun `deleteAllCategories calls dao`() = runTest {
        // Given
        coEvery { categoryDao.deleteAllCategories() } returns Unit
        
        // When
        categoryLocalDataSource.deleteAllCategories()
        
        // Then
        coVerify { categoryDao.deleteAllCategories() }
    }
}
