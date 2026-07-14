package com.cadnative.firevisioniptv.presentation.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.PinnedHttpClient
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.domain.service.ChannelHealthScanner
import com.cadnative.firevisioniptv.domain.usecase.RefreshChannelsUseCase
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.model.SettingsUiState
import com.cadnative.firevisioniptv.update.AppUpdater
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: Application,
    private val channelHealthScanner: ChannelHealthScanner,
    private val refreshChannelsUseCase: RefreshChannelsUseCase,
    private val appUpdater: AppUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val scanProgress: StateFlow<ScanProgress> = channelHealthScanner.scanProgress

    private var updateCheckJob: Job? = null

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val PREFS_NAME = AppPreferences.PREFS_NAME
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
                        appVersion = current.appVersion,
                        qrCodeBitmap = current.qrCodeBitmap,
                        isPaired = current.isPaired,
                        isDefaultMode = current.isDefaultMode,
                        sourceType = current.sourceType,
                        m3uUrl = current.m3uUrl,
                        xtreamHost = current.xtreamHost,
                        settingsSaved = current.settingsSaved,
                        isCheckingForUpdate = current.isCheckingForUpdate,
                        updateInfo = current.updateInfo,
                        updateChecked = current.updateChecked,
                        isDownloadingUpdate = current.isDownloadingUpdate,
                        downloadError = current.downloadError,
                        isClearingCache = current.isClearingCache,
                        cacheCleared = current.cacheCleared,
                        isTestingConnection = current.isTestingConnection,
                        connectionTestResult = current.connectionTestResult,
                        isLoadingPlaylist = current.isLoadingPlaylist,
                        playlistResult = current.playlistResult,
                        backExitProtection = current.backExitProtection,
                        keyUpDownAction = current.keyUpDownAction,
                        keyLeftRightAction = current.keyLeftRightAction,
                        longOkAction = current.longOkAction,
                        sleepTimerDefaultMinutes = current.sleepTimerDefaultMinutes,
                        alwaysShowProgramBar = current.alwaysShowProgramBar,
                        infoBarTimeoutSeconds = current.infoBarTimeoutSeconds
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                userPreferencesRepository.getBackExitProtection(),
                userPreferencesRepository.getPlayerKeyUpDownAction(),
                userPreferencesRepository.getPlayerKeyLeftRightAction(),
                userPreferencesRepository.getPlayerLongOkAction(),
                userPreferencesRepository.getSleepTimerDefaultMinutes()
            ) { backProtection, upDown, leftRight, longOk, sleepTimer ->
                _uiState.update {
                    it.copy(
                        backExitProtection = backProtection,
                        keyUpDownAction = upDown,
                        keyLeftRightAction = leftRight,
                        longOkAction = longOk,
                        sleepTimerDefaultMinutes = sleepTimer
                    )
                }
            }.collect { }
        }
        viewModelScope.launch {
            userPreferencesRepository.getAlwaysShowProgramBar().collect { enabled ->
                _uiState.update { it.copy(alwaysShowProgramBar = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.getInfoBarTimeoutSeconds().collect { seconds ->
                _uiState.update { it.copy(infoBarTimeoutSeconds = seconds) }
            }
        }
    }

    private fun loadServerConfig() {
        val ctx = application.applicationContext
        val serverUrl = AppPreferences.getServerUrl(ctx)
        val tvCode = AppPreferences.getTvCode(ctx)
        val isPaired = tvCode.isNotEmpty() && !AppPreferences.isDemoMode(ctx)
        val isDefaultMode = AppPreferences.isDemoMode(ctx)

        _uiState.update {
            it.copy(
                serverUrl = serverUrl,
                tvCode = tvCode,
                isPaired = isPaired,
                isDefaultMode = isDefaultMode,
                sourceType = AppPreferences.getPlaylistSourceType(ctx),
                m3uUrl = AppPreferences.getM3uUrl(ctx),
                xtreamHost = AppPreferences.getXtreamHost(ctx),
                appVersion = getAppVersion()
            )
        }

        generateQRCode(serverUrl)
    }

    /**
     * Re-reads the active channel-source config from prefs. Called when Settings
     * resumes (e.g. returning from the AddSource screen) so a source change made
     * there — self-hosted connect, M3U/Xtream load — is reflected immediately.
     */
    fun refreshSource() {
        val ctx = application.applicationContext
        val tvCode = AppPreferences.getTvCode(ctx)
        _uiState.update {
            it.copy(
                serverUrl = AppPreferences.getServerUrl(ctx),
                tvCode = tvCode,
                isPaired = tvCode.isNotEmpty() && !AppPreferences.isDemoMode(ctx),
                isDefaultMode = AppPreferences.isDemoMode(ctx),
                sourceType = AppPreferences.getPlaylistSourceType(ctx),
                m3uUrl = AppPreferences.getM3uUrl(ctx),
                xtreamHost = AppPreferences.getXtreamHost(ctx)
            )
        }
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
        if (!url.startsWith("https://")) return false
        if (!android.util.Patterns.WEB_URL.matcher(url).matches()) return false

        // Route through AppPreferences so the playlist source type flips back to
        // "paired" (and the demo flag clears) — otherwise a prior M3U/Xtream source
        // stays active and Connection keeps showing the old source.
        val ctx = application.applicationContext
        AppPreferences.setServerUrl(ctx, url)
        AppPreferences.setTvCode(ctx, code)

        _uiState.update {
            it.copy(
                serverUrl = url,
                tvCode = code,
                isPaired = true,
                isDefaultMode = false,
                sourceType = AppPreferences.getPlaylistSourceType(ctx),
                settingsSaved = true,
                error = null
            )
        }

        generateQRCode(url)
        return true
    }

    fun resetPairing() {
        AppPreferences.clearPairing(application.applicationContext)
        _uiState.update { it.copy(tvCode = "", isPaired = false) }
    }

    private fun generateQRCode(serverUrl: String) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val registrationUrl = "$serverUrl/pair"
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
            _uiState.update { old ->
                old.qrCodeBitmap?.recycle()
                old.copy(qrCodeBitmap = bitmap)
            }
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

    fun setBackExitProtection(enabled: Boolean) {
        viewModelScope.launch {
            handleResult(userPreferencesRepository.setBackExitProtection(enabled), "Failed to update back protection")
        }
    }

    /** Save an M3U playlist URL as the channel source and load it immediately. */
    fun saveM3uPlaylist(url: String) {
        if (url.isBlank()) return
        AppPreferences.setM3uSource(application, url)
        loadPlaylist()
    }

    /** Save Xtream Codes credentials as the channel source and load them immediately. */
    fun saveXtreamPlaylist(host: String, username: String, password: String) {
        if (host.isBlank() || username.isBlank()) return
        AppPreferences.setXtreamSource(application, host, username, password)
        loadPlaylist()
    }

    /** Switch back to the managed (paired) source. */
    fun useManagedSource() {
        AppPreferences.useManagedSource(application)
        _uiState.update { it.copy(playlistResult = "Using managed source") }
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaylist = true, playlistResult = null) }
            val result = refreshChannelsUseCase(Unit)
            _uiState.update {
                it.copy(
                    isLoadingPlaylist = false,
                    playlistResult = when (result) {
                        is Result.Success -> "Playlist loaded"
                        is Result.Error -> "Failed to load playlist"
                    }
                )
            }
        }
    }

    fun setAlwaysShowProgramBar(enabled: Boolean) {
        viewModelScope.launch {
            handleResult(userPreferencesRepository.setAlwaysShowProgramBar(enabled), "Failed to update program bar")
        }
    }

    fun setInfoBarTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            handleResult(userPreferencesRepository.setInfoBarTimeoutSeconds(seconds), "Failed to update banner timeout")
        }
    }

    fun setKeyUpDownAction(action: String) {
        viewModelScope.launch {
            handleResult(userPreferencesRepository.setPlayerKeyUpDownAction(action), "Failed to update key action")
        }
    }

    fun setKeyLeftRightAction(action: String) {
        viewModelScope.launch {
            handleResult(userPreferencesRepository.setPlayerKeyLeftRightAction(action), "Failed to update key action")
        }
    }

    fun setLongOkAction(action: String) {
        viewModelScope.launch {
            handleResult(userPreferencesRepository.setPlayerLongOkAction(action), "Failed to update key action")
        }
    }

    fun setSleepTimerDefaultMinutes(minutes: Int) {
        viewModelScope.launch {
            handleResult(userPreferencesRepository.setSleepTimerDefaultMinutes(minutes), "Failed to update sleep timer")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true, cacheCleared = false) }
            val result = userPreferencesRepository.clearCache()
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isClearingCache = false, cacheCleared = true) }
                    refreshChannelsUseCase(Unit)
                    // Reset success message after 3 seconds
                    delay(3_000)
                    _uiState.update { it.copy(cacheCleared = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isClearingCache = false,
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

    fun checkForUpdate() {
        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdate = true, updateInfo = null, updateChecked = false) }

            val result = withContext(Dispatchers.IO) { appUpdater.check() }

            _uiState.update {
                it.copy(
                    isCheckingForUpdate = false,
                    updateInfo = result,
                    updateChecked = true
                )
            }
        }

        updateCheckJob?.invokeOnCompletion { error ->
            if (error != null && error !is kotlinx.coroutines.CancellationException) {
                _uiState.update {
                    it.copy(
                        isCheckingForUpdate = false,
                        updateChecked = true,
                        error = "Failed to check for updates"
                    )
                }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val update = _uiState.value.updateInfo ?: return
        if (_uiState.value.isDownloadingUpdate) return

        _uiState.update { it.copy(isDownloadingUpdate = true, downloadError = null) }
        appUpdater.downloadAndInstall(update) { state ->
            _uiState.update {
                when (state) {
                    AppUpdater.DownloadState.Started -> it.copy(isDownloadingUpdate = true)
                    AppUpdater.DownloadState.InstallLaunched -> it.copy(isDownloadingUpdate = false)
                    is AppUpdater.DownloadState.Failed ->
                        it.copy(isDownloadingUpdate = false, downloadError = state.message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appUpdater.cleanup()
        _uiState.value.qrCodeBitmap?.recycle()
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
            val result = withContext(Dispatchers.IO) {
                try {
                    val serverUrl = _uiState.value.serverUrl.trim().trimEnd('/')
                    val response = PinnedHttpClient.get("$serverUrl/health")
                    response.use { resp ->
                        if (resp.code in 200..299) "Connected" else "Server returned ${resp.code}"
                    }
                } catch (e: java.net.ConnectException) {
                    "Connection refused — check server URL"
                } catch (e: java.net.UnknownHostException) {
                    "Server not found — check URL"
                } catch (e: java.net.SocketTimeoutException) {
                    "Connection timed out"
                } catch (e: Exception) {
                    "Failed: ${e.message}"
                }
            }
            _uiState.update { it.copy(isTestingConnection = false, connectionTestResult = result) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, downloadError = null) }
    }
}
