package com.cabletech.iptvplayer.playlist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChannelSourceEntity::class, ChannelEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PlaylistDatabase : RoomDatabase() {

    abstract fun channelSourceDao(): ChannelSourceDao
    abstract fun channelDao(): ChannelDao

    companion object {
        private const val DB_NAME = "playlist.db"

        @Volatile
        private var INSTANCE: PlaylistDatabase? = null

        fun getInstance(context: Context): PlaylistDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): PlaylistDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PlaylistDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
