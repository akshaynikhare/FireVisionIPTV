package com.cadnative.firevisioniptv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

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
        // Home screen
        composable(route = Screen.Home.route) {
            // HomeScreen will be implemented in next task
            // HomeScreen(
            //     onNavigateToChannels = { navController.navigate(Screen.Channels.route) },
            //     onNavigateToSearch = { navController.navigate(Screen.Search.route) },
            //     onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
            //     onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            //     onChannelClick = { channelId -> 
            //         navController.navigate(Screen.Player.createRoute(channelId))
            //     }
            // )
        }

        // Channels screen
        composable(route = Screen.Channels.route) {
            // ChannelsScreen will be implemented in next task
            // ChannelsScreen(
            //     onNavigateBack = { navController.popBackStack() },
            //     onChannelClick = { channelId ->
            //         navController.navigate(Screen.Player.createRoute(channelId))
            //     },
            //     onCategoryClick = { categoryId ->
            //         navController.navigate(Screen.ChannelsByCategory.createRoute(categoryId))
            //     }
            // )
        }

        // Channels by category screen
        composable(
            route = Screen.ChannelsByCategory.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            // ChannelsByCategoryScreen will be implemented later
            // ChannelsByCategoryScreen(
            //     categoryId = categoryId,
            //     onNavigateBack = { navController.popBackStack() },
            //     onChannelClick = { channelId ->
            //         navController.navigate(Screen.Player.createRoute(channelId))
            //     }
            // )
        }

        // Search screen
        composable(route = Screen.Search.route) {
            // SearchScreen will be implemented in next task
            // SearchScreen(
            //     onNavigateBack = { navController.popBackStack() },
            //     onChannelClick = { channelId ->
            //         navController.navigate(Screen.Player.createRoute(channelId))
            //     }
            // )
        }

        // Favorites screen
        composable(route = Screen.Favorites.route) {
            // FavoritesScreen will be implemented in next task
            // FavoritesScreen(
            //     onNavigateBack = { navController.popBackStack() },
            //     onChannelClick = { channelId ->
            //         navController.navigate(Screen.Player.createRoute(channelId))
            //     }
            // )
        }

        // Settings screen
        composable(route = Screen.Settings.route) {
            // SettingsScreen will be implemented in next task
            // SettingsScreen(
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }

        // Player screen
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
            // PlayerScreen will be implemented in Phase 7
            // PlayerScreen(
            //     channelId = channelId,
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }
    }
}
