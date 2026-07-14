package com.cadnative.firevisioniptv.update

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
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.PinnedHttpClient
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.pow

/**
 * Single source of truth for the in-app update flow — version check (server API
 * first, GitHub releases fallback) and the APK download + signature-verified
 * install. Shared by the launch-time overlay ([AppUpdateViewModel]) and the
 * Settings "Check for updates" action so the logic exists in exactly one place.
 */
@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Terminal outcomes of a download+install, delivered on the main thread. */
    sealed interface DownloadState {
        data object Started : DownloadState
        data object InstallLaunched : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    /** Blocking network check — call from a background dispatcher. */
    fun check(): UpdateInfo? = checkFromServer() ?: checkFromGitHub()

    private fun checkFromServer(): UpdateInfo? {
        return try {
            val baseUrl = AppPreferences.getServerUrl(context)
            val tvCode = AppPreferences.getTvCode(context)
            val response = PinnedHttpClient.get(
                "$baseUrl/api/v1/app/version?currentVersion=${getVersionCode()}",
                mapOf("Accept" to "application/json", "X-Session-ID" to tvCode)
            )
            response.use { resp ->
                if (!resp.isSuccessful) return null
                val json = org.json.JSONObject(resp.body?.string() ?: "{}")
                if (json.optBoolean("success", false) && json.optBoolean("updateAvailable", false)) {
                    val latest = json.optJSONObject("latestVersion")
                    UpdateInfo(
                        versionName = latest?.optString("versionName", "") ?: "",
                        releaseNotes = latest?.optString("releaseNotes", "")?.takeIf { it != "null" } ?: "",
                        fileSize = formatFileSize(latest?.optLong("apkFileSize", 0) ?: 0),
                        downloadUrl = latest?.optString("downloadUrl", "") ?: "",
                        isMandatory = json.optBoolean("isMandatory", false)
                    )
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
                val latestVersion = json.optString("tag_name", "").removePrefix("v")
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
                        releaseNotes = json.optString("body", "").takeIf { it != "null" }?.take(500) ?: "",
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

    /**
     * Downloads the APK and, once complete + signature-verified, launches the
     * system installer. [onState] is invoked on the main thread with the outcome.
     */
    fun downloadAndInstall(updateInfo: UpdateInfo, onState: (DownloadState) -> Unit) {
        if (updateInfo.downloadUrl.isEmpty()) {
            onState(DownloadState.Failed("No download URL"))
            return
        }
        try {
            downloadReceiver?.let { runCatching { context.unregisterReceiver(it) } }

            val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
            if (oldFile.exists()) oldFile.delete()

            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl)).apply {
                setTitle("FireVision IPTV Update")
                setDescription("Downloading update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)
            onState(DownloadState.Started)

            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id != downloadId) return
                    val cursor = downloadManager.query(DownloadManager.Query().apply { setFilterById(downloadId) })
                    try {
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                onState(installUpdate())
                            } else {
                                onState(DownloadState.Failed("Download failed"))
                            }
                        }
                    } finally {
                        cursor.close()
                    }
                }
            }

            // Must be exported: ACTION_DOWNLOAD_COMPLETE is sent by the Download
            // Provider app, not the system UID, so a NOT_EXPORTED receiver is never
            // delivered on Android 13+ and the UI hangs at "Downloading...". Safe:
            // the receiver checks the download id, queries DownloadManager for the
            // real status, and the APK signature is verified pre-install.
            ContextCompat.registerReceiver(
                context,
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            onState(DownloadState.Failed("Failed to start download"))
        }
    }

    private fun installUpdate(): DownloadState {
        return try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
            if (!file.exists()) return DownloadState.Failed("Failed to get download file")

            if (!verifyApkSignature(file)) {
                Log.e(TAG, "APK signature verification failed — refusing to install")
                file.delete()
                return DownloadState.Failed("Update verification failed — signature mismatch")
            }

            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            DownloadState.InstallLaunched
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
            DownloadState.Failed("Failed to install update")
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyApkSignature(apkFile: File): Boolean {
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

    /** Unregister the download receiver — call from the owner's onCleared. */
    fun cleanup() {
        downloadReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        downloadReceiver = null
    }

    private fun getAppVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    @Suppress("DEPRECATION")
    private fun getVersionCode(): Int = try {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode.toInt() else pkg.versionCode
    } catch (_: Exception) {
        1
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
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

    companion object {
        private const val TAG = "AppUpdater"
        private const val APK_FILENAME = "FireVisionIPTV.apk"
        private const val GITHUB_RELEASES_API =
            "https://api.github.com/repos/akshaynikhare/FireVisionIPTV/releases/latest"
    }
}
