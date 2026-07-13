package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Depth tokens — a small elevation scale used to give cards, overlays and
 * dialogs tactile depth instead of relying on hairline borders alone.
 *
 * TV surfaces are mostly dark, where a plain black drop shadow reads weakly.
 * [softShadow] tints the shadow so it stays visible on Void surfaces, and is
 * a no-op at [Elevation.Level0]. Focus-driven elevation (glow + raise on
 * focus) is built on top of these in the focus system (Phase 2).
 */
object Elevation {
    val Level0: Dp = 0.dp    // flush — inline chips, flat backgrounds
    val Level1: Dp = 2.dp    // resting card / raised list item
    val Level2: Dp = 6.dp    // focused card / hovered surface
    val Level3: Dp = 12.dp   // overlays, bottom sheets, popovers
    val Level4: Dp = 24.dp   // dialogs, modal peak
}

// Shadow tint that stays perceptible on dark (Void) TV surfaces.
val ShadowColor: Color = Color(0xE60A0A12)

/**
 * Soft, brand-tuned drop shadow. Prefer this over raw [Modifier.shadow] so
 * depth stays consistent across the app. Renders nothing at [Elevation.Level0].
 */
fun Modifier.softShadow(
    elevation: Dp,
    shape: Shape,
    clip: Boolean = false,
): Modifier =
    if (elevation <= Elevation.Level0) this
    else this.shadow(
        elevation = elevation,
        shape = shape,
        clip = clip,
        ambientColor = ShadowColor,
        spotColor = ShadowColor,
    )
