package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall

/**
 * D-pad-friendly single-line selectable row: a checkmark-prefixed label that
 * tints to [accentColor] when [selected] and gets the app's standard focus
 * scale/glow ([tvFocusVisuals]) plus a faint accent fill when focused. Shared by
 * the player track/audio panel and the multiview channel picker; pass
 * [contentColor] for the resting label color (e.g. onVideo over the player).
 */
@Composable
fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    accentColor: Color = Amber
) {
    var isFocused by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            // No elevation shadow: rows sit on scrims/panels where a shadow body
            // reads as a dark box.
            .tvFocusVisuals(
                focused = isFocused,
                shape = ShapeSmall,
                restingElevation = 0.dp,
                focusedElevation = 0.dp
            )
            .clip(ShapeSmall)
            .background(if (isFocused) accentColor.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
    ) {
        Text(
            text = if (selected) "✓  $label" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) accentColor else contentColor
        )
    }
}
