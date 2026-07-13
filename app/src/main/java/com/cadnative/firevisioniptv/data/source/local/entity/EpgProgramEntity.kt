package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Persisted EPG program. Times are stored as epoch millis (UTC).
 *
 * EPG is cached locally so the guide survives app restarts and network failures
 * (last-good fallback) instead of blanking. No foreign key to channels — EPG is
 * keyed by the channel's EPG id (tvg-id), not the internal channel id.
 */
@Entity(
    tableName = "epg_programs",
    primaryKeys = ["channelEpgId", "startTimeMs"],
    indices = [Index(value = ["endTimeMs"])]
)
data class EpgProgramEntity(
    val channelEpgId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val title: String,
    val description: String?,
    val icon: String?
)
