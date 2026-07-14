package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_TILE
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation

// Netflix-style square A–Z grid: 26 letters + 10 digits fill a 6×6 block, so
// there is no separate numeric layer and every key is a short D-pad hop. A
// 6-column block also stays narrow, leaving the results pane most of the width.
private val KEY_ROWS = listOf(
    "abcdef",
    "ghijkl",
    "mnopqr",
    "stuvwx",
    "yz0123",
    "456789"
)
private const val KEY_COLUMNS = 6

/**
 * An in-app A–Z grid keyboard for TV search — replaces the system IME so the
 * D-pad drives text entry without trapping navigation. Stateless: each key
 * reports the character (or action) to the caller, which owns the query.
 *
 * The first key ('a') carries [firstKeyFocus] so the screen can land focus on
 * the keyboard on entry. Key focus visuals reuse [tvFocusVisuals] and are
 * therefore already `reduceMotion`-gated.
 */
@Composable
fun TvSearchKeyboard(
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    firstKeyFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    // Pin the keyboard to its natural 6-column width so it doesn't stretch to
    // fill the row (the action row's fillMaxWidth would otherwise consume the
    // whole width and leave the results pane with none).
    val keyboardWidth =
        Dimens.KeyboardKeySize * KEY_COLUMNS + Dimens.KeyboardKeyGap * (KEY_COLUMNS - 1)
    Column(
        modifier = modifier.width(keyboardWidth),
        verticalArrangement = Arrangement.spacedBy(Dimens.KeyboardKeyGap)
    ) {
        KEY_ROWS.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.KeyboardKeyGap)) {
                row.forEachIndexed { colIndex, char ->
                    val isFirst = rowIndex == 0 && colIndex == 0
                    KeyButton(
                        onClick = { onKey(char) },
                        modifier = Modifier
                            .size(Dimens.KeyboardKeySize)
                            .then(if (isFirst) Modifier.focusRequester(firstKeyFocus) else Modifier)
                    ) {
                        Text(
                            text = char.uppercase(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.KeyboardKeyGap)
        ) {
            KeyButton(
                onClick = onSpace,
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.KeyboardKeySize)
            ) {
                Icon(imageVector = Icons.Default.SpaceBar, contentDescription = "Space")
            }
            KeyButton(
                onClick = onBackspace,
                modifier = Modifier
                    .widthIn(min = Dimens.KeyboardActionKeyMinWidth)
                    .height(Dimens.KeyboardKeySize)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace"
                )
            }
            KeyButton(
                onClick = onClear,
                modifier = Modifier
                    .widthIn(min = Dimens.KeyboardActionKeyMinWidth)
                    .height(Dimens.KeyboardKeySize)
            ) {
                Text(text = "Clear", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val colorSpec: AnimationSpec<Color> =
        if (LocalPerfProfile.current.reduceMotion) snap()
        else tween(DURATION_FAST, easing = EaseOutQuart)

    val contentColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = colorSpec,
        label = "keyContent"
    )
    val keyColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = colorSpec,
        label = "keyBg"
    )
    val shape = MaterialTheme.shapes.small

    Box(
        modifier = modifier
            .tvFocusVisuals(
                focused = isFocused,
                shape = shape,
                focusedScale = FOCUS_SCALE_TILE,
                restingElevation = Elevation.Level0
            )
            .clip(shape)
            .background(keyColor)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
