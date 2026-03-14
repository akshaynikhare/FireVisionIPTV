package com.cadnative.firevisioniptv.presentation.model

import android.graphics.Bitmap

/**
 * UI state for the settings screen.
 *
 * Represents the complete state of the settings screen including
 * all user preferences, server configuration, and pairing info.
 */
data class SettingsUiState(
    val theme: String = "dark",
    val gridSize: Int = 3,
    val fontSize: Float = 1.0f,
    val animationSpeed: Float = 1.0f,
    val layoutDensity: String = "comfortable",
    val autoPlay: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Server configuration
    val serverUrl: String = "",
    val tvCode: String = "",
    val autoloadChannelName: String = "",
    val appVersion: String = "1.0.0",
    val qrCodeBitmap: Bitmap? = null,
    val isPaired: Boolean = false,
    val settingsSaved: Boolean = false
)
