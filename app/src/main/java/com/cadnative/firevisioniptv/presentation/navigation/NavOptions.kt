package com.cadnative.firevisioniptv.presentation.navigation

import androidx.navigation.NavOptionsBuilder

/**
 * Navigation options for the sidebar rail and bottom bar.
 *
 * `restoreState` is deliberately off for Home. Home is the graph's root, not a tab
 * with its own stack, and a non-inclusive `popUpTo(Home)` with `saveState` files
 * whatever it popped — a Player -> Guide chain, say — under Home's id. Restoring on
 * the Home click would then put that chain straight back on top of Home, which
 * looked like "Home is stuck on the Guide".
 */
fun NavOptionsBuilder.topLevelNavOptions(screen: Screen) {
    popUpTo(Screen.Home.route) { saveState = true }
    launchSingleTop = true
    restoreState = screen != Screen.Home
}

/**
 * Navigation options for the player's jumps to Guide, Settings and Search.
 *
 * The player is replaced rather than stacked under. Its route is parameterised by
 * channel id, so `launchSingleTop` does not dedupe across channels and the
 * Player -> Guide -> pick a channel -> Player loop would grow the stack without
 * bound, leaving sidebar screens sitting on top of a still-live player.
 */
fun NavOptionsBuilder.fromPlayerNavOptions() {
    popUpTo(Screen.Player.route) { inclusive = true }
    launchSingleTop = true
}
