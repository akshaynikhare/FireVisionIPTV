// Package intentionally stays `presentation.ui.screens` (not `.settings`):
// AddSourceScreen.kt uses these components via same-package resolution.
package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

@Composable
internal fun SettingsCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Static, flat card — no focus highlight and no drop shadow. The card's inner
    // controls (buttons, options) show their own focus; a card-level glow/shadow
    // just doubles the highlight and reads as noisy. A null [title] renders a
    // header-less card when the body already carries enough context.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, subtleBorder)
    ) {
        Column(modifier = Modifier.padding(Dimens.Space3)) {
            if (title != null) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Dimens.Space3))
            }
            content()
        }
    }
}

@Composable
internal fun FocusAwareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = ShapeSmall,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused },
        content = content
    )
}

@Composable
internal fun FocusAwareOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, subtleBorder),
    content: @Composable RowScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "outlinedBtnScale"
    )
    val animatedBorder = if (isFocused) {
        BorderStroke(2.dp, FocusBorder.copy(alpha = 0.5f))
    } else {
        border
    }

    OutlinedButton(
        onClick = onClick,
        border = animatedBorder,
        shape = ShapeSmall,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused },
        content = content
    )
}

/**
 * A settings row that pairs a descriptive [text] block with a trailing [action]
 * (button / options). On TV/landscape they sit side-by-side; on compact/portrait
 * phones the action stacks full-width below the text for readable, tappable rows.
 */
@Composable
internal fun SettingRowLayout(
    text: @Composable ColumnScope.() -> Unit,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    if (isCompact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Column(content = text)
            action()
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), content = text)
            action()
        }
    }
}

/** Pill-style selectable option used by Appearance and Controls sections. */
@Composable
internal fun SettingOption(
    label: String,
    value: String,
    current: String,
    onSelect: (String) -> Unit
) {
    val isSelected = current == value
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> FocusBorder
            isSelected -> MaterialTheme.colorScheme.primary
            else -> subtleBorder
        },
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "optionBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || isSelected) 2.dp else 1.dp,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "optionBorderWidth"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "optionScale"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused },
        shape = ShapeSmall,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor),
        onClick = { onSelect(value) }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
