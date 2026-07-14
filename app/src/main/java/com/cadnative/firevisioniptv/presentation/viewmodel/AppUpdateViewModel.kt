package com.cadnative.firevisioniptv.presentation.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.PinnedHttpClient
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.ln
import kotlin.math.pow

/**
 * UI state for the app-root update overlay.
 *
 * [dismissed] is session-only (not persisted) — ignoring the update hides the
 * screen until the next app launch, when the check runs again.
 */
data class AppUpdateUiState(
    val updateInfo: UpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadError: String? = null,
    val dismissed: Boolean = false
)

/**
 * Drives the full-screen "Update Available" overlay shown once at launch
 * (after the splash) when a newer app version is published.
 *
 * The check + download/install mirror [SettingsViewModel]'s proven flow — server
 * API first (`/api/v1/app/version`), GitHub releases as fallback — but stay in a
 * dedicated lightweight ViewModel so the app root doesn't pay for the full
 * settings state (preference flows, QR bitmap) just to check for an update.
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var checked = false
    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "AppUpdateViewModel"
        private const val APK_FILENAME = "FireVisionIPTV.apk"
        private const val GITHUB_RELEASES_API =
            "https://api.github.com/repos/akshaynikhare/FireVisionIPTV/releases/latest"
    }

    /** Checks once per process. Safe to call repeatedly. */
    fun checkForUpdate() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                checkFromServer() ?: checkFromGitHub()
            }
            if (result != null) {
                _uiState.update { it.copy(updateInfo = result) }
            }
        }
    }

    fun dismiss() {
        _uiState.update { it.copy(dismissed = true) }
    }

    private fun checkFromServer(): UpdateInfo? {
        return try {
            val ctx = application.applicationContext
            val baseUrl = AppPreferences.getServerUrl(ctx)
            val tvCode = AppPreferences.getTvCode(ctx)
            val versionCode = getVersionCode()

            val response = PinnedHttpClient.get(
                "$baseUrl/api/v1/app/version?currentVersion=$versionCode",
                mapOf("Accept" to "application/json", "X-Session-ID" to tvCode)
            )

            response.use { resp ->
                if (resp.isSuccessful) {
                    val json = org.json.JSONObject(resp.body?.string() ?: "{}")
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
                    } else null
                } else null
            }
        } catch (_: Exception) {
            null // fall through to GitHub check
        }
    }

    private fun checkFromGitHub(): UpdateInfo? {
        return try {
            val response = PinnedHttpClient.get(
                GITHUB_RELEASES_API,
                mapOf("Accept" to "application/vnd.github+json")
            )

            response.use { resp ->
                if (!resp.isSuccessful) return null

                val json = org.json.JSONObject(resp.body?.string() ?: "{}")
                val tagName = json.optString("tag_name", "")
                val latestVersion = tagName.removePrefix("v")
                val currentVersionName = getAppVersionName()

                if (latestVersion.isNotEmpty() && latestVersion != currentVersionName &&
                    compareVersions(latestVersion, currentVersionName) > 0
                ) {
                    val assets = json.optJSONArray("assets")
                    var downloadUrl = ""
                    var fileSize = 0L
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name", "").endsWith(".apk")) {
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
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun downloadAndInstallUpdate() {
        val downloadUrl = _uiState.value.updateInfo?.downloadUrl
        if (downloadUrl.isNullOrEmpty()) return
        if (_uiState.value.isDownloading) return

        _uiState.update { it.copy(isDownloading = true, downloadError = null) }

        try {
            val ctx = application.applicationContext

            downloadReceiver?.let { receiver ->
                try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
            }

            val oldFile = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
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
                                _uiState.update { it.copy(isDownloading = false) }
                                installUpdate(context)
                            } else {
                                _uiState.update {
                                    it.copy(isDownloading = false, downloadError = "Download failed")
                                }
                            }
                        }
                    } finally {
                        cursor.close()
                    }
                }
            }

            // Must be exported: ACTION_DOWNLOAD_COMPLETE is sent by the Download
            // Provider app, not the system UID, so a NOT_EXPORTED receiver is never
            // delivered on Android 13+ and the UI hangs at "Downloading...".
            // Safe: the receiver checks the download id, queries DownloadManager for
            // the real status, and the APK signature is verified pre-install.
            ContextCompat.registerReceiver(
                ctx,
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            _uiState.update {
                it.copy(isDownloading = false, downloadError = "Failed to start download")
            }
        }
    }

    private fun installUpdate(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
            if (!file.exists()) return

            if (!verifyApkSignature(context, file)) {
                Log.e(TAG, "APK signature verification failed — refusing to install")
                file.delete()
                _uiState.update { it.copy(downloadError = "Update verification failed — signature mismatch") }
                return
            }

            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
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

    @Suppress("DEPRECATION")
    private fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            val currentSigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo?.apkContentsSigners
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName, PackageManager.GET_SIGNATURES
                ).signatures
            }

            val apkSigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES
                )?.signingInfo?.apkContentsSigners
            } else {
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath, PackageManager.GET_SIGNATURES
                )?.signatures
            }

            if (currentSigs.isNullOrEmpty() || apkSigs.isNullOrEmpty()) {
                Log.e(TAG, "Could not retrieve signatures for verification")
                return false
            }

            currentSigs[0].toByteArray().contentEquals(apkSigs[0].toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification error", e)
            false
        }
    }

    private fun getAppVersionName(): String {
        return try {
            application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    @Suppress("DEPRECATION")
    private fun getVersionCode(): Int {
        return try {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
        } catch (_: Exception) {
            1
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

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, 6)
        val pre = "KMGTPE"[exp - 1]
        return "%.1f %sB".format(bytes / 1024.0.pow(exp.toDouble()), pre)
    }

    override fun onCleared() {
        super.onCleared()
        downloadReceiver?.let { receiver ->
            try { application.applicationContext.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
        downloadReceiver = null
    }
}
