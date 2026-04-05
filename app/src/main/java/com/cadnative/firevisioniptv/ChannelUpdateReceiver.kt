package com.cadnative.firevisioniptv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cadnative.firevisioniptv.worker.ChannelSyncWorker

class ChannelUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")
        when (action) {
            TvContract.ACTION_INITIALIZE_PROGRAMS -> {
                Log.d(TAG, "Initializing channels...")
                enqueueSync(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device booted, syncing channels...")
                enqueueSync(context)
            }
        }
    }

    private fun enqueueSync(context: Context) {
        val syncRequest = OneTimeWorkRequestBuilder<ChannelSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            BOOT_SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
        Log.d(TAG, "Channel sync enqueued via WorkManager")
    }

    companion object {
        private const val TAG = "ChannelUpdateReceiver"
        private const val BOOT_SYNC_WORK = "boot_channel_sync"
    }
}
