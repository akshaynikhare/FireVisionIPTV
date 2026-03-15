package com.cadnative.firevisioniptv.domain.model

/**
 * Domain model representing a channel category.
 * 
 * Categories are used to organize channels into logical groups
 * (e.g., Sports, News, Entertainment).
 */
data class Category(
    val id: String,
    val name: String,
    val channelCount: Int
)
