package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * No foreign key to channels — health data persists independently.
 * Old statuses remain visible until replaced by a new scan result.
 */
@Entity(
    tableName = "channel_health",
    indices = [
        Index(value = ["channelId"], unique = true),
        Index(value = ["lastCheckedAt"]),
        Index(value = ["status"])
    ]
)
data class ChannelHealthEntity(
    @PrimaryKey
    val channelId: String,
    val status: String,
    val lastCheckedAt: Long = 0L,
    val responseTimeMs: Long? = null,
    val errorMessage: String? = null,
    val thumbnailPath: String? = null
)
