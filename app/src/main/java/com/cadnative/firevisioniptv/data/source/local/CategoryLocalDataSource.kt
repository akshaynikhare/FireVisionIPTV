package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.CategoryDao
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for category-related database operations.
 * 
 * This class wraps the CategoryDao and provides a clean interface for
 * accessing category data from the local Room database. All operations
 * are executed on the IO dispatcher for optimal performance.
 * 
 * Requirements: TR-006 (Offline-first architecture with local database)
 */
@Singleton
class CategoryLocalDataSource @Inject constructor(
    private val categoryDao: CategoryDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    
    /**
     * Get all categories as a Flow.
     * 
     * @return Flow emitting list of all categories ordered by display order
     */
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }
    
    /**
     * Get a specific category by ID as a Flow.
     * 
     * @param categoryId The unique identifier of the category
     * @return Flow emitting the category or null if not found
     */
    fun getCategoryById(categoryId: String): Flow<CategoryEntity?> {
        return categoryDao.getCategoryById(categoryId)
    }
    
    /**
     * Insert or replace a single category.
     * 
     * @param category The category to insert
     */
    suspend fun insertCategory(category: CategoryEntity) = withContext(dispatcher) {
        categoryDao.insertCategory(category)
    }
    
    /**
     * Insert or replace multiple categories.
     * 
     * @param categories List of categories to insert
     */
    suspend fun insertCategories(categories: List<CategoryEntity>) = withContext(dispatcher) {
        categoryDao.insertCategories(categories)
    }
    
    /**
     * Update an existing category.
     * 
     * @param category The category to update
     */
    suspend fun updateCategory(category: CategoryEntity) = withContext(dispatcher) {
        categoryDao.updateCategory(category)
    }
    
    /**
     * Delete a category.
     * 
     * @param category The category to delete
     */
    suspend fun deleteCategory(category: CategoryEntity) = withContext(dispatcher) {
        categoryDao.deleteCategory(category)
    }
    
    /**
     * Delete all categories from the database.
     */
    suspend fun deleteAllCategories() = withContext(dispatcher) {
        categoryDao.deleteAllCategories()
    }
}
