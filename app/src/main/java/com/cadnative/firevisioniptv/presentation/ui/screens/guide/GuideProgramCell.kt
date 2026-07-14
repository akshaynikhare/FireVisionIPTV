package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.GuideProgramUiModel
import com.cadnative.firevisioniptv.presentation.ui.components.tvFocusVisuals
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.GuideCellFocusEnd
import com.cadnative.firevisioniptv.presentation.ui.theme.GuideCellFocusStart
import com.cadnative.firevisioniptv.presentation.ui.theme.GuideLiveTintAlpha
import com.cadnative.firevisioniptv.presentation.ui.theme.GuideRowWash
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

/**
 * A single program in a channel row, sized to its duration. Focus is a multi-cue
 * treatment — amber gradient fill + a [Dimens.GuideFocusBorderWidth] Flame border on
 * top of the shared [tvFocusVisuals] glow — so the cell reads as focused from across a
 * room; currently-airing programs carry a subtle category tint. The title is the only
 * text (start time reads from the axis above and the detail panel), and it stays pinned
 * at the visible edge while the cell is partly scrolled off the left.
 *
 * When the row holds focus but this cell doesn't, the cell carries a light amber tint
 * ([rowFocused]) so the whole active row reads at a glance, not just the focused cell.
 */
@Composable
internal fun GuideProgramCell(
    program: GuideProgramUiModel,
    width: Dp,
    laneOffset: Dp,
    scrollState: ScrollState,
    category: String,
    isCompact: Boolean,
    rowFocused: Boolean,
    onFocused: () -> Unit,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    var titleWidthPx by remember { mutableIntStateOf(0) }
    val shape = ShapeMedium
    val glow = categoryColor(category)
    val cellPadding = if (isCompact) Dimens.GuideCellPaddingMobile else Dimens.GuideCellPadding

    val background = when {
        focused -> Brush.horizontalGradient(listOf(GuideCellFocusStart, GuideCellFocusEnd))
        rowFocused -> SolidColor(GuideRowWash)
        program.isLive -> SolidColor(glow.copy(alpha = GuideLiveTintAlpha))
        else -> SolidColor(MaterialTheme.colorScheme.surface)
    }
    val borderWidth = if (focused) Dimens.GuideFocusBorderWidth else 1.dp
    val borderColor = when {
        focused -> FocusBorder
        program.isLive -> glow.copy(alpha = 0.55f)
        else -> subtleBorder
    }

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .padding(end = Dimens.GuideCellGap)
            // No elevation glow: the row clips each cell to its exact height, which would
            // shear a drop-shadow into a hard band. Border + gradient carry the focus cue.
            .tvFocusVisuals(
                focused = focused,
                shape = shape,
                glowColor = glow,
                restingElevation = 0.dp,
                focusedElevation = 0.dp
            )
            .clip(shape)
            .background(background)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelected() }
            .padding(horizontal = cellPadding, vertical = Dimens.Space1),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = program.title,
            style = if (isCompact) MaterialTheme.typography.bodySmall
            else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .onSizeChanged { titleWidthPx = it.width }
                // Keep the title readable while the cell is partly scrolled off-screen left.
                // Read scroll inside the lambda so only placement (not recomposition) runs per frame.
                .offset {
                    val cellStartPx = laneOffset.roundToPx()
                    val innerWidthPx = (width - Dimens.GuideCellGap - cellPadding * 2).roundToPx()
                    val maxShift = (innerWidthPx - titleWidthPx).coerceAtLeast(0)
                    IntOffset(x = (scrollState.value - cellStartPx).coerceIn(0, maxShift), y = 0)
                }
        )
    }
}

/**
 * Placeholder cell across a channel's lane: either the row's schedule hasn't hydrated
 * yet, or the channel has no program data in the window. [label] distinguishes the two.
 */
@Composable
internal fun GuideGapCell(
    width: Dp,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    label: String = "No information"
) {
    val shape = ShapeMedium
    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .padding(end = Dimens.GuideCellGap)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(
                horizontal = if (isCompact) Dimens.GuideCellPaddingMobile else Dimens.GuideCellPadding
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            style = if (isCompact) MaterialTheme.typography.labelSmall
            else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
