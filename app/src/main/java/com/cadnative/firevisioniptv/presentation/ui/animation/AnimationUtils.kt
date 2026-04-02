package com.cadnative.firevisioniptv.presentation.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

// ── Easing ──────────────────────────────────────────────────────────

/** Smooth, refined deceleration — premium cinematic feel */
val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

// ── Durations ───────────────────────────────────────────────────────

const val DURATION_FAST = 150        // Instant feedback (press, toggle)
const val DURATION_NORMAL = 250      // State changes (hover, selection)
const val DURATION_ENTRANCE = 350    // Screen / content entrance
const val DURATION_EXIT = 200        // Exit (faster than entrance)
const val STAGGER_DELAY_MS = 50L     // Between staggered items
const val DURATION_OVERLAY = 300     // Overlay slide in/out
const val AUTO_HIDE_DELAY_MS = 7000L // Channel overlay auto-hide timeout

// ── Splash Animation (Lottie) ────────────────────────────────────
const val SPLASH_MIN_DISPLAY_MS = 1500L         // Minimum time to show splash (one full Lottie loop)
const val SPLASH_FADE_OUT_DURATION = 400        // Crossfade out of splash screen

// ── Navigation Transitions ──────────────────────────────────────────

fun screenEnterTransition(): EnterTransition =
    fadeIn(tween(DURATION_ENTRANCE, easing = EaseOutQuart))

fun screenExitTransition(): ExitTransition =
    fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart))

fun screenPopEnterTransition(): EnterTransition =
    fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart))

fun screenPopExitTransition(): ExitTransition =
    fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart))

// ── Item Entrance Modifier ──────────────────────────────────────────

/**
 * Animates an item's entrance with a staggered fade + upward slide.
 * Use on grid/list items for a cinematic cascade effect.
 *
 * GPU-accelerated: uses only graphicsLayer (transform + opacity).
 *
 * @param index Item position for stagger delay.
 * @param maxStagger Cap on stagger count so late items don't feel slow.
 */
fun Modifier.animateItemEntrance(
    index: Int,
    maxStagger: Int = 8
): Modifier = composed {
    var appeared by remember { mutableStateOf(false) }
    val staggerDelay = (index.coerceAtMost(maxStagger) * STAGGER_DELAY_MS).toInt()

    LaunchedEffect(Unit) {
        delay(staggerDelay.toLong())
        appeared = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(DURATION_ENTRANCE, easing = EaseOutQuart),
        label = "itemAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (appeared) 0f else 30f,
        animationSpec = tween(DURATION_ENTRANCE, easing = EaseOutQuart),
        label = "itemTranslateY"
    )

    graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}

/**
 * Simple fade-in entrance for a single element (no stagger).
 */
fun Modifier.animateFadeIn(): Modifier = composed {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(DURATION_ENTRANCE, easing = EaseOutQuart),
        label = "fadeInAlpha"
    )

    graphicsLayer { this.alpha = alpha }
}
