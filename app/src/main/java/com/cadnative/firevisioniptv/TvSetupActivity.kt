package com.cadnative.firevisioniptv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pairingIntent = Intent(this, PairingActivity::class.java).apply {
            putExtra("source", "tv_setup")
        }
        @Suppress("DEPRECATION")
        startActivityForResult(pairingIntent, REQUEST_CODE)
    }

    @Deprecated("Use Activity Result API")
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val channelManager = ChannelManager.create(this@TvSetupActivity)
                channelManager.syncChannelsToTif()
            } catch (e: Exception) {
                android.util.Log.e("TvSetupActivity", "Error syncing channels", e)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 1
    }
}
