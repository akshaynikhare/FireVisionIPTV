package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Full-screen background with a subtle dual-tone diagonal gradient.
 *
 * Dark mode: Flame glow top-left, info-blue glow bottom-right over Void950.
 * Light mode: Very subtle Flame warmth top-left over Parchment50 — keeps
 * the parchment feel without competing with content.
 *
 * GPU-accelerated via [drawBehind] — no recomposition on frame draws.
 */
@Composable
fun DiagonalGradientBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val baseColor = if (darkTheme) Void950 else Parchment50
    val startGlow = if (darkTheme) AmberGlow else Color(0x08E07818)   // ~3% Flame400
    val endGlow   = if (darkTheme) SteelBlueGlow else Color(0x00000000) // none in light

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
            .drawBehind {
                // Flame glow — top-left corner fading toward center
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(startGlow, Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.6f, size.height * 0.6f)
                    )
                )
                // Blue glow — bottom-right corner fading toward center (dark only)
                if (darkTheme) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, endGlow),
                            start = Offset(size.width * 0.4f, size.height * 0.4f),
                            end = Offset(size.width, size.height)
                        )
                    )
                }
            },
        content = content
    )
}
