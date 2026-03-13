package com.cadnative.firevisioniptv

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.cadnative.firevisioniptv.presentation.navigation.FireVisionNavGraph
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the modernized FireVision IPTV app.
 * Hosts the Compose navigation graph and handles first-launch pairing.
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ComposeMainActivity"
        private const val PREFS_NAME = "FireVisionSettings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Check if this is the first launch and TV code is not configured
        if (isFirstLaunch() && !isTvCodeConfigured()) {
            markFirstLaunchComplete()
            startActivity(Intent(this, PairingActivity::class.java))
            finish()
            return
        }

        // Parse deep link channel ID if present
        val deepLinkChannelId = intent?.data?.let { uri ->
            if (uri.host == "play" && uri.pathSegments.size >= 2 && uri.pathSegments[0] == "movie") {
                uri.pathSegments[1]
            } else null
        }

        // Check for autoload channel
        val autoloadChannelId = SettingsActivity.getAutoloadChannelId(this)
            .takeIf { it.isNotEmpty() }

        setContent {
            FireVisionTheme {
                val navController = rememberNavController()

                FireVisionNavGraph(navController = navController)

                // Navigate to player if deep link or autoload channel is set
                val targetChannelId = deepLinkChannelId ?: autoloadChannelId
                if (targetChannelId != null && savedInstanceState == null) {
                    navController.navigate(Screen.Player.createRoute(targetChannelId))
                }
            }
        }
    }

    private fun isTvCodeConfigured(): Boolean {
        val tvCode = SettingsActivity.getTvCode(this)
        return !tvCode.isNullOrEmpty() && tvCode != "5T6FEP"
    }

    private fun isFirstLaunch(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return !prefs.getBoolean("has_launched_before", false)
    }

    private fun markFirstLaunchComplete() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean("has_launched_before", true).apply()
    }
}
