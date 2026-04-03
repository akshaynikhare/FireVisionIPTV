package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.Typeface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_FADE_OUT_DURATION
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_MIN_DISPLAY_MS
import com.cadnative.firevisioniptv.presentation.ui.theme.Void950
import kotlinx.coroutines.delay

private const val LOTTIE_TOTAL_FRAMES = 90
private const val LOTTIE_LOOP_START_FRAME = 60

/**
 * Cinematic Lottie splash screen.
 *
 * Plays `assets/splash_animation.json` in a seamless loop on a dark Void950
 * background. After [SPLASH_MIN_DISPLAY_MS] (one full loop), the splash
 * fades out and fires [onSplashFinished].
 *
 * The Lottie file is expected to contain:
 * - Deep gradient background (#0A0A12 → #191921)
 * - Radial amber glow with breathing animation
 * - FireVision logo with scale-up + blur-to-sharp reveal
 * - Flame flicker, micro-parallax, ambient particles
 * - Optional loading dots / progress bar at the bottom
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // ── Lottie composition ───────────────────────────────────────────

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("splash_animation.json")
    )

    // Map Lottie font to Android system typeface (Arial not available on Android)
    val fontMap = remember {
        mapOf("Arial-BoldMT" to Typeface.create("sans-serif", Typeface.BOLD))
    }

    // Phase 0 = intro (full 1.5s), Phase 1 = loop last 0.5s
    var looping by remember { mutableStateOf(false) }

    // Play full animation once, then switch to looping last segment
    val introProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = !looping
    )

    val loopProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = looping,
        clipSpec = LottieClipSpec.Progress(LOTTIE_LOOP_START_FRAME.toFloat() / LOTTIE_TOTAL_FRAMES, 1f)
    )

    // Switch to loop when intro completes
    LaunchedEffect(introProgress) {
        if (introProgress == 1f) {
            looping = true
        }
    }

    // ── Timed exit ───────────────────────────────────────────────────

    var fadingOut by remember { mutableStateOf(false) }

    LaunchedEffect(composition) {
        if (composition != null) {
            delay(SPLASH_MIN_DISPLAY_MS)
            fadingOut = true
            delay(SPLASH_FADE_OUT_DURATION.toLong())
            onSplashFinished()
        }
    }

    // Fallback: if Lottie never loads, still advance after a max timeout
    LaunchedEffect(Unit) {
        delay(SPLASH_MIN_DISPLAY_MS + SPLASH_FADE_OUT_DURATION.toLong() + 1000L)
        // Only fire if the composition-based exit hasn't already run
        if (!fadingOut) {
            onSplashFinished()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (fadingOut) 0f else 1f,
        animationSpec = tween(SPLASH_FADE_OUT_DURATION, easing = EaseOutQuart),
        label = "splashFadeOut"
    )

    // ── Layout ───────────────────────────────────────────────────────

    val currentProgress = if (looping) loopProgress else introProgress

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void950)
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { currentProgress },
            modifier = Modifier.fillMaxSize(),
            fontMap = fontMap
        )
    }
}
