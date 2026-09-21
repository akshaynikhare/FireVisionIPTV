package com.cadnative.firevisioniptv.presentation.navigation

import android.app.Application
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Back-stack shapes for the two navigation paths that have regressed before:
 * the rail restoring a chain it should not have saved, and the player stacking
 * itself underneath sidebar screens.
 */
// Plain Application rather than FireVisionApplication: the real one starts Hilt,
// Sentry and WorkManager, none of which these tests need, and loading the manifest's
// declared receivers pulls in the Amazon DRM jar, whose pre-Java-7 bytecode fails
// verification on a modern JVM.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, manifest = Config.NONE)
class NavOptionsTest {

    private lateinit var navController: TestNavHostController

    private val routes = listOf(
        Screen.Home.route,
        Screen.Guide.route,
        Screen.Settings.route,
        Screen.Search.route,
        Screen.Channels.route,
        Screen.Favorites.route,
        Screen.Player.route
    )

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph = navController.createGraph(startDestination = Screen.Home.route) {
            routes.forEach { route -> composable(route) {} }
        }
    }

    /** Routes currently on the back stack, oldest first, ignoring graph entries. */
    private fun backStack(): List<String> =
        navController.currentBackStack.value
            .mapNotNull { it.destination.route }
            .filter { it in routes }

    private fun goToPlayer() =
        navController.navigate(Screen.Player.createRoute("ch1"))

    @Test
    fun `player jumping to guide leaves no player behind it`() {
        goToPlayer()
        navController.navigate(Screen.Guide.route) { fromPlayerNavOptions() }

        assertEquals(listOf(Screen.Home.route, Screen.Guide.route), backStack())
    }

    @Test
    fun `repeated player to guide loops do not grow the stack`() {
        // The player route is parameterised by channel id, so launchSingleTop alone
        // never dedupes it — this is the loop that used to accumulate entries.
        repeat(3) {
            goToPlayer()
            navController.navigate(Screen.Guide.route) { fromPlayerNavOptions() }
        }

        assertEquals(listOf(Screen.Home.route, Screen.Guide.route), backStack())
    }

    @Test
    fun `home click does not restore a player guide chain`() {
        goToPlayer()
        navController.navigate(Screen.Guide.route) { fromPlayerNavOptions() }
        navController.navigate(Screen.Settings.route) { topLevelNavOptions(Screen.Settings) }

        navController.navigate(Screen.Home.route) { topLevelNavOptions(Screen.Home) }

        assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
        assertFalse(
            "Home must not restore a saved chain — it is the root, not a tab",
            backStack().any { it == Screen.Guide.route || it == Screen.Player.route }
        )
    }

    @Test
    fun `sibling tabs still restore their own state`() {
        // restoreState is only disabled for Home; disabling it everywhere would
        // have been an easy over-correction.
        navController.navigate(Screen.Channels.route) { topLevelNavOptions(Screen.Channels) }
        navController.navigate(Screen.Favorites.route) { topLevelNavOptions(Screen.Favorites) }
        navController.navigate(Screen.Channels.route) { topLevelNavOptions(Screen.Channels) }

        assertEquals(Screen.Channels.route, navController.currentBackStackEntry?.destination?.route)
        assertEquals(listOf(Screen.Home.route, Screen.Channels.route), backStack())
    }

    @Test
    fun `rail navigation keeps home at the root`() {
        navController.navigate(Screen.Guide.route) { topLevelNavOptions(Screen.Guide) }
        assertEquals(listOf(Screen.Home.route, Screen.Guide.route), backStack())
    }
}
