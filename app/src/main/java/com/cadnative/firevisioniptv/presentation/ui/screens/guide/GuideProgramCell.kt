package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.GuideProgramUiModel
import com.cadnative.firevisioniptv.presentation.ui.components.tvFocusVisuals
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

/**
 * A single program in a channel row, sized to its duration. Applies the shared
 * [tvFocusVisuals] treatment; Center invokes [onSelected] (which tunes the channel).
 * The category color is used as the focus glow so each genre reads distinctly.
 */
@Composable
internal fun GuideProgramCell(
    program: GuideProgramUiModel,
    width: Dp,
    category: String,
    onFocused: () -> Unit,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val glow = categoryColor(category)
    val accent = if (program.isLive) glow else Color.Unspecified

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .padding(end = Dimens.GuideCellGap)
            .tvFocusVisuals(focused = focused, shape = shape, glowColor = glow)
            .clip(shape)
            .background(
                if (focused) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (program.isLive) glow.copy(alpha = 0.55f) else subtleBorder,
                shape = shape
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelected() }
            .padding(horizontal = Dimens.GuideCellPadding, vertical = Dimens.Space2),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatSlotLabel(program.startTime),
                style = MaterialTheme.typography.labelMedium,
                color = if (accent != Color.Unspecified) accent
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Placeholder cell rendered where a channel has no program in a stretch of the window. */
@Composable
internal fun GuideGapCell(
    width: Dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .padding(end = Dimens.GuideCellGap)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(horizontal = Dimens.GuideCellPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "No information",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
