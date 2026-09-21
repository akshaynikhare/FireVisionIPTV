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
import androidx.compose.ui.res.stringResource
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingOption
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingRowLayout
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard
import com.cadnative.firevisioniptv.R

// Labels are resource ids rather than strings so these can stay top-level vals.
// The player's key-hint strip resolves the same family, so the two cannot drift.
private val keyActionOptions = listOf(
    R.string.player_key_action_zap to PlayerKeyAction.ZAP,
    R.string.player_key_action_last_channel to PlayerKeyAction.LAST_CHANNEL,
    R.string.player_key_action_favorite to PlayerKeyAction.FAVORITE,
    R.string.player_key_action_play_pause to PlayerKeyAction.PLAY_PAUSE,
    R.string.player_key_action_controls to PlayerKeyAction.MENU
)

private val sleepTimerOptions = listOf(
    R.string.settings_sleep_off to "0",
    R.string.settings_sleep_30m to "30",
    R.string.settings_sleep_1h to "60",
    R.string.settings_sleep_2h to "120",
    R.string.settings_sleep_3h to "180"
)

private val infoBarTimeoutOptions = listOf(
    R.string.settings_timeout_4s to "4",
    R.string.settings_timeout_6s to "6",
    R.string.settings_timeout_8s to "8",
    R.string.settings_timeout_10s to "10"
)

private val toggleOptions = listOf(
    R.string.settings_toggle_on to "on",
    R.string.settings_toggle_off to "off"
)

@Composable
internal fun ControlsSection(
    backExitProtection: Boolean,
    keyUpDownAction: String,
    keyLeftRightAction: String,
    sleepTimerDefaultMinutes: Int,
    alwaysShowProgramBar: Boolean,
    infoBarTimeoutSeconds: Int,
    onBackExitProtectionChange: (Boolean) -> Unit,
    onKeyUpDownChange: (String) -> Unit,
    onKeyLeftRightChange: (String) -> Unit,
    onSleepTimerDefaultChange: (Int) -> Unit,
    onAlwaysShowProgramBarChange: (Boolean) -> Unit,
    onInfoBarTimeoutChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    SettingsCard(title = stringResource(R.string.settings_controls_title), modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 10.dp)) {
            PlayerKeyRow(
                label = stringResource(R.string.settings_controls_back_protection),
                options = toggleOptions,
                current = if (backExitProtection) "on" else "off",
                onSelect = { onBackExitProtectionChange(it == "on") }
            )
            PlayerKeyRow(
                label = stringResource(R.string.settings_controls_always_show_bar),
                options = toggleOptions,
                current = if (alwaysShowProgramBar) "on" else "off",
                onSelect = { onAlwaysShowProgramBarChange(it == "on") }
            )
            PlayerKeyRow(
                label = stringResource(R.string.settings_controls_info_timeout),
                options = infoBarTimeoutOptions,
                current = infoBarTimeoutSeconds.toString(),
                onSelect = { onInfoBarTimeoutChange(it.toIntOrNull() ?: 4) }
            )
            // D-pad / remote key mappings only make sense on TV (no D-pad on a touch phone)
            if (!isCompact) {
                // Five key-action pills fill the row, so keep the label on its own
                // line above them — side-by-side starves the label to a 1-char column.
                PlayerKeyRow(
                    label = stringResource(R.string.settings_controls_dpad_up_down),
                    options = keyActionOptions,
                    current = keyUpDownAction,
                    onSelect = onKeyUpDownChange,
                    stacked = true
                )
                PlayerKeyRow(
                    label = stringResource(R.string.settings_controls_dpad_left_right),
                    options = keyActionOptions,
                    current = keyLeftRightAction,
                    onSelect = onKeyLeftRightChange,
                    stacked = true
                )
            }
            PlayerKeyRow(
                label = stringResource(R.string.settings_controls_sleep_timer),
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
    options: List<Pair<Int, String>>,
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
                SettingOption(label = stringResource(optLabel), value = optValue, current = current, onSelect = onSelect)
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
