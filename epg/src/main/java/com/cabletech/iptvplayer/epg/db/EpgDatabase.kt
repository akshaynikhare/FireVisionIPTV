package com.cabletech.iptvplayer.epg.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EpgSourceEntity::class, EpgProgrammeEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class EpgDatabase : RoomDatabase() {

    abstract fun epgDao(): EpgDao

    companion object {
        private const val DB_NAME = "epg.db"

        @Volatile
        private var INSTANCE: EpgDatabase? = null

        fun getInstance(context: Context): EpgDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EpgDatabase::class.java,
                    DB_NAME,
                ).build().also { INSTANCE = it }
            }
    }
}
