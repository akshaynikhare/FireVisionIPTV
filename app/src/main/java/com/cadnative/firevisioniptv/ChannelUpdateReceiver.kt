package com.cadnative.firevisioniptv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channelManager = ChannelManager.create(context)
                channelManager.syncChannelsToTif()
                Log.d(TAG, "Channel sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing channels", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ChannelUpdateReceiver"
    }
}
