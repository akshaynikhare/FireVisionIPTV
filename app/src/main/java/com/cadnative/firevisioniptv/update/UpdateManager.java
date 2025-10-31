package com.cadnative.firevisioniptv.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.cadnative.firevisioniptv.api.ApiClient;

import java.io.File;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private final Activity activity;
    private long downloadId = -1;
    private BroadcastReceiver downloadReceiver;

    public UpdateManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * Check for updates and show dialog if available
     */
    public void checkForUpdates(boolean showNoUpdateDialog) {
        int currentVersionCode = getCurrentVersionCode();

        ApiClient.checkForUpdates(currentVersionCode, new ApiClient.AppVersionCallback() {
            @Override
            public void onSuccess(ApiClient.AppVersionInfo versionInfo) {
                activity.runOnUiThread(() -> {
                    if (versionInfo.updateAvailable) {
                        showUpdateDialog(versionInfo);
                    } else if (showNoUpdateDialog) {
                        Toast.makeText(activity, "You are using the latest version", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> {
                    Log.e(TAG, "Update check failed: " + error);
                    if (showNoUpdateDialog) {
                        Toast.makeText(activity, "Failed to check for updates", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Show update dialog
     */
    private void showUpdateDialog(ApiClient.AppVersionInfo versionInfo) {
        String message = "A new version " + versionInfo.versionName + " is available!\n\n";

        if (versionInfo.releaseNotes != null && !versionInfo.releaseNotes.isEmpty()) {
            message += versionInfo.releaseNotes + "\n\n";
        }

        message += "Size: " + formatFileSize(versionInfo.fileSize);

        if (versionInfo.isMandatory) {
            message += "\n\nThis update is mandatory.";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Update Available");
        builder.setMessage(message);
        builder.setCancelable(!versionInfo.isMandatory);

        builder.setPositiveButton("Update Now", (dialog, which) -> {
            downloadAndInstallUpdate(versionInfo.downloadUrl);
        });

        if (!versionInfo.isMandatory) {
            builder.setNegativeButton("Later", (dialog, which) -> {
                dialog.dismiss();
            });
        }

        builder.show();
    }

    /**
     * Download and install update
     */
    private void downloadAndInstallUpdate(String downloadUrl) {
        try {
            // Unregister previous receiver if exists
            if (downloadReceiver != null) {
                try {
                    activity.unregisterReceiver(downloadReceiver);
                } catch (Exception e) {
                    Log.e(TAG, "Error unregistering receiver", e);
                }
            }

            // Setup download request
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("FireVision IPTV Update");
            request.setDescription("Downloading update...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "FireVisionIPTV.apk");
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            // Start download
            DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadId = downloadManager.enqueue(request);

            Toast.makeText(activity, "Downloading update...", Toast.LENGTH_SHORT).show();

            // Register download complete receiver
            downloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                    if (id == downloadId) {
                        // Check download status
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        Cursor cursor = downloadManager.query(query);

                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            int status = cursor.getInt(columnIndex);

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                installUpdate(downloadManager, downloadId);
                            } else {
                                Toast.makeText(activity, "Download failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        cursor.close();
                    }
                }
            };

            activity.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } catch (Exception e) {
            Log.e(TAG, "Error downloading update", e);
            Toast.makeText(activity, "Failed to download update", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Install downloaded update
     */
    private void installUpdate(DownloadManager downloadManager, long downloadId) {
        try {
            Uri downloadUri = downloadManager.getUriForDownloadedFile(downloadId);

            if (downloadUri == null) {
                Toast.makeText(activity, "Failed to get download file", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // For Android 7.0 and above, use FileProvider
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "FireVisionIPTV.apk");

                Uri apkUri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".provider", file);

                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                installIntent.setDataAndType(downloadUri, "application/vnd.android.package-archive");
            }

            activity.startActivity(installIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error installing update", e);
            Toast.makeText(activity, "Failed to install update", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get current app version code
     */
    private int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) packageInfo.getLongVersionCode();
            } else {
                return packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get version code", e);
            return 1;
        }
    }

    /**
     * Format file size
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Cleanup receivers
     */
    public void cleanup() {
        if (downloadReceiver != null) {
            try {
                activity.unregisterReceiver(downloadReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
    }
}
