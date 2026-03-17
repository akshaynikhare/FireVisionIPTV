package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthChecking
import com.cadnative.firevisioniptv.presentation.ui.theme.SubtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.Warning
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onPairDevice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pairing Status — stagger index 0
            PairingStatusCard(
                isPaired = uiState.isPaired,
                tvCode = uiState.tvCode,
                onPairDevice = onPairDevice,
                modifier = Modifier.animateItemEntrance(index = 0)
            )

            // Server Configuration — stagger index 1
            ServerConfigCard(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                settingsSaved = uiState.settingsSaved,
                onServerUrlChange = { viewModel.onServerUrlChange(it) },
                onTvCodeChange = { viewModel.onTvCodeChange(it) },
                onSave = { viewModel.saveServerSettings() },
                modifier = Modifier.animateItemEntrance(index = 1)
            )

            // Account Registration QR — stagger index 2
            uiState.qrCodeBitmap?.let { bitmap ->
                SettingsCard(
                    title = "Account Registration",
                    modifier = Modifier.animateItemEntrance(index = 2)
                ) {
                    Text(
                        text = "Scan with your phone to register",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Registration QR Code",
                            modifier = Modifier.size(160.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Auto-Load Channel — stagger index 3
            SettingsCard(
                title = "Auto-Load Channel",
                modifier = Modifier.animateItemEntrance(index = 3)
            ) {
                Text(
                    text = "Hold OK button on any channel to set it as auto-load on startup",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.autoloadChannelName.isNotEmpty())
                        "Auto-load: ${uiState.autoloadChannelName}"
                    else
                        "No channel set",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (uiState.autoloadChannelName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearAutoloadChannel() },
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear Auto-Load", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Check Liveliness — stagger index 4
            CheckLivelinessCard(
                scanProgress = scanProgress,
                onCheckLiveliness = { viewModel.triggerLivelinessCheck() },
                modifier = Modifier.animateItemEntrance(index = 4)
            )

            // Display — stagger index 5
            SettingsCard(
                title = "Display",
                modifier = Modifier.animateItemEntrance(index = 5)
            ) {
                SettingsRow(
                    title = "Grid Size",
                    subtitle = "Channel grid columns",
                    value = uiState.gridSize.toString()
                )
                SettingsRow(
                    title = "Font Size",
                    subtitle = "Text size scale",
                    value = "${uiState.fontSize}x"
                )
            }

            // About & Update — stagger index 6
            AppUpdateCard(
                appVersion = uiState.appVersion,
                isChecking = uiState.isCheckingForUpdate,
                updateInfo = uiState.updateInfo,
                updateChecked = uiState.updateChecked,
                onCheckForUpdate = { viewModel.checkForUpdate() },
                modifier = Modifier.animateItemEntrance(index = 6)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PairingStatusCard(
    isPaired: Boolean,
    tvCode: String,
    onPairDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isPaired) Success else Warning)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPaired) "Paired" else "Not Paired",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPaired) "TV Code: $tvCode" else "No TV code configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPaired) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isPaired) {
                Button(
                    onClick = onPairDevice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Pair Now", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ServerConfigCard(
    serverUrl: String,
    tvCode: String,
    settingsSaved: Boolean,
    onServerUrlChange: (String) -> Unit,
    onTvCodeChange: (String) -> Unit,
    onSave: () -> Boolean,
    modifier: Modifier = Modifier
) {
    var validationError by remember { mutableStateOf<String?>(null) }

    SettingsCard(title = "Server Configuration", modifier = modifier) {
        Text(
            text = "Server URL",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextField(
            value = serverUrl,
            onValueChange = {
                onServerUrlChange(it)
                validationError = null
            },
            placeholder = { Text("https://tv.cadnative.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "TV Pairing Code",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextField(
            value = tvCode,
            onValueChange = {
                onTvCodeChange(it)
                validationError = null
            },
            placeholder = { Text("Enter TV code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val saved = onSave()
                    if (!saved) {
                        validationError = when {
                            serverUrl.isBlank() || tvCode.isBlank() -> "Server URL and TV code are required"
                            !serverUrl.startsWith("http://") && !serverUrl.startsWith("https://") ->
                                "URL must start with http:// or https://"
                            else -> "Invalid settings"
                        }
                    } else {
                        validationError = null
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Settings", fontWeight = FontWeight.SemiBold)
            }
            // Animated "Saved" confirmation
            AnimatedVisibility(
                visible = settingsSaved,
                enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
                exit = fadeOut(tween(DURATION_NORMAL, easing = EaseOutQuart))
            ) {
                Text(
                    text = "Saved",
                    color = Success,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Animated validation error
        AnimatedVisibility(
            visible = validationError != null,
            enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_NORMAL, easing = EaseOutQuart))
        ) {
            validationError?.let { error ->
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = Warning,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckLivelinessCard(
    scanProgress: ScanProgress,
    onCheckLiveliness: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "Check Liveliness", modifier = modifier) {
        Text(
            text = "Scan all channels to check if their streams are online or offline.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (scanProgress.isScanning) {
            val progress = if (scanProgress.total > 0) {
                scanProgress.scanned.toFloat() / scanProgress.total
            } else 0f

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = HealthChecking,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${scanProgress.scanned}/${scanProgress.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HealthChecking,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scanning channels...",
                style = MaterialTheme.typography.bodySmall,
                color = HealthChecking
            )
        } else {
            if (scanProgress.total > 0) {
                Text(
                    text = "Last scan: ${scanProgress.scanned}/${scanProgress.total} channels checked",
                    style = MaterialTheme.typography.bodySmall,
                    color = Success
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(
                onClick = onCheckLiveliness,
                enabled = !scanProgress.isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Check Liveliness", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AppUpdateCard(
    appVersion: String,
    isChecking: Boolean,
    updateInfo: UpdateInfo?,
    updateChecked: Boolean,
    onCheckForUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "About", modifier = modifier) {
        SettingsRow(
            title = "Version",
            subtitle = appVersion,
            value = ""
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isChecking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Checking for updates...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (updateInfo != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Update available: v${updateInfo.versionName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (updateInfo.releaseNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3
                        )
                    }
                    if (updateInfo.fileSize.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Size: ${updateInfo.fileSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (updateInfo.isMandatory) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This update is mandatory",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Warning
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCheckForUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Check Again", fontWeight = FontWeight.SemiBold)
            }
        } else {
            if (updateChecked) {
                Text(
                    text = "You're on the latest version",
                    style = MaterialTheme.typography.bodySmall,
                    color = Success
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            OutlinedButton(
                onClick = onCheckForUpdate,
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Check for Updates", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
