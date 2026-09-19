package com.cadnative.firevisioniptv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvSetupActivity : ComponentActivity() {

    private val pairingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Device paired! Syncing channels...", Toast.LENGTH_SHORT).show()
            syncChannels()
        }
        setResult(result.resultCode)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pairingIntent = Intent(this, PairingActivity::class.java).apply {
            putExtra("source", "tv_setup")
        }
        pairingLauncher.launch(pairingIntent)
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
}
