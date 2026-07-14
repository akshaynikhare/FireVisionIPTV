package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeLarge

/** Pill-shaped transient toast used over video and at the app root. */
@Composable
internal fun OverlayToast(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = LabelToast,
        color = OnVideo,
        modifier = modifier
            .clip(ShapeLarge)
            .background(ScrimHeavy)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}
