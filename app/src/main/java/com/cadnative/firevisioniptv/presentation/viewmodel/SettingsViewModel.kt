package com.cadnative.firevisioniptv.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.SettingsActivity
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.domain.service.ChannelHealthScanner
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.model.SettingsUiState
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: Application,
    private val channelHealthScanner: ChannelHealthScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val scanProgress: StateFlow<ScanProgress> = channelHealthScanner.scanProgress

    companion object {
        private const val PREFS_NAME = "FireVisionSettings"
        private const val DEFAULT_TV_CODE = "5T6FEP"
        private const val AUTOLOAD_CHANNEL_NAME_KEY = "autoload_channel_name"
    }

    init {
        loadPreferences()
        loadServerConfig()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                userPreferencesRepository.getTheme(),
                userPreferencesRepository.getGridSize(),
                userPreferencesRepository.getFontSize(),
                userPreferencesRepository.getAnimationSpeed(),
                userPreferencesRepository.getLayoutDensity()
            ) { theme, gridSize, fontSize, animationSpeed, layoutDensity ->
                SettingsUiState(
                    theme = theme,
                    gridSize = gridSize,
                    fontSize = fontSize,
                    animationSpeed = animationSpeed,
                    layoutDensity = layoutDensity,
                    isLoading = false
                )
            }.collect { state ->
                // Merge with existing server config fields
                _uiState.update { current ->
                    state.copy(
                        serverUrl = current.serverUrl,
                        tvCode = current.tvCode,
                        autoloadChannelName = current.autoloadChannelName,
                        appVersion = current.appVersion,
                        qrCodeBitmap = current.qrCodeBitmap,
                        isPaired = current.isPaired,
                        settingsSaved = current.settingsSaved
                    )
                }
            }
        }
    }

    private fun loadServerConfig() {
        val ctx = application.applicationContext
        val serverUrl = SettingsActivity.getServerUrl(ctx)
        val tvCode = SettingsActivity.getTvCode(ctx)
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoloadName = prefs.getString(AUTOLOAD_CHANNEL_NAME_KEY, "") ?: ""
        val isPaired = tvCode.isNotEmpty() && tvCode != DEFAULT_TV_CODE

        _uiState.update {
            it.copy(
                serverUrl = serverUrl,
                tvCode = tvCode,
                autoloadChannelName = autoloadName,
                isPaired = isPaired,
                appVersion = getAppVersion()
            )
        }

        generateQRCode(serverUrl)
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, settingsSaved = false) }
    }

    fun onTvCodeChange(code: String) {
        _uiState.update { it.copy(tvCode = code, settingsSaved = false) }
    }

    fun saveServerSettings(): Boolean {
        val url = _uiState.value.serverUrl.trim()
        val code = _uiState.value.tvCode.trim()

        if (url.isEmpty() || code.isEmpty()) return false
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false

        viewModelScope.launch(Dispatchers.IO) {
            val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("server_url", url)
                .putString("tv_code", code)
                .apply()
        }

        val isPaired = code.isNotEmpty() && code != DEFAULT_TV_CODE
        _uiState.update {
            it.copy(
                serverUrl = url,
                tvCode = code,
                isPaired = isPaired,
                settingsSaved = true,
                error = null
            )
        }

        generateQRCode(url)
        return true
    }

    fun clearAutoloadChannel() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("autoload_channel_id")
                .remove(AUTOLOAD_CHANNEL_NAME_KEY)
                .apply()
        }

        _uiState.update { it.copy(autoloadChannelName = "") }
    }

    private fun generateQRCode(serverUrl: String) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val registrationUrl = "$serverUrl/user/register.html"
                    val writer = QRCodeWriter()
                    val bitMatrix = writer.encode(registrationUrl, BarcodeFormat.QR_CODE, 512, 512)
                    val w = bitMatrix.width
                    val h = bitMatrix.height
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                    for (x in 0 until w) {
                        for (y in 0 until h) {
                            bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                        }
                    }
                    bmp
                } catch (e: Exception) {
                    null
                }
            }
            _uiState.update { it.copy(qrCodeBitmap = bitmap) }
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            "$versionName (Build $versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setTheme(theme)
            handleResult(result, "Failed to update theme")
        }
    }

    fun setGridSize(size: Int) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setGridSize(size)
            handleResult(result, "Failed to update grid size")
        }
    }

    fun setFontSize(scale: Float) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setFontSize(scale)
            handleResult(result, "Failed to update font size")
        }
    }

    fun setAnimationSpeed(speed: Float) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setAnimationSpeed(speed)
            handleResult(result, "Failed to update animation speed")
        }
    }

    fun setLayoutDensity(density: String) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setLayoutDensity(density)
            handleResult(result, "Failed to update layout density")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = userPreferencesRepository.clearCache()
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to clear cache"
                        )
                    }
                }
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            setTheme("dark")
            setGridSize(3)
            setFontSize(1.0f)
            setAnimationSpeed(1.0f)
            setLayoutDensity("comfortable")
        }
    }

    private fun handleResult(result: Result<Unit>, errorMessage: String) {
        when (result) {
            is Result.Success -> { }
            is Result.Error -> {
                _uiState.update {
                    it.copy(error = result.exception.message ?: errorMessage)
                }
            }
        }
    }

    fun triggerLivelinessCheck() {
        channelHealthScanner.triggerManualScan()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
