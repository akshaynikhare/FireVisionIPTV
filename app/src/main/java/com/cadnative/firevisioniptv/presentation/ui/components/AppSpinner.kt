package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Preset diameters + stroke widths for [AppSpinner], so inline spinners stop
 *  hand-picking size/stroke pairs. */
enum class SpinnerSize(val diameter: Dp, val stroke: Dp) {
    Small(16.dp, 2.dp),   // inline, inside buttons / rows
    Medium(32.dp, 3.dp)   // standalone placeholder (QR loading, etc.)
}

/** The app's circular progress spinner. Color defaults to [MaterialTheme]'s
 *  primary; pass one in for on-colored surfaces (e.g. onPrimary inside a filled
 *  button, or the brand Amber over video). */
@Composable
fun AppSpinner(
    modifier: Modifier = Modifier,
    size: SpinnerSize = SpinnerSize.Small,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier.size(size.diameter),
        strokeWidth = size.stroke,
        color = color
    )
}
