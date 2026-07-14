package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_TILE
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

/**
 * D-pad-friendly filter chip shared by the Channels category row and the Guide
 * filter bar: amber focus ring + tile scale on focus, category-colored fill when
 * selected.
 */
@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    selectedContainerColor: Color,
    selectedLabelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ShapeSmall
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderStroke: BorderStroke? = when {
        isFocused -> BorderStroke(2.5.dp, FocusBorder)
        !isSelected -> BorderStroke(1.dp, subtleBorder)
        else -> null
    }
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFocused && !isSelected) Amber else Color.Unspecified
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedContainerColor,
            selectedLabelColor = selectedLabelColor,
            containerColor = if (isFocused) Amber.copy(alpha = 0.15f) else Color.Transparent
        ),
        border = borderStroke,
        shape = shape,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .tvFocusVisuals(
                focused = isFocused,
                shape = shape,
                focusedScale = FOCUS_SCALE_TILE,
                // Chips are flat controls — no resting drop shadow (only the focus glow on TV)
                restingElevation = 0.dp
            )
    )
}
