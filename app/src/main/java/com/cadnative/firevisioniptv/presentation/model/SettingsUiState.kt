package com.cadnative.firevisioniptv.presentation.model

/**
 * UI state for the settings screen.
 * 
 * Represents the complete state of the settings screen including
 * all user preferences and configuration options.
 */
data class SettingsUiState(
    val theme: String = "dark",
    val gridSize: Int = 3,
    val fontSize: Float = 1.0f,
    val animationSpeed: Float = 1.0f,
    val layoutDensity: String = "comfortable",
    val isLoading: Boolean = false,
    val error: String? = null
)
