package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a channel category in the local database.
 * 
 * Categories are used to organize channels and support efficient browsing.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    val displayOrder: Int = 0,
    
    val channelCount: Int = 0
)
