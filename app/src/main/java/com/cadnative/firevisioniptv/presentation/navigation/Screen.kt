package com.cadnative.firevisioniptv.presentation.navigation

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Sealed class representing all navigation destinations in the app.
 */
sealed class Screen(val route: String) {
    object Pairing : Screen("pairing")
    object Home : Screen("home")
    object Channels : Screen("channels")
    object Categories : Screen("categories")
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Player : Screen("player/{channelId}") {
        fun createRoute(channelId: String) = "player/$channelId"
    }
    object ChannelsByCategory : Screen("channels/category/{categoryId}") {
        fun createRoute(categoryId: String): String {
            val encoded = URLEncoder.encode(categoryId, "UTF-8")
            return "channels/category/$encoded"
        }
        fun decodeCategory(raw: String): String =
            URLDecoder.decode(raw, "UTF-8")
    }

    companion object {
        /** Route strings for top-level screens that show the sidebar navigation rail. */
        val sidebarRoutes = setOf("home", "channels", "categories", "search", "favorites", "settings")
    }
}
