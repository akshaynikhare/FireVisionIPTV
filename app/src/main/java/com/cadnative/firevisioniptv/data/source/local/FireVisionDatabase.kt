package com.cadnative.firevisioniptv.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cadnative.firevisioniptv.data.source.local.dao.CategoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.dao.EpgDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteCategoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.dao.SearchHistoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.StreamMetricsDao
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import com.cadnative.firevisioniptv.data.source.local.entity.EpgProgramEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteCategoryEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import com.cadnative.firevisioniptv.data.source.local.entity.PlaybackPositionEntity
import com.cadnative.firevisioniptv.data.source.local.entity.SearchHistoryEntity
import com.cadnative.firevisioniptv.data.source.local.entity.StreamMetricsEntity

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
        ChannelHealthEntity::class,
        FavoriteCategoryEntity::class,
        StreamMetricsEntity::class,
        EpgProgramEntity::class
    ],
    version = 8,
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

        /** v3→v4: Add unique index on favorites.channelId and search_history.query */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove duplicate favorites keeping the earliest entry per channelId
                db.execSQL("DELETE FROM favorites WHERE id NOT IN (SELECT MIN(id) FROM favorites GROUP BY channelId)")
                db.execSQL("DROP INDEX IF EXISTS `index_favorites_channelId`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_channelId` ON `favorites` (`channelId`)")
                // Remove duplicate search history keeping the latest entry per query
                db.execSQL("DELETE FROM search_history WHERE id NOT IN (SELECT MAX(id) FROM search_history GROUP BY `query`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
            }
        }

        /** v4→v5: Add favorite_categories table */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `favorite_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_categories_categoryName` ON `favorite_categories` (`categoryName`)")
            }
        }

        /** v5→v6: Add stream_metrics table for tracking play/health counters */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stream_metrics` (
                        `channelId` TEXT NOT NULL,
                        `playCount` INTEGER NOT NULL DEFAULT 0,
                        `aliveCount` INTEGER NOT NULL DEFAULT 0,
                        `deadCount` INTEGER NOT NULL DEFAULT 0,
                        `unresponsiveCount` INTEGER NOT NULL DEFAULT 0,
                        `lastPlayedAt` INTEGER,
                        `lastDeadAt` INTEGER,
                        `lastAliveAt` INTEGER,
                        `lastUnresponsiveAt` INTEGER,
                        PRIMARY KEY(`channelId`),
                        FOREIGN KEY(`channelId`) REFERENCES `channels`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stream_metrics_channelId` ON `stream_metrics` (`channelId`)")
            }
        }

        /** v6→v7: Remove FK CASCADE from favorites, channel_health, stream_metrics.
         *  All three tables are now independent of the channels table lifecycle.
         *  Channel sync and health scans no longer cascade-delete user data. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ── Favorites: remove FK CASCADE ──
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `favorites_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `channelId` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL DEFAULT 0,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `favorites_new` (`id`, `channelId`, `addedAt`, `displayOrder`) SELECT `id`, `channelId`, `addedAt`, `displayOrder` FROM `favorites`")
                db.execSQL("DROP TABLE `favorites`")
                db.execSQL("ALTER TABLE `favorites_new` RENAME TO `favorites`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_channelId` ON `favorites` (`channelId`)")

                // ── Channel Health: remove FK CASCADE ──
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `channel_health_new` (
                        `channelId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastCheckedAt` INTEGER NOT NULL DEFAULT 0,
                        `responseTimeMs` INTEGER,
                        `errorMessage` TEXT,
                        `thumbnailPath` TEXT,
                        PRIMARY KEY(`channelId`)
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `channel_health_new` SELECT * FROM `channel_health`")
                db.execSQL("DROP TABLE `channel_health`")
                db.execSQL("ALTER TABLE `channel_health_new` RENAME TO `channel_health`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_channel_health_channelId` ON `channel_health` (`channelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channel_health_lastCheckedAt` ON `channel_health` (`lastCheckedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channel_health_status` ON `channel_health` (`status`)")

                // ── Stream Metrics: remove FK CASCADE ──
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stream_metrics_new` (
                        `channelId` TEXT NOT NULL,
                        `playCount` INTEGER NOT NULL DEFAULT 0,
                        `aliveCount` INTEGER NOT NULL DEFAULT 0,
                        `deadCount` INTEGER NOT NULL DEFAULT 0,
                        `unresponsiveCount` INTEGER NOT NULL DEFAULT 0,
                        `lastPlayedAt` INTEGER,
                        `lastDeadAt` INTEGER,
                        `lastAliveAt` INTEGER,
                        `lastUnresponsiveAt` INTEGER,
                        PRIMARY KEY(`channelId`)
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `stream_metrics_new` SELECT * FROM `stream_metrics`")
                db.execSQL("DROP TABLE `stream_metrics`")
                db.execSQL("ALTER TABLE `stream_metrics_new` RENAME TO `stream_metrics`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stream_metrics_channelId` ON `stream_metrics` (`channelId`)")
            }
        }

        /** v7→v8: Add epg_programs table for offline-first EPG cache (last-good survives restarts). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `epg_programs` (
                        `channelEpgId` TEXT NOT NULL,
                        `startTimeMs` INTEGER NOT NULL,
                        `endTimeMs` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `icon` TEXT,
                        PRIMARY KEY(`channelEpgId`, `startTimeMs`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_programs_endTimeMs` ON `epg_programs` (`endTimeMs`)")
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

    abstract fun favoriteCategoryDao(): FavoriteCategoryDao

    abstract fun streamMetricsDao(): StreamMetricsDao

    abstract fun epgDao(): EpgDao
}
