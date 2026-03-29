package com.cabletech.iptvplayer.epg.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persists a user-configured XMLTV EPG source URL. */
@Entity(tableName = "epg_sources")
data class EpgSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    @ColumnInfo(name = "last_refreshed_at") val lastRefreshedAt: Long? = null,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
)
