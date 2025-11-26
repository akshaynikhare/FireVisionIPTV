package com.cadnative.firevisioniptv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.util.Log;

/**
 * Broadcast Receiver for Fire TV channel updates
 * Listens for INITIALIZE_PROGRAMS action to sync channels
 */
public class ChannelUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "ChannelUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);

        if (TvContract.ACTION_INITIALIZE_PROGRAMS.equals(action)) {
            // Sync channels when requested
            Log.d(TAG, "Initializing channels...");
            syncChannels(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Re-sync channels on device boot
            Log.d(TAG, "Device booted, syncing channels...");
            syncChannels(context);
        }
    }

    private void syncChannels(Context context) {
        try {
            ChannelManager channelManager = new ChannelManager(context);
            channelManager.syncChannelsToTif();
            Log.d(TAG, "Channel sync completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error syncing channels", e);
        }
    }
}
