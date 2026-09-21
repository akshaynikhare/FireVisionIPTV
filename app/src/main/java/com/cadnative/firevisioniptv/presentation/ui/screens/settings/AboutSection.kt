package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.presentation.ui.components.AppSpinner
import com.cadnative.firevisioniptv.presentation.ui.components.Status
import com.cadnative.firevisioniptv.presentation.ui.components.StatusText
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareOutlinedButton
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingRowLayout
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard
import com.cadnative.firevisioniptv.R

@Composable
internal fun AboutSection(
    appVersion: String,
    isChecking: Boolean,
    updateInfo: UpdateInfo?,
    updateChecked: Boolean,
    isDownloading: Boolean,
    downloadError: String?,
    onCheckForUpdate: () -> Unit,
    onUpdateNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = isChecking || isDownloading
    val canInstall = updateInfo != null && updateInfo.downloadUrl.isNotEmpty()

    SettingsCard(title = stringResource(R.string.settings_section_about), modifier = modifier) {
        SettingRowLayout(
            text = {
                if (updateInfo != null) {
                    UpdateAvailableLabel(appVersion = appVersion, updateInfo = updateInfo)
                } else {
                    VersionLabel(appVersion = appVersion, upToDate = updateChecked && !isChecking)
                }
            },
            action = {
                // One persistent button across all states: replacing the focused
                // node mid-action drops TV focus back to the section rail's first
                // item, snapping the settings pane to Connection.
                FocusAwareOutlinedButton(
                    onClick = {
                        if (!busy) {
                            if (canInstall) onUpdateNow() else onCheckForUpdate()
                        }
                    }
                ) {
                    if (busy) {
                        AppSpinner()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            isChecking -> stringResource(R.string.settings_about_checking)
                            isDownloading -> stringResource(R.string.settings_about_downloading)
                            canInstall -> stringResource(R.string.settings_about_install)
                            else -> stringResource(R.string.settings_about_check)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
        downloadError?.let { error ->
            Spacer(modifier = Modifier.height(6.dp))
            StatusText(text = error, status = Status.WARNING, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun VersionLabel(
    appVersion: String,
    upToDate: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_about_version),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        StatusText(
            text = if (upToDate) {
                stringResource(R.string.settings_about_up_to_date, appVersion)
            } else {
                appVersion
            },
            status = if (upToDate) Status.SUCCESS else Status.NEUTRAL
        )
    }
}

@Composable
private fun UpdateAvailableLabel(
    appVersion: String,
    updateInfo: UpdateInfo,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_about_update_available, updateInfo.versionName),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = buildString {
                append(stringResource(R.string.settings_about_current, appVersion))
                if (updateInfo.fileSize.isNotEmpty()) {
                    append(stringResource(R.string.update_overlay_detail, updateInfo.fileSize))
                }
                if (updateInfo.isMandatory) {
                    append(stringResource(R.string.update_overlay_mandatory))
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
