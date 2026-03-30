package com.cadnative.firevisioniptv.domain.model

/**
 * Domain model representing a TV channel.
 * 
 * This is the core business entity for channels, independent of any
 * data source or UI framework. It contains only the essential information
 * needed by the business logic.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val category: String,
    val language: String?,
    val country: String?,
    val tvgId: String? = null,
    val isFavorite: Boolean = false,
    val alternateStreamUrls: List<String> = emptyList()
)
