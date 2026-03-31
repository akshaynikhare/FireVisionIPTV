package com.cadnative.firevisioniptv

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.domain.service.ChannelHealthScanner
import com.cadnative.firevisioniptv.presentation.navigation.FireVisionNavGraph
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_ENTRANCE
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.SideNavRail
import com.cadnative.firevisioniptv.presentation.ui.screens.SplashScreen
import com.cadnative.firevisioniptv.presentation.ui.theme.DiagonalGradientBackground
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for the modernized FireVision IPTV app.
 *
 * Hosts the Compose navigation graph with a left rail sidebar.
 * On first launch (no TV code), the Pairing screen is shown instead.
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {

    @Inject
    lateinit var channelHealthScanner: ChannelHealthScanner

    companion object {
        private const val PREFS_NAME = AppPreferences.PREFS_NAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val needsPairing = !isTvCodeConfigured()
        if (isFirstLaunch()) {
            markFirstLaunchComplete()
        }

        // Start background channel health scanning
        if (!needsPairing) {
            channelHealthScanner.startAutoScan()
        }

        // Parse deep link channel ID if present
        val deepLinkChannelId = intent?.data?.let { uri ->
            if (uri.host == "play" && uri.pathSegments.size >= 2 && uri.pathSegments[0] == "movie") {
                uri.pathSegments[1]
            } else null
        }

        val targetChannelId = deepLinkChannelId
        val showSplashOnStart = savedInstanceState == null

        setContent {
            FireVisionTheme {
                var showSplash by rememberSaveable { mutableStateOf(showSplashOnStart) }
                val navController = rememberNavController()
                val startDestination = if (needsPairing) Screen.Pairing.route else Screen.Home.route

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(DURATION_ENTRANCE, easing = EaseOutQuart),
                    label = "splashTransition"
                ) { isSplashing ->
                    if (isSplashing) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        FireVisionAppShell(
                            navController = navController,
                            startDestination = startDestination
                        )

                        // Navigate to player if deep link channel is set (once only)
                        if (targetChannelId != null && savedInstanceState == null && !needsPairing) {
                            LaunchedEffect(targetChannelId) {
                                navController.navigate(Screen.Player.createRoute(targetChannelId))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isTvCodeConfigured(): Boolean {
        return AppPreferences.hasChannelSelection(this)
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

    DiagonalGradientBackground {
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
}
