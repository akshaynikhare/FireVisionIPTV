package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FOCUS
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_TRANSITION
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_CARD
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation
import com.cadnative.firevisioniptv.presentation.ui.theme.ShadowColor

/**
 * The single, unified TV focus treatment for the whole app.
 *
 * On focus a surface scales up and gains a brand-tinted glow (a colored drop
 * shadow — the Android-TV "glow" focus indicator); unfocused it rests on a
 * subtle neutral shadow. This replaces ad-hoc per-component scale+border so
 * every focusable surface reacts to the D-pad identically and legibly from
 * across a room.
 *
 * The caller owns focus tracking (`onFocusChanged`) and passes [focused], and
 * draws its own background/border/clip on top. Motion is gated by
 * [LocalPerfProfile] — low-end boxes jump straight to the end state.
 *
 * Colored shadows require API 28+ (the app's minSdk), so the glow renders on
 * every supported device.
 *
 * @param focused whether this surface currently holds D-pad focus.
 * @param shape the surface shape — shadow/glow follow it.
 * @param focusedScale scale applied when focused (see FOCUS_SCALE_* tokens).
 * @param glowColor glow tint; defaults to the theme primary when unspecified.
 * @param restingElevation resting depth when unfocused.
 * @param focusedElevation depth (glow spread) when focused.
 */
fun Modifier.tvFocusVisuals(
    focused: Boolean,
    shape: Shape,
    focusedScale: Float = FOCUS_SCALE_CARD,
    glowColor: Color = Color.Unspecified,
    restingElevation: androidx.compose.ui.unit.Dp = Elevation.Level1,
    // Subtle glow per Google's TV focus guidance — scale + border ring are the
    // primary focus cues; the glow is "almost a shadow", not a heavy halo.
    // (Level4/24dp read as a cropped amber band on small cards.)
    focusedElevation: androidx.compose.ui.unit.Dp = Elevation.Level2,
): Modifier = composed {
    val reduceMotion = LocalPerfProfile.current.reduceMotion
    val glow = if (glowColor.isSpecified) glowColor else MaterialTheme.colorScheme.primary

    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = if (reduceMotion) snap() else tween(DURATION_TRANSITION, easing = EaseOutQuart),
        label = "tvFocusScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (focused) focusedElevation else restingElevation,
        animationSpec = if (reduceMotion) snap() else tween(DURATION_FOCUS, easing = EaseOutQuart),
        label = "tvFocusElevation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = if (focused) glow else ShadowColor,
            spotColor = if (focused) glow else ShadowColor,
        )
}
