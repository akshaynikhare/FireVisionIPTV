package com.cadnative.firevisioniptv.presentation.model

/**
 * UI model representing a channel for display in the presentation layer.
 * 
 * This model contains only the data needed for UI display, separated from
 * the domain model to allow UI-specific transformations and optimizations.
 */
data class ChannelUiModel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val category: String,
    val isFavorite: Boolean
)
