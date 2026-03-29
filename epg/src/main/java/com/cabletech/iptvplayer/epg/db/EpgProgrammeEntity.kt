package com.cabletech.iptvplayer.epg.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cabletech.iptvplayer.epg.EpgProgramme

@Entity(
    tableName = "epg_programmes",
    indices = [
        Index(value = ["channel_id", "start_utc_ms"]),
    ],
)
data class EpgProgrammeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "channel_id") val channelId: String,
    val title: String,
    @ColumnInfo(name = "start_utc_ms") val startUtcMs: Long,
    @ColumnInfo(name = "stop_utc_ms") val stopUtcMs: Long,
    val description: String? = null,
) {
    fun toDomain() = EpgProgramme(
        channelId = channelId,
        title = title,
        startUtcMs = startUtcMs,
        stopUtcMs = stopUtcMs,
        description = description,
    )
}

fun EpgProgramme.toEntity() = EpgProgrammeEntity(
    channelId = channelId,
    title = title,
    startUtcMs = startUtcMs,
    stopUtcMs = stopUtcMs,
    description = description,
)
