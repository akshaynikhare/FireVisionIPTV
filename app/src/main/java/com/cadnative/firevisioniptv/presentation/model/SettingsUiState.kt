package com.cadnative.firevisioniptv.presentation.model

import android.graphics.Bitmap
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction

/**
 * UI state for the settings screen.
 *
 * Represents the complete state of the settings screen including
 * all user preferences, server configuration, and pairing info.
 */
data class SettingsUiState(
    val theme: String = "system",
    val gridSize: Int = 3,
    val fontSize: Float = 1.0f,
    val animationSpeed: Float = 1.0f,
    val layoutDensity: String = "comfortable",
    val autoPlay: Boolean = true,
    // Player controls
    val backExitProtection: Boolean = true,
    val keyUpDownAction: String = PlayerKeyAction.ZAP,
    val keyLeftRightAction: String = PlayerKeyAction.ZAP,
    val longOkAction: String = PlayerKeyAction.FAVORITE,
    val sleepTimerDefaultMinutes: Int = 0,
    val alwaysShowProgramBar: Boolean = false,
    val infoBarTimeoutSeconds: Int = 4,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Server configuration
    val serverUrl: String = "",
    val tvCode: String = "",
    val appVersion: String = "1.0.0",
    val qrCodeBitmap: Bitmap? = null,
    val isPaired: Boolean = false,
    val isDefaultMode: Boolean = false,
    val settingsSaved: Boolean = false,
    // App update
    val isCheckingForUpdate: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val updateChecked: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val downloadError: String? = null,
    // Cache
    val isClearingCache: Boolean = false,
    val cacheCleared: Boolean = false,
    // Connection test
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    // Bring-your-own playlist (M3U / Xtream)
    val isLoadingPlaylist: Boolean = false,
    val playlistResult: String? = null
)

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val fileSize: String,
    val downloadUrl: String,
    val isMandatory: Boolean
)
