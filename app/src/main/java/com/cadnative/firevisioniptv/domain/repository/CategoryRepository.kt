package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category data operations.
 * 
 * This interface defines the contract for accessing and managing category data,
 * following the Repository pattern from Clean Architecture.
 */
interface CategoryRepository {
    
    /**
     * Get all categories as a Flow that emits updates when data changes.
     * 
     * @return Flow emitting Result with list of categories
     */
    fun getCategories(): Flow<Result<List<Category>>>
    
    /**
     * Get a specific category by ID.
     * 
     * @param id The category ID
     * @return Flow emitting Result with the category or error if not found
     */
    fun getCategoryById(id: String): Flow<Result<Category>>
    
    /**
     * Refresh categories from remote source.
     * 
     * @return Result indicating success or failure of the refresh operation
     */
    suspend fun refreshCategories(): Result<Unit>
}
