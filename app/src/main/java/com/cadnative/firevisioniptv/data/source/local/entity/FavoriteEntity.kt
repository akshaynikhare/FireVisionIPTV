package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a user's favorite channel.
 *
 * No foreign key to channels — favorites are independent of channel lifecycle.
 * Channel syncs must never delete favorites. The JOIN query in FavoriteDao
 * naturally excludes favorites whose channel no longer exists.
 */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["channelId"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val channelId: String,
    
    val addedAt: Long = System.currentTimeMillis(),
    
    val displayOrder: Int = 0
)
