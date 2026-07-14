package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingOption
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingRowLayout
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard

private val keyActionOptions = listOf(
    "Zap" to PlayerKeyAction.ZAP,
    "Last Ch" to PlayerKeyAction.LAST_CHANNEL,
    "Favorite" to PlayerKeyAction.FAVORITE,
    "Play/Pause" to PlayerKeyAction.PLAY_PAUSE,
    "Menu" to PlayerKeyAction.MENU
)

private val sleepTimerOptions = listOf(
    "Off" to "0",
    "30 min" to "30",
    "1 h" to "60",
    "2 h" to "120",
    "3 h" to "180"
)

private val infoBarTimeoutOptions = listOf(
    "4 s" to "4",
    "6 s" to "6",
    "8 s" to "8",
    "10 s" to "10"
)

@Composable
internal fun ControlsSection(
    backExitProtection: Boolean,
    keyUpDownAction: String,
    keyLeftRightAction: String,
    longOkAction: String,
    sleepTimerDefaultMinutes: Int,
    alwaysShowProgramBar: Boolean,
    infoBarTimeoutSeconds: Int,
    onBackExitProtectionChange: (Boolean) -> Unit,
    onKeyUpDownChange: (String) -> Unit,
    onKeyLeftRightChange: (String) -> Unit,
    onLongOkChange: (String) -> Unit,
    onSleepTimerDefaultChange: (Int) -> Unit,
    onAlwaysShowProgramBarChange: (Boolean) -> Unit,
    onInfoBarTimeoutChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    SettingsCard(title = "Player Controls", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 10.dp)) {
            PlayerKeyRow(
                label = "Confirm app exit (press back twice on Home)",
                options = listOf("On" to "on", "Off" to "off"),
                current = if (backExitProtection) "on" else "off",
                onSelect = { onBackExitProtectionChange(it == "on") }
            )
            PlayerKeyRow(
                label = "Always show program bar",
                options = listOf("On" to "on", "Off" to "off"),
                current = if (alwaysShowProgramBar) "on" else "off",
                onSelect = { onAlwaysShowProgramBarChange(it == "on") }
            )
            PlayerKeyRow(
                label = "Info banner timeout",
                options = infoBarTimeoutOptions,
                current = infoBarTimeoutSeconds.toString(),
                onSelect = { onInfoBarTimeoutChange(it.toIntOrNull() ?: 4) }
            )
            // D-pad / remote key mappings only make sense on TV (no D-pad on a touch phone)
            if (!isCompact) {
                // Five key-action pills fill the row, so keep the label on its own
                // line above them — side-by-side starves the label to a 1-char column.
                PlayerKeyRow(
                    label = "D-pad up/down",
                    options = keyActionOptions,
                    current = keyUpDownAction,
                    onSelect = onKeyUpDownChange,
                    stacked = true
                )
                PlayerKeyRow(
                    label = "D-pad left/right",
                    options = keyActionOptions,
                    current = keyLeftRightAction,
                    onSelect = onKeyLeftRightChange,
                    stacked = true
                )
                PlayerKeyRow(
                    label = "Hold OK",
                    options = keyActionOptions,
                    current = longOkAction,
                    onSelect = onLongOkChange,
                    stacked = true
                )
            }
            PlayerKeyRow(
                label = "Sleep timer",
                options = sleepTimerOptions,
                current = sleepTimerDefaultMinutes.toString(),
                onSelect = { onSleepTimerDefaultChange(it.toIntOrNull() ?: 0) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerKeyRow(
    label: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit,
    stacked: Boolean = false
) {
    val labelText: @Composable () -> Unit = {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
    // FlowRow so option pills wrap to a new line instead of overflowing on narrow rows
    val optionPills: @Composable () -> Unit = {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (optLabel, optValue) ->
                SettingOption(label = optLabel, value = optValue, current = current, onSelect = onSelect)
            }
        }
    }

    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labelText()
            optionPills()
        }
    } else {
        SettingRowLayout(text = { labelText() }, action = optionPills)
    }
}
