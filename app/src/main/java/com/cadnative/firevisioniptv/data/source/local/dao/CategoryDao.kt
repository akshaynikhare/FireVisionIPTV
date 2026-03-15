package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Category operations.
 * 
 * Provides CRUD operations for category management with reactive Flow-based queries.
 */
@Dao
interface CategoryDao {
    
    /**
     * Get all categories ordered by display order.
     * 
     * @return Flow emitting list of all categories
     */
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    /**
     * Get a specific category by ID.
     * 
     * @param categoryId The unique identifier of the category
     * @return Flow emitting the category or null if not found
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: String): Flow<CategoryEntity?>
    
    /**
     * Insert or replace a single category.
     * 
     * @param category The category to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    
    /**
     * Insert or replace multiple categories.
     * 
     * @param categories List of categories to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    /**
     * Update an existing category.
     * 
     * @param category The category to update
     */
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    /**
     * Delete a category.
     * 
     * @param category The category to delete
     */
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    /**
     * Delete all categories from the database.
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    /**
     * Atomically replace all categories: delete then insert in a single transaction.
     */
    @Transaction
    suspend fun replaceAllCategories(categories: List<CategoryEntity>) {
        deleteAllCategories()
        insertCategories(categories)
    }
}
