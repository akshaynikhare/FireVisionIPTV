package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareButton
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareOutlinedButton
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.Warning

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
    SettingsCard(title = "About", modifier = modifier) {
        if (isChecking) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VersionLabel(appVersion = appVersion, upToDate = false, modifier = Modifier.weight(1f))
                ProgressLabel(text = "Checking...")
            }
        } else if (updateInfo != null) {
            UpdateAvailableRow(
                appVersion = appVersion,
                updateInfo = updateInfo,
                isDownloading = isDownloading,
                onUpdateNow = onUpdateNow
            )
            downloadError?.let { error ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    color = Warning,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VersionLabel(appVersion = appVersion, upToDate = updateChecked, modifier = Modifier.weight(1f))
                FocusAwareOutlinedButton(onClick = onCheckForUpdate) {
                    Text("Check for Updates", fontWeight = FontWeight.Medium)
                }
            }
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
            text = "Version",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (upToDate) "$appVersion  ·  Up to date" else appVersion,
            style = MaterialTheme.typography.bodySmall,
            color = if (upToDate) Success else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UpdateAvailableRow(
    appVersion: String,
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    onUpdateNow: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Update available: v${updateInfo.versionName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    append("Current: $appVersion")
                    if (updateInfo.fileSize.isNotEmpty()) append("  ·  ${updateInfo.fileSize}")
                    if (updateInfo.isMandatory) append("  ·  Mandatory")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isDownloading) {
            ProgressLabel(text = "Downloading...")
        } else if (updateInfo.downloadUrl.isNotEmpty()) {
            FocusAwareButton(
                onClick = onUpdateNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Update Now", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
