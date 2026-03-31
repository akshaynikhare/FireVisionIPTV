package com.cadnative.firevisioniptv.presentation.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.domain.service.ChannelHealthScanner
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.model.SettingsUiState
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.pow
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

    private var updateCheckJob: Job? = null

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val PREFS_NAME = AppPreferences.PREFS_NAME
        private const val DEFAULT_TV_CODE = AppPreferences.DEFAULT_TV_CODE
        private const val UPDATE_CHECK_TIMEOUT = 15_000
        private const val APK_FILENAME = "FireVisionIPTV.apk"
        private const val GITHUB_RELEASES_API = "https://api.github.com/repos/akshaynikhare/FireVisionIPTV/releases/latest"
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
                        settingsSaved = current.settingsSaved,
                        isCheckingForUpdate = current.isCheckingForUpdate,
                        updateInfo = current.updateInfo,
                        updateChecked = current.updateChecked
                    )
                }
            }
        }
    }

    private fun loadServerConfig() {
        val ctx = application.applicationContext
        val serverUrl = AppPreferences.getServerUrl(ctx)
        val tvCode = AppPreferences.getTvCode(ctx)
        val isPaired = tvCode.isNotEmpty() && tvCode != DEFAULT_TV_CODE

        _uiState.update {
            it.copy(
                serverUrl = serverUrl,
                tvCode = tvCode,
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
        if (!android.util.Patterns.WEB_URL.matcher(url).matches()) return false

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

    fun checkForUpdate() {
        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdate = true, updateInfo = null, updateChecked = false) }

            val result = withContext(Dispatchers.IO) {
                // Try server API first, fall back to GitHub releases
                checkFromServer() ?: checkFromGitHub()
            }

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

    private fun checkFromServer(): UpdateInfo? {
        return try {
            val ctx = application.applicationContext
            val baseUrl = AppPreferences.getServerUrl(ctx)
            val tvCode = AppPreferences.getTvCode(ctx)
            val versionCode = getVersionCode()

            val url = java.net.URL("$baseUrl/api/v1/app/version?currentVersion=$versionCode")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = UPDATE_CHECK_TIMEOUT
            connection.readTimeout = UPDATE_CHECK_TIMEOUT
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Session-ID", tvCode)

            try {
                if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)

                    if (json.optBoolean("success", false) && json.optBoolean("updateAvailable", false)) {
                        val latest = json.optJSONObject("latestVersion")
                        val fileBytes = latest?.optLong("apkFileSize", 0) ?: 0
                        UpdateInfo(
                            versionName = latest?.optString("versionName", "") ?: "",
                            releaseNotes = latest?.optString("releaseNotes", "")
                                ?.takeIf { it != "null" } ?: "",
                            fileSize = formatFileSize(fileBytes),
                            downloadUrl = latest?.optString("downloadUrl", "") ?: "",
                            isMandatory = json.optBoolean("isMandatory", false)
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null // fall through to GitHub check
        }
    }

    private fun checkFromGitHub(): UpdateInfo? {
        return try {
            val url = java.net.URL(GITHUB_RELEASES_API)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = UPDATE_CHECK_TIMEOUT
            connection.readTimeout = UPDATE_CHECK_TIMEOUT
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")

            try {
                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) return null

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)

                val tagName = json.optString("tag_name", "")
                val latestVersion = tagName.removePrefix("v")
                val currentVersionName = getAppVersionName()

                if (latestVersion.isNotEmpty() && latestVersion != currentVersionName &&
                    compareVersions(latestVersion, currentVersionName) > 0) {

                    // Find APK asset
                    val assets = json.optJSONArray("assets")
                    var downloadUrl = ""
                    var fileSize = 0L
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                fileSize = asset.optLong("size", 0)
                                break
                            }
                        }
                    }

                    UpdateInfo(
                        versionName = latestVersion,
                        releaseNotes = json.optString("body", "")
                            .takeIf { it != "null" }?.take(500) ?: "",
                        fileSize = formatFileSize(fileSize),
                        downloadUrl = downloadUrl,
                        isMandatory = false
                    )
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getAppVersionName(): String {
        return try {
            application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    private fun getVersionCode(): Int {
        return try {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, 6)
        val pre = "KMGTPE"[exp - 1]
        return "%.1f %sB".format(bytes / 1024.0.pow(exp.toDouble()), pre)
    }

    fun downloadAndInstallUpdate() {
        val downloadUrl = _uiState.value.updateInfo?.downloadUrl
        if (downloadUrl.isNullOrEmpty()) return
        if (_uiState.value.isDownloadingUpdate) return

        _uiState.update { it.copy(isDownloadingUpdate = true, downloadError = null) }

        try {
            val ctx = application.applicationContext

            // Cleanup previous receiver
            downloadReceiver?.let { receiver ->
                try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
            }

            // Delete old APK
            val oldFile = java.io.File(
                ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILENAME
            )
            if (oldFile.exists()) oldFile.delete()

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("FireVision IPTV Update")
                setDescription("Downloading update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id != downloadId) return

                    val query = DownloadManager.Query().apply { setFilterById(downloadId) }
                    val cursor = downloadManager.query(query)
                    try {
                        if (cursor.moveToFirst()) {
                            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(statusIdx)
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                _uiState.update { it.copy(isDownloadingUpdate = false) }
                                installUpdate(context)
                            } else {
                                _uiState.update {
                                    it.copy(isDownloadingUpdate = false, downloadError = "Download failed")
                                }
                            }
                        }
                    } finally {
                        cursor.close()
                    }
                }
            }

            ContextCompat.registerReceiver(
                ctx,
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            _uiState.update {
                it.copy(isDownloadingUpdate = false, downloadError = "Failed to start download")
            }
        }
    }

    private fun installUpdate(context: Context) {
        try {
            val file = java.io.File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILENAME
            )
            if (!file.exists()) return

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
            _uiState.update { it.copy(downloadError = "Failed to install update") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadReceiver?.let { receiver ->
            try { application.applicationContext.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
        downloadReceiver = null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, downloadError = null) }
    }
}
