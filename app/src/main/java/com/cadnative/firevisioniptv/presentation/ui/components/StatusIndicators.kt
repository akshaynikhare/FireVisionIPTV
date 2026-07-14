package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.Warning

/** Small filled dot used to flag a banner/state's status. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** Semantic state for [StatusText] — maps to the app's shared feedback colors. */
enum class Status { SUCCESS, WARNING, NEUTRAL }

/**
 * Color-coded feedback text — "Saved", "Connected", "Up to date", error strings.
 * Centralises the Success/Warning/neutral color choice that was repeated inline
 * across settings and source screens.
 */
@Composable
fun StatusText(
    text: String,
    status: Status,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = modifier,
        color = when (status) {
            Status.SUCCESS -> Success
            Status.WARNING -> Warning
            Status.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = style,
        fontWeight = fontWeight
    )
}
