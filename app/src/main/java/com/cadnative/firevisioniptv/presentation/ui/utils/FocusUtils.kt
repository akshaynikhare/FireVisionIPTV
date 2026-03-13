package com.cadnative.firevisioniptv.presentation.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged

/**
 * Focus management utilities for Android TV.
 */

/**
 * Applies a scale animation when the component gains focus.
 * 
 * @param focusedScale The scale factor when focused (default 1.1f)
 * @param unfocusedScale The scale factor when unfocused (default 1.0f)
 * @param animationDuration The duration of the animation in milliseconds (default 200ms)
 */
fun Modifier.focusScale(
    focusedScale: Float = 1.1f,
    unfocusedScale: Float = 1.0f,
    animationDuration: Int = 200
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else unfocusedScale,
        animationSpec = tween(durationMillis = animationDuration),
        label = "focusScale"
    )

    this
        .scale(scale)
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
}

/**
 * Returns whether the component is currently focused.
 */
@Composable
fun rememberFocusState(): MutableState<Boolean> {
    return remember { mutableStateOf(false) }
}

/**
 * Modifier that tracks focus state.
 */
fun Modifier.trackFocus(focusState: MutableState<Boolean>): Modifier = composed {
    this.onFocusChanged { focusState.value = it.isFocused }
}
