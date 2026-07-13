package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Void700
import com.cadnative.firevisioniptv.presentation.ui.theme.Void800
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

private const val MaxRowSkeletonCards = 8
private const val MaxGridSkeletonCards = 12

/**
 * Single shared shimmer brush for a row/grid of skeleton cards.
 * Hoist this once per section and pass it down — never per card.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 1000f, translateAnim - 1000f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Skeleton loading card. Pass a hoisted [shimmerBrush] from [rememberShimmerBrush];
 * null renders a static solid card with zero animation.
 */
@Composable
fun ChannelCardSkeleton(
    width: Dp,
    height: Dp,
    shimmerBrush: Brush? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(width)
            .height(height),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, subtleBorder),
        colors = CardDefaults.cardColors(containerColor = Void800),
    ) {
        if (shimmerBrush != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shimmerBrush)
            )
        }
    }
}

/**
 * Section-title placeholder bar plus a row of skeleton cards, sharing one shimmer.
 */
@Composable
fun ChannelRowSkeleton(
    cardWidth: Dp,
    cardHeight: Dp,
    count: Int = 4,
    showTitle: Boolean = true,
    showShimmer: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Low-end boxes drop the shimmer sweep and rest on static placeholder cards.
    val animate = showShimmer && !LocalPerfProfile.current.reduceMotion
    val brush = if (animate) rememberShimmerBrush() else null

    Column(modifier = modifier) {
        if (showTitle) {
            Box(
                modifier = Modifier
                    .width(Dimens.SkeletonTitleWidth)
                    .height(Dimens.SkeletonTitleHeight)
                    .background(Void700, MaterialTheme.shapes.extraSmall)
            )
            Spacer(modifier = Modifier.height(Dimens.Space3))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)) {
            repeat(count.coerceAtMost(MaxRowSkeletonCards)) {
                ChannelCardSkeleton(
                    width = cardWidth,
                    height = cardHeight,
                    shimmerBrush = brush
                )
            }
        }
    }
}

/**
 * Grid of skeleton cards for the Channels screen, sharing one shimmer.
 */
@Composable
fun ChannelGridSkeleton(
    columns: Int,
    rows: Int,
    cardWidth: Dp,
    cardHeight: Dp,
    showShimmer: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animate = showShimmer && !LocalPerfProfile.current.reduceMotion
    val brush = if (animate) rememberShimmerBrush() else null
    val totalCards = (columns * rows).coerceAtMost(MaxGridSkeletonCards)
    val renderedRows = (totalCards + columns - 1) / columns

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        repeat(renderedRows) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)) {
                val cardsInRow = minOf(columns, totalCards - rowIndex * columns)
                repeat(cardsInRow) {
                    ChannelCardSkeleton(
                        width = cardWidth,
                        height = cardHeight,
                        shimmerBrush = brush
                    )
                }
            }
        }
    }
}
