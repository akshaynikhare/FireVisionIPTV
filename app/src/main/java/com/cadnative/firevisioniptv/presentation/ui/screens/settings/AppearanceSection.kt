package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingOption
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingRowLayout
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard

@Composable
internal fun AppearanceSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "Appearance", modifier = modifier) {
        SettingRowLayout(
            text = {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            },
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingOption(label = "Dark",   value = "dark",   current = currentTheme, onSelect = onThemeChange)
                    SettingOption(label = "Light",  value = "light",  current = currentTheme, onSelect = onThemeChange)
                    SettingOption(label = "System", value = "system", current = currentTheme, onSelect = onThemeChange)
                }
            }
        )
    }
}
