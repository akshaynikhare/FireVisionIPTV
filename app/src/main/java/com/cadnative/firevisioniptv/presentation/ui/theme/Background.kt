package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.foundation.background
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
 * Amber glows from the top-left corner, steel blue from the bottom-right,
 * both at ~3 % opacity over the near-black base. GPU-accelerated via
 * [drawBehind] — no recomposition on frame draws.
 */
@Composable
fun DiagonalGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .drawBehind {
                // Amber glow — top-left corner fading toward center
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(AmberGlow, Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.6f, size.height * 0.6f)
                    )
                )
                // Steel blue glow — bottom-right corner fading toward center
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, SteelBlueGlow),
                        start = Offset(size.width * 0.4f, size.height * 0.4f),
                        end = Offset(size.width, size.height)
                    )
                )
            },
        content = content
    )
}
