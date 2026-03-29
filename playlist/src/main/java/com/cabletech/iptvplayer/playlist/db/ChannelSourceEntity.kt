package com.cabletech.iptvplayer.playlist.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cabletech.iptvplayer.core.model.ChannelSource

@Entity(tableName = "channel_sources")
data class ChannelSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "last_refreshed_at") val lastRefreshedAt: Long? = null,
    @ColumnInfo(name = "channel_count") val channelCount: Int = 0,
) {
    fun toDomain(): ChannelSource = ChannelSource(
        id = id,
        name = name,
        url = url,
        lastRefreshedAt = lastRefreshedAt,
        isEnabled = isEnabled,
    )
}

fun ChannelSource.toEntity(channelCount: Int = 0) = ChannelSourceEntity(
    id = id,
    name = name,
    url = url,
    isEnabled = isEnabled,
    lastRefreshedAt = lastRefreshedAt,
    channelCount = channelCount,
)
