package com.cadnative.firevisioniptv.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cadnative.firevisioniptv.data.source.local.dao.CategoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.dao.SearchHistoryDao
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import com.cadnative.firevisioniptv.data.source.local.entity.PlaybackPositionEntity
import com.cadnative.firevisioniptv.data.source.local.entity.SearchHistoryEntity

/**
 * Room database for FireVision IPTV application.
 * 
 * This database serves as the single source of truth for all local data,
 * implementing an offline-first architecture pattern.
 * 
 * Database version: 1
 * 
 * Entities:
 * - ChannelEntity: Stores channel information
 * - CategoryEntity: Stores channel categories
 * - FavoriteEntity: Stores user's favorite channels
 * - SearchHistoryEntity: Stores search history
 * - PlaybackPositionEntity: Stores playback positions for resume functionality
 * 
 * Requirements: TR-007 (Database Performance)
 */
@Database(
    entities = [
        ChannelEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        SearchHistoryEntity::class,
        PlaybackPositionEntity::class,
        ChannelHealthEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class FireVisionDatabase : RoomDatabase() {
    
    /**
     * Provides access to channel data operations.
     */
    abstract fun channelDao(): ChannelDao
    
    /**
     * Provides access to category data operations.
     */
    abstract fun categoryDao(): CategoryDao
    
    /**
     * Provides access to favorite data operations.
     */
    abstract fun favoriteDao(): FavoriteDao
    
    /**
     * Provides access to search history data operations.
     */
    abstract fun searchHistoryDao(): SearchHistoryDao
    
    /**
     * Provides access to playback position data operations.
     */
    abstract fun playbackPositionDao(): PlaybackPositionDao

    /**
     * Provides access to channel health/liveliness data operations.
     */
    abstract fun channelHealthDao(): ChannelHealthDao
}
