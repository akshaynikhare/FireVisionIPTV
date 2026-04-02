package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_BLOOM_DURATION
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_HOLD_DURATION_MS
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_IGNITION_DURATION
import com.cadnative.firevisioniptv.presentation.ui.animation.SPLASH_SWEEP_DURATION
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BackgroundDark
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue
import kotlinx.coroutines.delay

/**
 * Signal Sweep splash screen — CRT-inspired horizontal scanline reveals the logo.
 *
 * Four-phase animation (~1.5 s total):
 * 1. Ignition (100 ms) — faint amber point of light appears at center-left
 * 2. Sweep (700 ms) — glowing scanline sweeps L→R, progressively revealing logo text
 * 3. Bloom (400 ms) — scanline fades, radial amber glow blooms behind the logo
 * 4. Hold (300 ms) — brief pause before calling [onSplashFinished]
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    var phase by remember { mutableStateOf(SplashPhase.Ignition) }

    LaunchedEffect(Unit) {
        delay(SPLASH_IGNITION_DURATION.toLong())
        phase = SplashPhase.Sweep
        delay(SPLASH_SWEEP_DURATION.toLong())
        phase = SplashPhase.Bloom
        delay(SPLASH_BLOOM_DURATION.toLong())
        phase = SplashPhase.Hold
        delay(SPLASH_HOLD_DURATION_MS)
        phase = SplashPhase.Finished
        onSplashFinished()
    }

    // ── Animated values ──────────────────────────────────────────────

    // Phase 1: ignition point of light
    val ignitionAlpha by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.Ignition) 1f else 0f,
        animationSpec = tween(SPLASH_IGNITION_DURATION, easing = EaseOutQuart),
        label = "ignitionAlpha"
    )

    // Phase 2: scanline sweeps left → right (linear for constant velocity)
    val sweepProgress by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.Sweep) 1f else 0f,
        animationSpec = tween(SPLASH_SWEEP_DURATION, easing = LinearEasing),
        label = "sweepProgress"
    )

    // Phase 3: scanline fades out, bloom expands
    val bloomRadius by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.Bloom) 1f else 0f,
        animationSpec = tween(SPLASH_BLOOM_DURATION, easing = EaseOutQuart),
        label = "bloomRadius"
    )

    // CRT lines fade out during bloom
    val crtLinesAlpha by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.Bloom) 0f else 1f,
        animationSpec = tween(SPLASH_BLOOM_DURATION / 2, easing = EaseOutQuart),
        label = "crtLinesAlpha"
    )

    val scanlineHalfWidthPx = with(LocalDensity.current) { 12.dp.toPx() }

    // ── Layout ───────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .drawBehind {
                val w = size.width
                val h = size.height

                // ── CRT horizontal scan lines (subtle retro texture) ──
                if (crtLinesAlpha > 0.01f) {
                    val lineAlpha = 0.04f * crtLinesAlpha
                    val lineColor = Color.White.copy(alpha = lineAlpha)
                    val lineCount = 3
                    for (i in 1..lineCount) {
                        val y = h * i / (lineCount + 1)
                        drawRect(
                            color = lineColor,
                            topLeft = Offset(0f, y),
                            size = Size(w, 1.dp.toPx())
                        )
                    }
                }

                // ── Ignition: point of light at center-left ──
                if (ignitionAlpha > 0f && phase == SplashPhase.Ignition) {
                    val cx = w * 0.15f
                    val cy = h * 0.5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Amber.copy(alpha = 0.6f * ignitionAlpha),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = 40.dp.toPx()
                        ),
                        radius = 40.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // ── Scanline glow strip ──
                if (sweepProgress > 0f && phase < SplashPhase.Hold) {
                    val scanX = sweepProgress * w
                    // Fade scanline out as bloom starts
                    val scanAlpha = if (phase >= SplashPhase.Bloom) 1f - bloomRadius else 1f

                    if (scanAlpha > 0.01f) {
                        val left = (scanX - scanlineHalfWidthPx).coerceAtLeast(0f)
                        val right = (scanX + scanlineHalfWidthPx).coerceAtMost(w)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Amber.copy(alpha = 0.5f * scanAlpha),
                                    Amber.copy(alpha = 0.85f * scanAlpha),
                                    Amber.copy(alpha = 0.5f * scanAlpha),
                                    Color.Transparent
                                ),
                                startX = left,
                                endX = right
                            ),
                            topLeft = Offset(left, 0f),
                            size = Size(right - left, h)
                        )
                    }
                }

                // ── Bloom: radial amber glow behind logo ──
                if (bloomRadius > 0f) {
                    val maxRadius = size.minDimension * 0.5f
                    val currentRadius = maxRadius * bloomRadius.coerceAtLeast(0.01f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Amber.copy(alpha = 0.15f * bloomRadius),
                                Color.Transparent
                            ),
                            center = center,
                            radius = currentRadius
                        ),
                        radius = currentRadius,
                        center = center
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // ── Logo text — clipped by scanline sweep ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .drawWithContent {
                    val clipRight = if (phase >= SplashPhase.Bloom) {
                        size.width // fully revealed
                    } else {
                        sweepProgress * size.width
                    }
                    clipRect(right = clipRight) {
                        this@drawWithContent.drawContent()
                    }
                }
                .graphicsLayer {
                    // Subtle scale-up during bloom for a finishing touch
                    val scale = if (phase >= SplashPhase.Bloom) 1f + (0.05f * bloomRadius) else 1f
                    scaleX = scale
                    scaleY = scale
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
    Ignition,
    Sweep,
    Bloom,
    Hold,
    Finished
}
