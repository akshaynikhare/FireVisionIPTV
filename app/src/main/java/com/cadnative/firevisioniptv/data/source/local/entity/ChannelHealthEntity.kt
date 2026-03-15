package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channel_health",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
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
