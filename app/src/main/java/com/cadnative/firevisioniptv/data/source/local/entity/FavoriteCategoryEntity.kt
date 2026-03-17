package com.cadnative.firevisioniptv.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_categories",
    indices = [Index(value = ["categoryName"], unique = true)]
)
data class FavoriteCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryName: String,
    val addedAt: Long = System.currentTimeMillis()
)
