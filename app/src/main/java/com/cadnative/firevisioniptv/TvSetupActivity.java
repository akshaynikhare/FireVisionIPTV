package com.cadnative.firevisioniptv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Setup Activity for Fire TV TIF Integration
 * This launches when users navigate to Settings > Live TV > Sync Sources
 * It can redirect to your existing PairingActivity or handle setup directly
 */
public class TvSetupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Option 1: Redirect to existing PairingActivity
        Intent pairingIntent = new Intent(this, PairingActivity.class);
        pairingIntent.putExtra("source", "tv_setup");
        startActivityForResult(pairingIntent, 1);

        // Option 2: Show simple message and sync channels
        // Toast.makeText(this, "Syncing FireVision IPTV channels...", Toast.LENGTH_SHORT).show();
        // syncChannels();
        // setResult(RESULT_OK);
        // finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // Pairing successful, sync channels
                Toast.makeText(this, "Device paired! Syncing channels...", Toast.LENGTH_SHORT).show();
                syncChannels();
            }
            setResult(resultCode);
            finish();
        }
    }

    private void syncChannels() {
        // Trigger channel sync via ChannelManager
        ChannelManager channelManager = new ChannelManager(this);
        channelManager.syncChannelsToTif();
    }
}
