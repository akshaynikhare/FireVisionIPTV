package com.cadnative.firevisioniptv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cadnative.firevisioniptv.presentation.ui.screens.ChannelsScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.FavoritesScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.HomeScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.PlayerScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.SearchScreen
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsScreen

/**
 * Navigation graph for FireVision IPTV app.
 *
 * Defines all navigation routes and handles navigation between screens.
 */
@Composable
fun FireVisionNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToChannels = { navController.navigate(Screen.Channels.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                }
            )
        }

        composable(route = Screen.Channels.route) {
            ChannelsScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.ChannelsByCategory.createRoute(categoryId))
                }
            )
        }

        composable(
            route = Screen.ChannelsByCategory.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            // Reuses ChannelsScreen with category filter - to be implemented
            ChannelsScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onCategoryClick = { }
            )
        }

        composable(route = Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                }
            )
        }

        composable(route = Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
            PlayerScreen(
                channelId = channelId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
