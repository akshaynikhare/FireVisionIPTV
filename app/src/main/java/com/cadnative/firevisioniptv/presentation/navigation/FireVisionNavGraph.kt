package com.cadnative.firevisioniptv.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_ENTRANCE
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.screenEnterTransition
import com.cadnative.firevisioniptv.presentation.ui.animation.screenExitTransition
import com.cadnative.firevisioniptv.presentation.ui.animation.screenPopEnterTransition
import com.cadnative.firevisioniptv.presentation.ui.animation.screenPopExitTransition
import com.cadnative.firevisioniptv.presentation.ui.screens.CategoriesScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.ChannelsScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.FavoritesScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.HomeScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.MultiviewScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.guide.GuideScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.PairingScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.PlayerScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.SearchScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.AddSourceScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsScreen
import com.cadnative.firevisioniptv.presentation.viewmodel.PairingViewModel
import kotlinx.coroutines.delay

/**
 * Navigation graph for FireVision IPTV app.
 *
 * Defines all navigation routes and handles navigation between screens.
 * The sidebar-based navigation for top-level screens is handled by the
 * parent composable in ComposeMainActivity; this graph only defines destinations.
 *
 * @param startDestination The initial route — either Pairing or Home.
 */
@Composable
fun FireVisionNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalPerfProfile.current.reduceMotion
    val enterSlideOffsetPx = with(LocalDensity.current) { 16.dp.roundToPx() }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (reduceMotion) screenEnterTransition()
            else screenEnterTransition() +
                    slideInVertically(tween(DURATION_ENTRANCE, easing = EaseOutQuart)) { enterSlideOffsetPx }
        },
        exitTransition = { screenExitTransition() },
        popEnterTransition = { screenPopEnterTransition() },
        popExitTransition = { screenPopExitTransition() }
    ) {
        // ── Pairing (onboarding) ────────────────────────────────────────
        composable(route = Screen.Pairing.route) {
            val viewModel: PairingViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Auto-navigate to Home after successful pairing
            LaunchedEffect(uiState.isPaired) {
                if (uiState.isPaired) {
                    delay(1500) // Show success message briefly
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            PairingScreen(
                pin = uiState.pin,
                statusMessage = uiState.statusMessage,
                statusColor = uiState.statusColor,
                countdownText = uiState.countdownText,
                isLoading = uiState.isLoading,
                showRetryButton = uiState.showRetryButton,
                showCountdown = uiState.showCountdown,
                qrCodeBitmap = uiState.qrCodeBitmap,
                serverUrl = uiState.serverUrl,
                isTvDevice = uiState.isTvDevice,
                pairingUrl = uiState.pairingUrl,
                onRetryClick = { viewModel.requestNewPairing() },
                onUseDefaultClick = { viewModel.useDefaultChannelList() },
                onUseAdvancedClick = { navController.navigate(Screen.AddSource.route) }
            )
        }

        // ── Home ────────────────────────────────────────────────────────
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToChannels = { category ->
                    navController.navigate(Screen.ChannelsByCategory.createRoute(category))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onPairDevice = {
                    navController.navigate(Screen.Pairing.route)
                },
                onMultiviewClick = { channelId ->
                    navController.navigate(Screen.Multiview.createRoute(channelId))
                }
            )
        }

        // ── Categories ─────────────────────────────────────────────────
        composable(route = Screen.Categories.route) {
            CategoriesScreen(
                onCategoryClick = { category ->
                    navController.navigate(Screen.ChannelsByCategory.createRoute(category))
                }
            )
        }

        // ── Channels ────────────────────────────────────────────────────
        composable(route = Screen.Channels.route) {
            ChannelsScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onPairDevice = {
                    navController.navigate(Screen.Pairing.route)
                },
                onMultiviewClick = { channelId ->
                    navController.navigate(Screen.Multiview.createRoute(channelId))
                }
            )
        }

        // ── Channels by Category ────────────────────────────────────────
        composable(
            route = Screen.ChannelsByCategory.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
                ?.let { Screen.ChannelsByCategory.decodeCategory(it) }
            ChannelsScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onPairDevice = {
                    navController.navigate(Screen.Pairing.route)
                },
                onMultiviewClick = { channelId ->
                    navController.navigate(Screen.Multiview.createRoute(channelId))
                },
                initialCategory = categoryId
            )
        }

        // ── Guide (EPG program grid) ────────────────────────────────────
        composable(route = Screen.Guide.route) {
            GuideScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onCatchup = { channelId, startMillis, durationMinutes ->
                    navController.navigate(
                        Screen.Player.createCatchupRoute(channelId, startMillis, durationMinutes)
                    )
                },
                onPairDevice = {
                    navController.navigate(Screen.Pairing.route)
                }
            )
        }

        // ── Multiview (watch several channels at once) ──────────────────
        composable(
            route = Screen.Multiview.route,
            arguments = listOf(
                navArgument("channelId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            MultiviewScreen(
                onNavigateBack = { navController.popBackStack() },
                initialChannelId = backStackEntry.arguments?.getString("channelId")
            )
        }

        // ── Search ──────────────────────────────────────────────────────
        composable(route = Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onMultiviewClick = { channelId ->
                    navController.navigate(Screen.Multiview.createRoute(channelId))
                }
            )
        }

        // ── Favorites ───────────────────────────────────────────────────
        composable(route = Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onCategoryClick = { categoryName ->
                    navController.navigate(Screen.ChannelsByCategory.createRoute(categoryName))
                },
                onMultiviewClick = { channelId ->
                    navController.navigate(Screen.Multiview.createRoute(channelId))
                }
            )
        }

        // ── Settings ────────────────────────────────────────────────────
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onPairDevice = {
                    navController.navigate(Screen.Pairing.route)
                },
                onResetPairing = {
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToSelfHost = {
                    navController.navigate(Screen.AddSource.route)
                }
            )
        }

        // ── Add a different source (self-hosted / M3U / Xtream) ────────
        composable(route = Screen.AddSource.route) {
            AddSourceScreen(
                onNavigateBack = { navController.popBackStack() },
                onPairDevice = {
                    navController.navigate(Screen.Pairing.route)
                },
                onPlaylistLoaded = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Player ──────────────────────────────────────────────────────
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("catchupStart") { type = NavType.LongType; defaultValue = 0L },
                navArgument("catchupDur") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId")
            val catchupStart = backStackEntry.arguments?.getLong("catchupStart") ?: 0L
            val catchupDur = backStackEntry.arguments?.getInt("catchupDur") ?: 0
            if (channelId.isNullOrEmpty()) {
                // Guard: pop back if channelId is missing
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                PlayerScreen(
                    channelId = channelId,
                    catchupStartMs = catchupStart,
                    catchupDurationMin = catchupDur,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToGuide = {
                        navController.navigate(Screen.Guide.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
