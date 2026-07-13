package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Corner token scale — YouTube-TV flat style, 3 steps ──────────────
val ShapeSmall  = RoundedCornerShape(8.dp)    // chips, badges, small buttons
val ShapeMedium = RoundedCornerShape(12.dp)   // cards, panels
val ShapeLarge  = RoundedCornerShape(20.dp)   // toasts/pills, dialogs

// ── Material 3 shape mapping ─────────────────────────────────────────
val FireVisionShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeLarge,
    extraLarge = RoundedCornerShape(28.dp)
)
