package com.cadnative.firevisioniptv.update

import com.cadnative.firevisioniptv.BuildConfig
import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.PinnedHttpClient
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.ln
import kotlin.math.pow

class UpdateManager(activity: Activity) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val TIMEOUT = 30000
        private const val APK_FILENAME = "FireVisionIPTV.apk"
    }

    private val activityRef = WeakReference(activity)
    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    data class AppVersionInfo(
        val updateAvailable: Boolean,
        val isMandatory: Boolean,
        val versionName: String,
        val downloadUrl: String,
        val fileSize: Long,
        val releaseNotes: String
    )

    fun checkForUpdates(showNoUpdateDialog: Boolean) {
        val activity = activityRef.get() ?: return
        val currentVersionCode = getCurrentVersionCode(activity)
        val baseUrl = AppPreferences.getServerUrl(activity)
        val tvCode = AppPreferences.getTvCode(activity)

        Thread {
            try {
                val response = PinnedHttpClient.get(
                    "$baseUrl/api/v1/app/version?currentVersion=$currentVersionCode",
                    mapOf("Accept" to "application/json", "X-TV-Code" to tvCode)
                )

                response.use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string() ?: "{}")

                        if (json.optBoolean("success", false)) {
                            val latest = json.optJSONObject("latestVersion")
                            val versionInfo = AppVersionInfo(
                                updateAvailable = json.optBoolean("updateAvailable", false),
                                isMandatory = json.optBoolean("isMandatory", false),
                                versionName = latest?.optString("versionName", "") ?: "",
                                downloadUrl = latest?.optString("downloadUrl", "") ?: "",
                                fileSize = latest?.optLong("apkFileSize", 0) ?: 0,
                                releaseNotes = latest?.optString("releaseNotes", "") ?: ""
                            )

                            val uiActivity = activityRef.get() ?: return@Thread
                            uiActivity.runOnUiThread {
                                if (versionInfo.updateAvailable) {
                                    showUpdateDialog(versionInfo)
                                } else if (showNoUpdateDialog) {
                                    Toast.makeText(uiActivity, "You are using the latest version", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Update check failed", e)
                if (showNoUpdateDialog) {
                    val errActivity = activityRef.get() ?: return@Thread
                    errActivity.runOnUiThread {
                        Toast.makeText(errActivity, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun showUpdateDialog(versionInfo: AppVersionInfo) {
        val activity = activityRef.get() ?: return
        var message = "A new version ${versionInfo.versionName} is available!\n\n"

        if (versionInfo.releaseNotes.isNotEmpty()) {
            message += "${versionInfo.releaseNotes}\n\n"
        }

        message += "Size: ${formatFileSize(versionInfo.fileSize)}"

        if (versionInfo.isMandatory) {
            message += "\n\nThis update is mandatory."
        }

        AlertDialog.Builder(activity).apply {
            setTitle("Update Available")
            setMessage(message)
            setCancelable(!versionInfo.isMandatory)
            setPositiveButton("Update Now") { _, _ ->
                downloadAndInstallUpdate(versionInfo.downloadUrl)
            }
            if (!versionInfo.isMandatory) {
                setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            }
            show()
        }
    }

    private fun downloadAndInstallUpdate(downloadUrl: String) {
        val activity = activityRef.get() ?: return
        try {
            downloadReceiver?.let { receiver ->
                try {
                    activity.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering receiver", e)
                }
            }

            // Delete old APK if exists
            val oldFile = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILENAME
            )
            if (oldFile.exists()) oldFile.delete()

            // Download to app-private directory instead of public Downloads
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("FireVision IPTV Update")
                setDescription("Downloading update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            Toast.makeText(activity, "Downloading update...", Toast.LENGTH_SHORT).show()

            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                    if (id == downloadId) {
                        val query = DownloadManager.Query().apply { setFilterById(downloadId) }
                        val cursor = downloadManager.query(query)
                        try {
                            if (cursor.moveToFirst()) {
                                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                val status = cursor.getInt(columnIndex)

                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    activityRef.get()?.let { installUpdate(it) }
                                } else {
                                    activityRef.get()?.let {
                                        Toast.makeText(it, "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } finally {
                            cursor.close()
                        }
                    }
                }
            }

            ContextCompat.registerReceiver(
                activity,
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            activityRef.get()?.let {
                Toast.makeText(it, "Failed to download update", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun installUpdate(activity: Activity) {
        try {
            val file = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILENAME
            )

            if (!file.exists()) {
                Toast.makeText(activity, "Failed to get download file", Toast.LENGTH_SHORT).show()
                return
            }

            if (!verifyApkSignature(activity, file)) {
                Log.e(TAG, "APK signature verification failed — refusing to install")
                Toast.makeText(activity, "Update verification failed — signature mismatch", Toast.LENGTH_LONG).show()
                file.delete()
                return
            }

            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(installIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
            Toast.makeText(activity, "Failed to install update", Toast.LENGTH_SHORT).show()
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

    @Suppress("DEPRECATION")
    private fun getCurrentVersionCode(activity: Activity): Int {
        return try {
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version code", e)
            1
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, 6)
        val pre = "KMGTPE"[exp - 1]
        return "%.1f %sB".format(bytes / 1024.0.pow(exp.toDouble()), pre)
    }

    fun cleanup() {
        val activity = activityRef.get() ?: return
        downloadReceiver?.let { receiver ->
            try {
                activity.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        downloadReceiver = null
    }
}
