package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingOption
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingRowLayout
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard
import com.cadnative.firevisioniptv.R

@Composable
internal fun AppearanceSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = stringResource(R.string.settings_section_appearance), modifier = modifier) {
        SettingRowLayout(
            text = {
                Text(
                    text = stringResource(R.string.settings_appearance_theme),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            },
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingOption(
                        label = stringResource(R.string.settings_theme_dark),
                        value = "dark",
                        current = currentTheme,
                        onSelect = onThemeChange
                    )
                    SettingOption(
                        label = stringResource(R.string.settings_theme_light),
                        value = "light",
                        current = currentTheme,
                        onSelect = onThemeChange
                    )
                    SettingOption(
                        label = stringResource(R.string.settings_theme_system),
                        value = "system",
                        current = currentTheme,
                        onSelect = onThemeChange
                    )
                }
            }
        )
    }
}
