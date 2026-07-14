package com.cadnative.firevisioniptv.presentation.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

// After exiting fullscreen we force PORTRAIT, then release to UNSPECIFIED once
// the rotation settles so physical rotation (and the system auto-rotate toggle)
// works again.
private const val ORIENTATION_RELEASE_DELAY_MS = 2000L

/** Actuator for the mobile player's fullscreen toggle. Layout reads LocalConfiguration. */
@Stable
internal class PlayerOrientationController(private val activity: Activity?) {
    fun enterFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

/**
 * Owns the mobile player's orientation + immersive-mode side effects:
 * landscape = system bars hidden, portrait = visible; on leaving the player,
 * bars/orientation/brightness overrides are all restored. The manifest declares
 * `configChanges=orientation|screenSize`, so rotation never recreates the
 * activity and this all runs as plain recomposition.
 */
@Composable
internal fun rememberPlayerOrientationController(
    isMobile: Boolean,
    isLandscape: Boolean
): PlayerOrientationController {
    val activity = LocalContext.current as? Activity
    val controller = remember { PlayerOrientationController(activity) }

    // Release the forced-portrait hold once the device reports portrait
    LaunchedEffect(isLandscape) {
        if (isMobile && !isLandscape &&
            activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ) {
            delay(ORIENTATION_RELEASE_DELAY_MS)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Immersive bars follow orientation while in the player
    DisposableEffect(isMobile, isLandscape) {
        if (isMobile) {
            activity?.window?.let { window ->
                val insets = WindowInsetsControllerCompat(window, window.decorView)
                if (isLandscape) {
                    insets.hide(WindowInsetsCompat.Type.systemBars())
                    insets.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insets.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        onDispose { }
    }

    // Leaving the player: restore bars, orientation, and any brightness override
    DisposableEffect(isMobile) {
        onDispose {
            if (isMobile) {
                activity?.let { act ->
                    act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    act.window?.let { window ->
                        WindowInsetsControllerCompat(window, window.decorView)
                            .show(WindowInsetsCompat.Type.systemBars())
                        val attrs = window.attributes
                        attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        window.attributes = attrs
                    }
                }
            }
        }
    }

    return controller
}
