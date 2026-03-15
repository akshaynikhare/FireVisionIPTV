package com.cadnative.firevisioniptv.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    companion object {
        /** v1→v2: Add channel_health table */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `channel_health` (
                        `channelId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastCheckedAt` INTEGER NOT NULL DEFAULT 0,
                        `responseTimeMs` INTEGER,
                        `errorMessage` TEXT,
                        PRIMARY KEY(`channelId`),
                        FOREIGN KEY(`channelId`) REFERENCES `channels`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_channel_health_channelId` ON `channel_health` (`channelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channel_health_lastCheckedAt` ON `channel_health` (`lastCheckedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channel_health_status` ON `channel_health` (`status`)")
            }
        }

        /** v2→v3: Add thumbnailPath column to channel_health */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `channel_health` ADD COLUMN `thumbnailPath` TEXT DEFAULT NULL")
            }
        }
    }

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
