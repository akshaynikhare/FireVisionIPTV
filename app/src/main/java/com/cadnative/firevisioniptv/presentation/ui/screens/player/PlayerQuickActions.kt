package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_TILE
import com.cadnative.firevisioniptv.presentation.ui.components.tvFocusVisuals
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeLarge
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.softShadow

// Rotating set of sleep-timer presets (minutes). null = off.
private val SLEEP_TIMER_STEPS = listOf<Int?>(null, 30, 60, 90, 120)

/**
 * Live-player quick-actions bar: favorite toggle, sleep-timer cycler, and
 * open-channel-list. Summoned + focused via MENU on TV (see PlayerKeyHandler);
 * ◀▶ move between buttons, OK activates, ▲/BACK/MENU exit back to the player.
 *
 * Live-appropriate only — no scrub/seek/speed. The sleep timer cycles through
 * preset durations (Off → 30 → 60 → 90 → 120 → Off); every action is a single
 * OK press so it stays D-pad friendly.
 */
@Composable
internal fun PlayerQuickActions(
    isFavorite: Boolean,
    sleepTimerMinutes: Int?,
    aspectLabel: String,
    firstActionFocusRequester: FocusRequester,
    onFocusStateChanged: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleSleepTimer: (Int?) -> Unit,
    onCycleAspect: () -> Unit,
    onShowTracks: () -> Unit,
    onShowChannelList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .onFocusChanged { onFocusStateChanged(it.hasFocus) }
            .focusGroup()
            .softShadow(Elevation.Level3, ShapeLarge)
            .clip(ShapeLarge)
            .background(ScrimHeavy)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButton(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            label = if (isFavorite) "Favorited" else "Favorite",
            tint = if (isFavorite) MaterialTheme.colorScheme.error else OnVideo,
            onClick = onToggleFavorite,
            focusRequester = firstActionFocusRequester
        )

        val sleepLabel = sleepTimerMinutes?.let { "Sleep ${it}m" } ?: "Sleep off"
        QuickActionButton(
            icon = Icons.Filled.Bedtime,
            label = sleepLabel,
            tint = if (sleepTimerMinutes != null) Amber else OnVideo,
            onClick = {
                val nextIndex = (SLEEP_TIMER_STEPS.indexOf(sleepTimerMinutes) + 1)
                    .mod(SLEEP_TIMER_STEPS.size)
                onCycleSleepTimer(SLEEP_TIMER_STEPS[nextIndex])
            }
        )

        QuickActionButton(
            icon = Icons.Filled.AspectRatio,
            label = aspectLabel,
            tint = OnVideo,
            onClick = onCycleAspect
        )

        QuickActionButton(
            icon = Icons.Filled.ClosedCaption,
            label = "Audio/Subs",
            tint = OnVideo,
            onClick = onShowTracks
        )

        QuickActionButton(
            icon = Icons.AutoMirrored.Filled.List,
            label = "Channels",
            tint = OnVideo,
            onClick = onShowChannelList
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space1),
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .tvFocusVisuals(
                focused = isFocused,
                shape = ShapeSmall,
                focusedScale = FOCUS_SCALE_TILE
            )
            .clip(ShapeSmall)
            .background(if (isFocused) Amber.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(Dimens.IconLarge)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnVideo
        )
    }
}
