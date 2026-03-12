package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a user's favorite channel.
 * 
 * This entity maintains a many-to-one relationship with ChannelEntity
 * and supports reordering through the displayOrder field.
 */
@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["channelId"])]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val channelId: String,
    
    val addedAt: Long = System.currentTimeMillis(),
    
    val displayOrder: Int = 0
)
