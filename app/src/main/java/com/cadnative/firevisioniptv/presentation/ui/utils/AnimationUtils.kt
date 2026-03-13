package com.cadnative.firevisioniptv.presentation.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset

/**
 * Animation utilities for Android TV UI.
 */

/**
 * Fade-in animation with 300ms duration.
 */
@Composable
fun fadeInAnimation(): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
    )
}

/**
 * Fade-out animation with 300ms duration.
 */
@Composable
fun fadeOutAnimation(): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
    )
}

/**
 * Scale animation for focus with 200ms duration.
 */
fun scaleAnimation(
    initialScale: Float = 0.8f,
    targetScale: Float = 1.0f,
    durationMillis: Int = 200
): EnterTransition {
    return scaleIn(
        initialScale = initialScale,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
    )
}

/**
 * Slide animation for navigation with 400ms duration.
 */
fun slideInAnimation(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )
}

/**
 * Slide out animation for navigation with 400ms duration.
 */
fun slideOutAnimation(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )
}

/**
 * Shimmer effect modifier for loading states.
 */
fun Modifier.shimmerEffect(
    colors: List<Color> = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    this.drawWithContent {
        drawContent()
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(translateAnim.value - 1000f, translateAnim.value - 1000f),
            end = Offset(translateAnim.value, translateAnim.value)
        )
        drawRect(brush = brush)
    }
}
