package com.cadnative.firevisioniptv.di

import android.content.Context
import androidx.room.Room
import com.cadnative.firevisioniptv.data.source.local.FireVisionDatabase
import com.cadnative.firevisioniptv.data.source.local.dao.CategoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.dao.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database-related dependencies.
 * Installed in SingletonComponent for app-wide availability.
 * 
 * This module provides:
 * - FireVisionDatabase singleton instance
 * - All DAO instances (ChannelDao, CategoryDao, FavoriteDao, SearchHistoryDao, PlaybackPositionDao)
 * 
 * Requirements: TR-003 (Architecture Modernization), TR-007 (Database Performance)
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance.
     * 
     * Configured with:
     * - fallbackToDestructiveMigration: Allows database recreation on schema changes during development
     * 
     * @param context Application context
     * @return Singleton instance of FireVisionDatabase
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FireVisionDatabase {
        return Room.databaseBuilder(
            context,
            FireVisionDatabase::class.java,
            "firevision_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides ChannelDao for channel data operations.
     * 
     * @param database FireVisionDatabase instance
     * @return ChannelDao instance
     */
    @Provides
    fun provideChannelDao(database: FireVisionDatabase): ChannelDao {
        return database.channelDao()
    }

    /**
     * Provides CategoryDao for category data operations.
     * 
     * @param database FireVisionDatabase instance
     * @return CategoryDao instance
     */
    @Provides
    fun provideCategoryDao(database: FireVisionDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * Provides FavoriteDao for favorite data operations.
     * 
     * @param database FireVisionDatabase instance
     * @return FavoriteDao instance
     */
    @Provides
    fun provideFavoriteDao(database: FireVisionDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    /**
     * Provides SearchHistoryDao for search history data operations.
     * 
     * @param database FireVisionDatabase instance
     * @return SearchHistoryDao instance
     */
    @Provides
    fun provideSearchHistoryDao(database: FireVisionDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    /**
     * Provides PlaybackPositionDao for playback position data operations.
     * 
     * @param database FireVisionDatabase instance
     * @return PlaybackPositionDao instance
     */
    @Provides
    fun providePlaybackPositionDao(database: FireVisionDatabase): PlaybackPositionDao {
        return database.playbackPositionDao()
    }
}
