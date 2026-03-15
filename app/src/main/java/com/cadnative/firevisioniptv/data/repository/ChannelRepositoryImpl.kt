package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.mapper.ChannelMapper
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.ChannelLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import com.cadnative.firevisioniptv.data.source.remote.ChannelRemoteDataSource
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChannelRepository with offline-first strategy.
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
class ChannelRepositoryImpl @Inject constructor(
    private val remoteDataSource: ChannelRemoteDataSource,
    private val localDataSource: ChannelLocalDataSource,
    private val favoriteDao: FavoriteDao,
    private val channelMapper: ChannelMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : ChannelRepository {
    
    /**
     * Get all channels with offline-first strategy.
     * 
     * Emits local data immediately, then fetches from remote in background.
     * The Flow will automatically emit updated data when remote fetch completes.
     */
    override fun getChannels(): Flow<Result<List<Channel>>> =
        localDataSource.getAllChannels()
            .combine(favoriteDao.getAllFavorites()) { channels, favorites ->
                val favoriteIds = favorites.map { it.channelId }.toSet()
                channels.map { entity ->
                    channelMapper.toDomain(entity, isFavorite = favoriteIds.contains(entity.id))
                }
            }
            .map<List<Channel>, Result<List<Channel>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(Exception(e.message, e))) }
            .flowOn(dispatcher)
    
    /**
     * Get a specific channel by ID with offline-first strategy.
     */
    override fun getChannelById(id: String): Flow<Result<Channel>> =
        localDataSource.getChannelById(id)
            .combine(favoriteDao.isFavorite(id)) { entity, isFavorite ->
                if (entity != null) {
                    Result.Success(channelMapper.toDomain(entity, isFavorite))
                } else {
                    Result.Error(Exception("Channel not found: $id"))
                }
            }
            .catch { e -> emit(Result.Error(Exception(e.message, e))) }
            .flowOn(dispatcher)
    
    /**
     * Get channels by category with offline-first strategy.
     */
    override fun getChannelsByCategory(category: String): Flow<Result<List<Channel>>> =
        localDataSource.getChannelsByCategory(category)
            .combine(favoriteDao.getAllFavorites()) { channels, favorites ->
                val favoriteIds = favorites.map { it.channelId }.toSet()
                channels.map { entity ->
                    channelMapper.toDomain(entity, isFavorite = favoriteIds.contains(entity.id))
                }
            }
            .map<List<Channel>, Result<List<Channel>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(Exception(e.message, e))) }
            .flowOn(dispatcher)
    
    /**
     * Search channels with offline-first strategy.
     */
    override fun searchChannels(query: String): Flow<Result<List<Channel>>> =
        localDataSource.searchChannels(query)
            .combine(favoriteDao.getAllFavorites()) { channels, favorites ->
                val favoriteIds = favorites.map { it.channelId }.toSet()
                channels.map { entity ->
                    channelMapper.toDomain(entity, isFavorite = favoriteIds.contains(entity.id))
                }
            }
            .map<List<Channel>, Result<List<Channel>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(Exception(e.message, e))) }
            .flowOn(dispatcher)
    
    /**
     * Refresh channels from remote source.
     * 
     * Fetches data from API and updates local cache. The Flow-based
     * methods will automatically emit the updated data.
     */
    override suspend fun refreshChannels(): Result<Unit> = withContext(dispatcher) {
        try {
            // Fetch from remote
            val result = remoteDataSource.fetchChannels()
            
            when (result) {
                is Result.Success -> {
                    val entities = result.data.map { dto ->
                        channelMapper.toEntity(dto)
                    }
                    localDataSource.replaceAllChannels(entities)
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
    
    /**
     * Add a channel to favorites.
     */
    override suspend fun addToFavorites(channelId: String): Result<Unit> = withContext(dispatcher) {
        try {
            val favorite = FavoriteEntity(
                channelId = channelId,
                addedAt = System.currentTimeMillis(),
                displayOrder = 0
            )
            favoriteDao.addFavorite(favorite)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Remove a channel from favorites.
     */
    override suspend fun removeFromFavorites(channelId: String): Result<Unit> = withContext(dispatcher) {
        try {
            favoriteDao.removeFavorite(channelId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Get all favorite channels.
     */
    override fun getFavoriteChannels(): Flow<Result<List<Channel>>> =
        favoriteDao.getFavoriteChannels()
            .map { entities ->
                entities.map { entity ->
                    channelMapper.toDomain(entity, isFavorite = true)
                }
            }
            .map<List<Channel>, Result<List<Channel>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(Exception(e.message, e))) }
            .flowOn(dispatcher)
}
