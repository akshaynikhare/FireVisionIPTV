package com.cadnative.firevisioniptv

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cadnative.firevisioniptv.presentation.navigation.FireVisionNavGraph
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.components.SideNavRail
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the modernized FireVision IPTV app.
 *
 * Hosts the Compose navigation graph with a left rail sidebar.
 * On first launch (no TV code), the Pairing screen is shown instead.
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "FireVisionSettings"
        /** Must match SettingsActivity.DEFAULT_TV_CODE */
        private const val DEFAULT_TV_CODE = "5T6FEP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val needsPairing = isFirstLaunch() && !isTvCodeConfigured()
        if (needsPairing) {
            markFirstLaunchComplete()
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

        val targetChannelId = deepLinkChannelId ?: autoloadChannelId

        setContent {
            FireVisionTheme {
                val navController = rememberNavController()
                val startDestination = if (needsPairing) Screen.Pairing.route else Screen.Home.route

                FireVisionAppShell(
                    navController = navController,
                    startDestination = startDestination
                )

                // Navigate to player if deep link or autoload channel is set (once only)
                if (targetChannelId != null && savedInstanceState == null && !needsPairing) {
                    LaunchedEffect(targetChannelId) {
                        navController.navigate(Screen.Player.createRoute(targetChannelId))
                    }
                }
            }
        }
    }

    private fun isTvCodeConfigured(): Boolean {
        val tvCode = SettingsActivity.getTvCode(this)
        return !tvCode.isNullOrEmpty() && tvCode != DEFAULT_TV_CODE
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

/**
 * App shell composable that combines the left rail sidebar with the NavHost.
 *
 * The sidebar is only visible on top-level screens (Home, Channels, Search,
 * Favorites, Settings). It is hidden during Pairing and Player screens.
 */
@Composable
private fun FireVisionAppShell(
    navController: NavHostController,
    startDestination: String
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showSidebar = currentRoute in Screen.sidebarRoutes

    Row(modifier = Modifier.fillMaxSize()) {
        if (showSidebar) {
            SideNavRail(
                currentRoute = currentRoute,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        FireVisionNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.weight(1f)
        )
    }
}
