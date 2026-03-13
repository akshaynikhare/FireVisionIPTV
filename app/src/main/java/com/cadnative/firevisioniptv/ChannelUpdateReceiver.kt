package com.cadnative.firevisioniptv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.util.Log

class ChannelUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")
        when (action) {
            TvContract.ACTION_INITIALIZE_PROGRAMS -> {
                Log.d(TAG, "Initializing channels...")
                syncChannels(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device booted, syncing channels...")
                syncChannels(context)
            }
        }
    }

    private fun syncChannels(context: Context) {
        try {
            val channelManager = ChannelManager(context)
            channelManager.syncChannelsToTif()
            Log.d(TAG, "Channel sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing channels", e)
        }
    }

    companion object {
        private const val TAG = "ChannelUpdateReceiver"
    }
}
