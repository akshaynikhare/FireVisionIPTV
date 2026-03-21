package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stream_metrics",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["channelId"], unique = true)
    ]
)
data class StreamMetricsEntity(
    @PrimaryKey
    val channelId: String,
    val playCount: Int = 0,
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
    val unresponsiveCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val lastDeadAt: Long? = null,
    val lastAliveAt: Long? = null,
    val lastUnresponsiveAt: Long? = null
)
