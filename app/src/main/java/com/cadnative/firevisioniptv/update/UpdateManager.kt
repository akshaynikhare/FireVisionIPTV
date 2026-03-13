package com.cadnative.firevisioniptv.update

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.cadnative.firevisioniptv.api.ApiClient
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

class UpdateManager(private val activity: Activity) {

    companion object {
        private const val TAG = "UpdateManager"
    }

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    fun checkForUpdates(showNoUpdateDialog: Boolean) {
        val currentVersionCode = getCurrentVersionCode()

        ApiClient.checkForUpdates(activity, currentVersionCode, object : ApiClient.AppVersionCallback {
            override fun onSuccess(versionInfo: ApiClient.AppVersionInfo) {
                activity.runOnUiThread {
                    if (versionInfo.updateAvailable) {
                        showUpdateDialog(versionInfo)
                    } else if (showNoUpdateDialog) {
                        Toast.makeText(activity, "You are using the latest version", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onError(error: String) {
                activity.runOnUiThread {
                    Log.e(TAG, "Update check failed: $error")
                    if (showNoUpdateDialog) {
                        Toast.makeText(activity, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showUpdateDialog(versionInfo: ApiClient.AppVersionInfo) {
        var message = "A new version ${versionInfo.versionName} is available!\n\n"

        if (!versionInfo.releaseNotes.isNullOrEmpty()) {
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
        try {
            // Unregister previous receiver if exists
            downloadReceiver?.let { receiver ->
                try {
                    activity.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering receiver", e)
                }
            }

            // Setup download request
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("FireVision IPTV Update")
                setDescription("Downloading update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "FireVisionIPTV.apk")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            // Start download
            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            Toast.makeText(activity, "Downloading update...", Toast.LENGTH_SHORT).show()

            // Register download complete receiver
            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                    if (id == downloadId) {
                        val query = DownloadManager.Query().apply { setFilterById(downloadId) }
                        val cursor = downloadManager.query(query)

                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(columnIndex)

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                installUpdate(downloadManager, downloadId)
                            } else {
                                Toast.makeText(activity, "Download failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        cursor.close()
                    }
                }
            }

            activity.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            Toast.makeText(activity, "Failed to download update", Toast.LENGTH_SHORT).show()
        }
    }

    private fun installUpdate(downloadManager: DownloadManager, downloadId: Long) {
        try {
            val downloadUri = downloadManager.getUriForDownloadedFile(downloadId)

            if (downloadUri == null) {
                Toast.makeText(activity, "Failed to get download file", Toast.LENGTH_SHORT).show()
                return
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // For Android 7.0 and above, use FileProvider
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "FireVisionIPTV.apk"
                    )
                    val apkUri = FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.provider",
                        file
                    )
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    setDataAndType(downloadUri, "application/vnd.android.package-archive")
                }
            }

            activity.startActivity(installIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
            Toast.makeText(activity, "Failed to install update", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun getCurrentVersionCode(): Int {
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
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return "%.1f %sB".format(bytes / 1024.0.pow(exp.toDouble()), pre)
    }

    fun cleanup() {
        downloadReceiver?.let { receiver ->
            try {
                activity.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
    }
}
