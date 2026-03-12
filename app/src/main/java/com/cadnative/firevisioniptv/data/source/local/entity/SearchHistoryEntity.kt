package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a user's search history.
 * 
 * This entity stores search queries with timestamps to support
 * recent searches functionality.
 */
@Entity(
    tableName = "search_history",
    indices = [Index(value = ["timestamp"])]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val query: String,
    
    val timestamp: Long = System.currentTimeMillis()
)
