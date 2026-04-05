package com.cadnative.firevisioniptv

import android.app.PictureInPictureParams
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.drm.AmazonDrmManager
import com.cadnative.firevisioniptv.domain.service.ChannelHealthScanner
import com.cadnative.firevisioniptv.presentation.navigation.FireVisionNavGraph
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_ENTRANCE
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.SideNavRail
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import com.cadnative.firevisioniptv.presentation.ui.screens.SplashScreen
import com.cadnative.firevisioniptv.presentation.ui.theme.DiagonalGradientBackground
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for the modernized FireVision IPTV app.
 *
 * Hosts the Compose navigation graph with orientation-adaptive navigation:
 * landscape uses a left rail sidebar, portrait uses a bottom navigation bar.
 * On first launch (no TV code), the Pairing screen is shown instead.
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {

    @Inject
    lateinit var channelHealthScanner: ChannelHealthScanner

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    companion object {
        private const val PREFS_NAME = AppPreferences.PREFS_NAME
        @Volatile
        var isPlayerActive = false
        @Volatile
        var isPlayerPlaying = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isMobileDevice(this)) {
            enableEdgeToEdge()
        }
        FirebaseApp.initializeApp(this)

        // Non-blocking DRM check — log result for Amazon Appstore compliance
        AmazonDrmManager(this).verifyLicense { licensed ->
            if (!licensed) {
                android.util.Log.w("FireVision", "DRM: App not licensed (non-blocking)")
            }
        }

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
            if (uri.host == "play" && uri.pathSegments.size >= 2 && uri.pathSegments[0] in listOf("movie", "channel")) {
                val rawId = uri.pathSegments[1]
                // Validate channelId: alphanumeric, hyphens, underscores only, max 64 chars
                if (rawId.length <= 64 && rawId.matches(Regex("^[a-zA-Z0-9_-]+$"))) rawId else null
            } else null
        }

        val targetChannelId = deepLinkChannelId
        val showSplashOnStart = savedInstanceState == null

        setContent {
            val themeStr by userPreferencesRepository.getTheme().collectAsState(initial = "system")
            val darkTheme = when (themeStr) {
                "light" -> false
                "system" -> isSystemInDarkTheme()
                else -> true
            }
            FireVisionTheme(darkTheme = darkTheme) {
                var showSplash by rememberSaveable { mutableStateOf(showSplashOnStart) }
                val navController = rememberNavController()
                val startDestination = if (needsPairing) Screen.Pairing.route else Screen.Home.route

                Box(modifier = Modifier.fillMaxSize()) {
                    FireVisionAppShell(
                        navController = navController,
                        startDestination = startDestination
                    )

                    // Deep link navigation (once only, after splash)
                    if (!showSplash && targetChannelId != null && savedInstanceState == null && !needsPairing) {
                        LaunchedEffect(targetChannelId) {
                            navController.navigate(Screen.Player.createRoute(targetChannelId))
                        }
                    }

                    // Splash overlay
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayerActive && isPlayerPlaying && isMobileDevice(this)) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        channelHealthScanner.destroy()
    }

    private fun markFirstLaunchComplete() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean("has_launched_before", true).apply()
    }
}

/**
 * App shell composable that combines the navigation chrome with the NavHost.
 *
 * In landscape (TV/tablet): left rail sidebar with SideNavRail.
 * In portrait (phone): bottom navigation bar.
 *
 * Navigation chrome is only visible on top-level screens (Home, Channels, Search,
 * Favorites, Settings). It is hidden during Pairing and Player screens.
 */
@Composable
private fun FireVisionAppShell(
    navController: NavHostController,
    startDestination: String
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showNav = currentRoute in Screen.sidebarRoutes
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val context = LocalContext.current
    val isMobile = remember { isMobileDevice(context) }

    val onNavigate: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    DiagonalGradientBackground {
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FireVisionNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (showNav) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onScreenSelected = onNavigate,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                if (showNav) {
                    SideNavRail(
                        currentRoute = currentRoute,
                        onScreenSelected = onNavigate,
                        compact = isMobile
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
}

private val bottomNavItems = listOf(
    Triple(Screen.Home, Icons.Default.Home, "Home"),
    Triple(Screen.Search, Icons.Default.Search, "Search"),
    Triple(Screen.Channels, Icons.Default.LiveTv, "Channels"),
    Triple(Screen.Categories, Icons.Default.Category, "Categories"),
    Triple(Screen.Favorites, Icons.Default.Favorite, "Favorites"),
    Triple(Screen.Settings, Icons.Default.Settings, "Settings"),
)

@Composable
private fun BottomNavBar(
    currentRoute: String?,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isPhone = screenWidthDp < 600
    val barHeight = if (isPhone) 80.dp else 64.dp
    val iconSize = if (isPhone) 26.dp else 22.dp
    val labelSize = if (isPhone) 11.sp else 10.sp

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { (screen, icon, label) ->
            val isSelected = currentRoute == screen.route ||
                (screen == Screen.Channels && currentRoute == Screen.ChannelsByCategory.route)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = { Text(text = label, maxLines = 1, fontSize = labelSize) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
