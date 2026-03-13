package com.cadnative.firevisioniptv.di

import com.cadnative.firevisioniptv.data.repository.CategoryRepositoryImpl
import com.cadnative.firevisioniptv.data.repository.ChannelRepositoryImpl
import com.cadnative.firevisioniptv.data.repository.FavoriteRepositoryImpl
import com.cadnative.firevisioniptv.data.repository.PlaybackRepositoryImpl
import com.cadnative.firevisioniptv.data.repository.SearchHistoryRepositoryImpl
import com.cadnative.firevisioniptv.domain.repository.CategoryRepository
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import com.cadnative.firevisioniptv.domain.repository.PlaybackRepository
import com.cadnative.firevisioniptv.domain.repository.SearchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 * 
 * This module binds repository interfaces to their implementations,
 * enabling dependency injection throughout the app. Using @Binds
 * instead of @Provides is more efficient as it generates less code.
 * 
 * Requirements: TR-003 (Clean Architecture - dependency injection)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Binds ChannelRepositoryImpl to ChannelRepository interface.
     * 
     * This allows the app to depend on the interface while Hilt
     * provides the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindChannelRepository(
        impl: ChannelRepositoryImpl
    ): ChannelRepository
    
    /**
     * Binds CategoryRepositoryImpl to CategoryRepository interface.
     * 
     * This allows the app to depend on the interface while Hilt
     * provides the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository
    
    /**
     * Binds FavoriteRepositoryImpl to FavoriteRepository interface.
     * 
     * This allows the app to depend on the interface while Hilt
     * provides the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(
        impl: FavoriteRepositoryImpl
    ): FavoriteRepository
    
    /**
     * Binds SearchHistoryRepositoryImpl to SearchHistoryRepository interface.
     * 
     * This allows the app to depend on the interface while Hilt
     * provides the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        impl: SearchHistoryRepositoryImpl
    ): SearchHistoryRepository
    
    /**
     * Binds PlaybackRepositoryImpl to PlaybackRepository interface.
     * 
     * This allows the app to depend on the interface while Hilt
     * provides the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(
        impl: PlaybackRepositoryImpl
    ): PlaybackRepository
}
