package com.cadnative.firevisioniptv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class TvSetupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pairingIntent = Intent(this, PairingActivity::class.java).apply {
            putExtra("source", "tv_setup")
        }
        startActivityForResult(pairingIntent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Device paired! Syncing channels...", Toast.LENGTH_SHORT).show()
                syncChannels()
            }
            setResult(resultCode)
            finish()
        }
    }

    private fun syncChannels() {
        val channelManager = ChannelManager(this)
        channelManager.syncChannelsToTif()
    }

    companion object {
        private const val REQUEST_CODE = 1
    }
}
