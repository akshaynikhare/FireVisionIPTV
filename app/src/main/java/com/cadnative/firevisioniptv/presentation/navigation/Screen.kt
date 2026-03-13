package com.cadnative.firevisioniptv.presentation.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Channels : Screen("channels")
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Player : Screen("player/{channelId}") {
        fun createRoute(channelId: String) = "player/$channelId"
    }
    object ChannelsByCategory : Screen("channels/category/{categoryId}") {
        fun createRoute(categoryId: String) = "channels/category/$categoryId"
    }
}
