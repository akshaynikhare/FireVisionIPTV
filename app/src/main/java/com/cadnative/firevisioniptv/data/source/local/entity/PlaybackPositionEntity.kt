package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a channel's playback position.
 * 
 * This entity stores the last playback position for each channel
 * to support resume functionality.
 */
@Entity(
    tableName = "playback_positions",
    indices = [Index(value = ["lastPlayed"])]
)
data class PlaybackPositionEntity(
    @PrimaryKey
    val channelId: String,
    
    val position: Long,
    
    val duration: Long,
    
    val lastPlayed: Long = System.currentTimeMillis()
)
