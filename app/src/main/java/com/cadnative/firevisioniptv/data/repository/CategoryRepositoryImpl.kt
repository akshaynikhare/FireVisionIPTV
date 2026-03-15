package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.mapper.CategoryMapper
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.CategoryLocalDataSource
import com.cadnative.firevisioniptv.data.source.remote.CategoryRemoteDataSource
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.model.Category
import com.cadnative.firevisioniptv.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CategoryRepository with offline-first strategy.
 * 
 * This repository follows the offline-first pattern where:
 * 1. Local database is the single source of truth
 * 2. Data is emitted immediately from local cache
 * 3. Remote data is fetched in the background
 * 4. Local cache is updated on successful remote fetch
 * 5. Errors are handled gracefully without blocking UI
 * 
 * Requirements:
 * - TR-006: Network Performance (offline-first architecture, caching)
 * - US-007: Offline Support (cached data, graceful offline mode)
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: CategoryRemoteDataSource,
    private val localDataSource: CategoryLocalDataSource,
    private val categoryMapper: CategoryMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : CategoryRepository {
    
    /**
     * Get all categories with offline-first strategy.
     * 
     * Emits local data immediately, then fetches from remote in background.
     * The Flow will automatically emit updated data when remote fetch completes.
     */
    override fun getCategories(): Flow<Result<List<Category>>> =
        localDataSource.getAllCategories()
            .map { entities ->
                entities.map { entity -> categoryMapper.toDomain(entity) }
            }
            .map<List<Category>, Result<List<Category>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(Exception(e.message, e))) }
            .flowOn(dispatcher)

    /**
     * Get a specific category by ID with offline-first strategy.
     */
    override fun getCategoryById(id: String): Flow<Result<Category>> =
        localDataSource.getCategoryById(id)
            .map { entity ->
                if (entity != null) {
                    Result.Success(categoryMapper.toDomain(entity))
                } else {
                    Result.Error(Exception("Category not found: $id"))
                }
            }
            .catch { e -> emit(Result.Error(Exception(e.message, e))) }
            .flowOn(dispatcher)
    
    /**
     * Refresh categories from remote source.
     * 
     * Fetches data from API and updates local cache. The Flow-based
     * methods will automatically emit the updated data.
     */
    override suspend fun refreshCategories(): Result<Unit> = withContext(dispatcher) {
        try {
            // Fetch from remote
            val result = remoteDataSource.fetchCategories()
            
            when (result) {
                is Result.Success -> {
                    // Map DTOs to entities
                    val entities = result.data.map { dto ->
                        categoryMapper.toEntity(dto)
                    }
                    
                    // Atomically update local cache
                    localDataSource.replaceAllCategories(entities)
                    
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    Result.Error(result.exception)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
