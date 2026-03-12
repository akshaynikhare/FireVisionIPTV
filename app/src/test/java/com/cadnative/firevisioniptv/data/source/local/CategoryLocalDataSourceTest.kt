package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.CategoryDao
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
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
 * Unit tests for CategoryLocalDataSource.
 * 
 * Tests the wrapper functionality around CategoryDao to ensure
 * proper delegation and Flow-based query exposure.
 */
class CategoryLocalDataSourceTest {
    
    private lateinit var categoryDao: CategoryDao
    private lateinit var categoryLocalDataSource: CategoryLocalDataSource
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        categoryDao = mockk()
        categoryLocalDataSource = CategoryLocalDataSource(categoryDao, testDispatcher)
    }
    
    @Test
    fun `getAllCategories returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val categories = listOf(
            CategoryEntity(
                id = "cat1",
                name = "Category 1",
                displayOrder = 0,
                channelCount = 10
            ),
            CategoryEntity(
                id = "cat2",
                name = "Category 2",
                displayOrder = 1,
                channelCount = 5
            )
        )
        every { categoryDao.getAllCategories() } returns flowOf(categories)
        
        // When
        val result = categoryLocalDataSource.getAllCategories().first()
        
        // Then
        assertEquals(categories, result)
    }
    
    @Test
    fun `getCategoryById returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val categoryId = "cat1"
        val category = CategoryEntity(
            id = categoryId,
            name = "Category 1",
            displayOrder = 0,
            channelCount = 10
        )
        every { categoryDao.getCategoryById(categoryId) } returns flowOf(category)
        
        // When
        val result = categoryLocalDataSource.getCategoryById(categoryId).first()
        
        // Then
        assertEquals(category, result)
    }
    
    @Test
    fun `getCategoryById returns null when category not found`() = runTest(testDispatcher) {
        // Given
        val categoryId = "nonexistent"
        every { categoryDao.getCategoryById(categoryId) } returns flowOf(null)
        
        // When
        val result = categoryLocalDataSource.getCategoryById(categoryId).first()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `insertCategory delegates to dao`() = runTest(testDispatcher) {
        // Given
        val category = CategoryEntity(
            id = "cat1",
            name = "Category 1",
            displayOrder = 0,
            channelCount = 10
        )
        coEvery { categoryDao.insertCategory(category) } returns Unit
        
        // When
        categoryLocalDataSource.insertCategory(category)
        
        // Then
        coVerify { categoryDao.insertCategory(category) }
    }
    
    @Test
    fun `insertCategories delegates to dao`() = runTest(testDispatcher) {
        // Given
        val categories = listOf(
            CategoryEntity(
                id = "cat1",
                name = "Category 1",
                displayOrder = 0,
                channelCount = 10
            ),
            CategoryEntity(
                id = "cat2",
                name = "Category 2",
                displayOrder = 1,
                channelCount = 5
            )
        )
        coEvery { categoryDao.insertCategories(categories) } returns Unit
        
        // When
        categoryLocalDataSource.insertCategories(categories)
        
        // Then
        coVerify { categoryDao.insertCategories(categories) }
    }
    
    @Test
    fun `updateCategory delegates to dao`() = runTest(testDispatcher) {
        // Given
        val category = CategoryEntity(
            id = "cat1",
            name = "Updated Category",
            displayOrder = 0,
            channelCount = 15
        )
        coEvery { categoryDao.updateCategory(category) } returns Unit
        
        // When
        categoryLocalDataSource.updateCategory(category)
        
        // Then
        coVerify { categoryDao.updateCategory(category) }
    }
    
    @Test
    fun `deleteCategory delegates to dao`() = runTest(testDispatcher) {
        // Given
        val category = CategoryEntity(
            id = "cat1",
            name = "Category 1",
            displayOrder = 0,
            channelCount = 10
        )
        coEvery { categoryDao.deleteCategory(category) } returns Unit
        
        // When
        categoryLocalDataSource.deleteCategory(category)
        
        // Then
        coVerify { categoryDao.deleteCategory(category) }
    }
    
    @Test
    fun `deleteAllCategories delegates to dao`() = runTest(testDispatcher) {
        // Given
        coEvery { categoryDao.deleteAllCategories() } returns Unit
        
        // When
        categoryLocalDataSource.deleteAllCategories()
        
        // Then
        coVerify { categoryDao.deleteAllCategories() }
    }
}
