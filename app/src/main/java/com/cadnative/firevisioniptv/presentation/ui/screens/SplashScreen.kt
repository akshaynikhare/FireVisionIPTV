package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_GLOW_EXPAND_DURATION
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_HOLD_DURATION_MS
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_LOGO_FADE_DURATION
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_REVEAL_DURATION
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BackgroundDark
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue
import kotlinx.coroutines.delay

/**
 * Animated splash screen shown on cold launch.
 *
 * Three-phase animation sequence (~1.5 s total):
 * 1. Logo fade-in (400 ms)
 * 2. Amber radial glow expands behind logo (500 ms)
 * 3. Scale-up reveal (400 ms) + brief hold (200 ms)
 *
 * Calls [onSplashFinished] when the animation completes.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    var phase by remember { mutableStateOf(SplashPhase.LogoFadeIn) }

    // Sequence the animation phases
    LaunchedEffect(Unit) {
        delay(SPLASH_LOGO_FADE_DURATION.toLong())
        phase = SplashPhase.GlowExpand
        delay(SPLASH_GLOW_EXPAND_DURATION.toLong())
        phase = SplashPhase.Reveal
        delay(SPLASH_REVEAL_DURATION.toLong() + SPLASH_HOLD_DURATION_MS)
        phase = SplashPhase.Finished
        onSplashFinished()
    }

    // ── Animated values ──────────────────────────────────────────────

    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.LogoFadeIn) 1f else 0f,
        animationSpec = tween(SPLASH_LOGO_FADE_DURATION, easing = EaseOutQuart),
        label = "splashLogoAlpha"
    )

    val glowRadius by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.GlowExpand) 1f else 0f,
        animationSpec = tween(SPLASH_GLOW_EXPAND_DURATION, easing = EaseOutQuart),
        label = "splashGlowRadius"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.Reveal) 1.15f else 1f,
        animationSpec = tween(SPLASH_REVEAL_DURATION, easing = EaseOutQuart),
        label = "splashLogoScale"
    )

    // ── Layout ───────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .drawBehind {
                // Animated amber radial glow behind the logo
                val maxRadius = size.minDimension * 0.5f
                val currentRadius = maxRadius * glowRadius.coerceAtLeast(0.01f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Amber.copy(alpha = 0.15f * glowRadius),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentRadius
                    ),
                    radius = currentRadius,
                    center = center
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = logoAlpha
                scaleX = logoScale
                scaleY = logoScale
            }
        ) {
            Text(
                text = "FireVision",
                color = Amber,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "IPTV",
                color = SteelBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }
    }
}

// ── Internal helpers ─────────────────────────────────────────────────

private enum class SplashPhase {
    LogoFadeIn,
    GlowExpand,
    Reveal,
    Finished
}
