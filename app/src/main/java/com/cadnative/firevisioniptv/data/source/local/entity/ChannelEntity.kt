package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a channel in the local database.
 * 
 * This entity stores all channel information including metadata from M3U playlists
 * and supports efficient querying through indexed columns.
 */
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isActive"]),
        Index(value = ["name"])
    ]
)
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    val streamUrl: String,
    
    val logoUrl: String?,
    
    val categoryId: String,
    
    val language: String?,
    
    val country: String?,
    
    val groupTitle: String?,
    
    val tvgId: String?,
    
    val tvgName: String?,
    
    val isActive: Boolean = true,
    
    val lastUpdated: Long = System.currentTimeMillis()
)
