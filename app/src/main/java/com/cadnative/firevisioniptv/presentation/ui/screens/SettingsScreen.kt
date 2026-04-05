package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthChecking
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.Warning
import com.cadnative.firevisioniptv.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onPairDevice: () -> Unit,
    onResetPairing: () -> Unit = {},
    onNavigateToSelfHost: () -> Unit = {},
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Connection status — stagger index 0
            if (uiState.isPaired) {
                PairedStatusBanner(
                    serverUrl = uiState.serverUrl,
                    tvCode = uiState.tvCode,
                    onResetPairing = {
                        viewModel.resetPairing()
                        onResetPairing()
                    },
                    modifier = Modifier.animateItemEntrance(index = 0)
                )
            } else if (uiState.isDefaultMode) {
                DemoModeBanner(
                    serverUrl = uiState.serverUrl,
                    tvCode = uiState.tvCode,
                    onPairDevice = onPairDevice,
                    modifier = Modifier.animateItemEntrance(index = 0)
                )
                SelfHostSetupRow(
                    onSetupServer = onNavigateToSelfHost,
                    modifier = Modifier.animateItemEntrance(index = 1)
                )
            } else {
                UnpairedSetupCard(
                    serverUrl = uiState.serverUrl,
                    tvCode = uiState.tvCode,
                    settingsSaved = uiState.settingsSaved,
                    onServerUrlChange = { viewModel.onServerUrlChange(it) },
                    onTvCodeChange = { viewModel.onTvCodeChange(it) },
                    onSave = { viewModel.saveServerSettings() },
                    onPairDevice = onPairDevice,
                    onTestConnection = { viewModel.testConnection() },
                    isTestingConnection = uiState.isTestingConnection,
                    connectionTestResult = uiState.connectionTestResult,
                    modifier = Modifier.animateItemEntrance(index = 0)
                )
            }

            // Section 2: Channels + About
            // Section 2: Channels + About — always stacked full-width
            ChannelsCard(
                scanProgress = scanProgress,
                onCheckLiveliness = { viewModel.triggerLivelinessCheck() },
                isClearingCache = uiState.isClearingCache,
                cacheCleared = uiState.cacheCleared,
                onClearCache = { viewModel.clearCache() },
                modifier = Modifier.animateItemEntrance(index = 1)
            )

            AboutCard(
                appVersion = uiState.appVersion,
                isChecking = uiState.isCheckingForUpdate,
                updateInfo = uiState.updateInfo,
                updateChecked = uiState.updateChecked,
                isDownloading = uiState.isDownloadingUpdate,
                downloadError = uiState.downloadError,
                onCheckForUpdate = { viewModel.checkForUpdate() },
                onUpdateNow = { viewModel.downloadAndInstallUpdate() },
                modifier = Modifier.animateItemEntrance(index = 2)
            )

            // Section 3: Appearance
            AppearanceCard(
                currentTheme = uiState.theme,
                onThemeChange = { viewModel.setTheme(it) },
                modifier = Modifier.animateItemEntrance(index = 3)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ── Section 1: Pairing ──────────────────────────────────────────────────────

@Composable
private fun PairedStatusBanner(
    serverUrl: String,
    tvCode: String,
    onResetPairing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasFocus by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) FocusBorder.copy(alpha = 0.5f) else Success.copy(alpha = 0.3f),
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "bannerBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (hasFocus) 2.dp else 1.dp,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "bannerBorderWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { hasFocus = it.hasFocus },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Success)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Paired",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$serverUrl  \u00b7  $tvCode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FocusAwareOutlinedButton(
                onClick = onResetPairing,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Text(
                    "Reset",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DemoModeBanner(
    serverUrl: String,
    tvCode: String,
    onPairDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasFocus by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) FocusBorder.copy(alpha = 0.5f) else Warning.copy(alpha = 0.3f),
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "demoBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (hasFocus) 2.dp else 1.dp,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "demoBorderWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { hasFocus = it.hasFocus },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Warning)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Demo Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Browsing shared demo channels. Pair your device to access your personal channel list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$serverUrl  ·  $tvCode",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FocusAwareOutlinedButton(
                onClick = onPairDevice
                ) {
                Text(
                    "Pair Now",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SelfHostSetupRow(
    onSetupServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasFocus by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) FocusBorder.copy(alpha = 0.5f) else subtleBorder,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "selfHostBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (hasFocus) 2.dp else 1.dp,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "selfHostBorderWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { hasFocus = it.hasFocus },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Self-Hosted Server",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Host your own IPTV server and connect this device to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FocusAwareOutlinedButton(onClick = onSetupServer) {
                Text("Setup", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UnpairedSetupCard(
    serverUrl: String,
    tvCode: String,
    settingsSaved: Boolean,
    onServerUrlChange: (String) -> Unit,
    onTvCodeChange: (String) -> Unit,
    onSave: () -> Boolean,
    onPairDevice: () -> Unit,
    onTestConnection: () -> Unit = {},
    isTestingConnection: Boolean = false,
    connectionTestResult: String? = null,
    modifier: Modifier = Modifier
) {
    var validationError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var serverUrlEditing by remember { mutableStateOf(false) }
    var tvCodeEditing by remember { mutableStateOf(false) }

    SettingsCard(title = "Device Setup", modifier = modifier) {
        DeviceSetupFormFields(
            serverUrl = serverUrl,
            tvCode = tvCode,
            settingsSaved = settingsSaved,
            validationError = validationError,
            onServerUrlChange = {
                onServerUrlChange(it)
                validationError = null
            },
            onTvCodeChange = {
                onTvCodeChange(it)
                validationError = null
            },
            onSave = {
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
            onPairDevice = onPairDevice,
            onTestConnection = onTestConnection,
            isTestingConnection = isTestingConnection,
            connectionTestResult = connectionTestResult,
            keyboardController = keyboardController,
            serverUrlEditing = serverUrlEditing,
            onServerUrlEditingChange = { serverUrlEditing = it },
            tvCodeEditing = tvCodeEditing,
            onTvCodeEditingChange = { tvCodeEditing = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSetupFormFields(
    serverUrl: String,
    tvCode: String,
    settingsSaved: Boolean,
    validationError: String?,
    onServerUrlChange: (String) -> Unit,
    onTvCodeChange: (String) -> Unit,
    onSave: () -> Unit,
    onPairDevice: () -> Unit,
    onTestConnection: () -> Unit = {},
    isTestingConnection: Boolean = false,
    connectionTestResult: String? = null,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    serverUrlEditing: Boolean,
    onServerUrlEditingChange: (Boolean) -> Unit,
    tvCodeEditing: Boolean,
    onTvCodeEditingChange: (Boolean) -> Unit
) {
    Text(
        text = "Server URL",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium
    )
    Spacer(modifier = Modifier.height(6.dp))
    TextField(
        value = serverUrl,
        onValueChange = onServerUrlChange,
        placeholder = { Text("https://tv.cadnative.com") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.hasFocus && !serverUrlEditing) keyboardController?.hide()
                if (!it.hasFocus) onServerUrlEditingChange(false)
            }
            .onKeyEvent { event ->
                if (!serverUrlEditing &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onServerUrlEditingChange(true)
                    keyboardController?.show()
                    true
                } else false
            },
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "TV Pairing Code",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium
    )
    Spacer(modifier = Modifier.height(6.dp))
    TextField(
        value = tvCode,
        onValueChange = onTvCodeChange,
        placeholder = { Text("Enter TV code") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.hasFocus && !tvCodeEditing) keyboardController?.hide()
                if (!it.hasFocus) onTvCodeEditingChange(false)
            }
            .onKeyEvent { event ->
                if (!tvCodeEditing &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onTvCodeEditingChange(true)
                    keyboardController?.show()
                    true
                } else false
            },
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FocusAwareButton(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Save Settings", fontWeight = FontWeight.SemiBold)
        }

        FocusAwareOutlinedButton(
            onClick = onPairDevice,
            border = BorderStroke(1.dp, subtleBorder)
        ) {
            Text("Pair with PIN", fontWeight = FontWeight.Medium)
        }

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

    // Test Connection
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FocusAwareOutlinedButton(
            onClick = { if (!isTestingConnection && serverUrl.isNotBlank()) onTestConnection() },
            border = BorderStroke(1.dp, subtleBorder)
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Testing...", fontWeight = FontWeight.Medium)
            } else {
                Text("Test Connection", fontWeight = FontWeight.Medium)
            }
        }
        connectionTestResult?.let { result ->
            Text(
                text = result,
                color = if (result == "Connected") Success else Warning,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Section 2: Two-column cards (Channels + About) ─────────────────────────

@Composable
private fun ChannelsCard(
    scanProgress: ScanProgress,
    onCheckLiveliness: () -> Unit,
    isClearingCache: Boolean,
    cacheCleared: Boolean,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "Channels", modifier = modifier) {
        // Stream Health
        if (scanProgress.isScanning) {
            val progress = if (scanProgress.total > 0) {
                scanProgress.scanned.toFloat() / scanProgress.total
            } else 0f

            Text(
                text = "Scanning channels...",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stream Health",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (scanProgress.total > 0)
                            "Last scan: ${scanProgress.scanned}/${scanProgress.total} checked"
                        else
                            "Scan channels to check if streams are online",
                        color = if (scanProgress.total > 0) Success
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FocusAwareButton(
                    onClick = onCheckLiveliness,
                    enabled = !scanProgress.isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Check Liveliness", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Cache section
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = subtleBorder)
        Spacer(modifier = Modifier.height(10.dp))

        if (isClearingCache) {
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
                    text = "Clearing cache...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local Cache",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AnimatedVisibility(
                        visible = cacheCleared,
                        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
                        exit = fadeOut(tween(DURATION_NORMAL, easing = EaseOutQuart))
                    ) {
                        Text(
                            text = "Cache cleared — refreshing channels",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                    if (!cacheCleared) {
                        Text(
                            text = "Clear cached channels and thumbnails",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (!cacheCleared) {
                    FocusAwareOutlinedButton(
                        onClick = onClearCache,
                    ) {
                        Text(
                            "Clear Cache",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = appVersion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        text = "Checking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (updateInfo != null) {
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
                            if (updateInfo.fileSize.isNotEmpty()) append("  \u00b7  ${updateInfo.fileSize}")
                            if (updateInfo.isMandatory) append("  \u00b7  Mandatory")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isDownloading) {
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
                            text = "Downloading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (updateChecked) "$appVersion  \u00b7  Up to date"
                        else appVersion,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (updateChecked) Success
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FocusAwareOutlinedButton(
                    onClick = onCheckForUpdate,
                    border = BorderStroke(1.dp, subtleBorder)
                ) {
                    Text("Check for Updates", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}   

// ── Section 3: Appearance ───────────────────────────────────────────────────

@Composable
private fun AppearanceCard(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(title = "Appearance", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(label = "Dark",   value = "dark",   currentTheme = currentTheme, onSelect = onThemeChange)
                ThemeOption(label = "Light",  value = "light",  currentTheme = currentTheme, onSelect = onThemeChange)
                ThemeOption(label = "System", value = "system", currentTheme = currentTheme, onSelect = onThemeChange)
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    value: String,
    currentTheme: String,
    onSelect: (String) -> Unit
) {
    val isSelected = currentTheme == value
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> FocusBorder
            isSelected -> MaterialTheme.colorScheme.primary
            else -> subtleBorder
        },
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "themeOptionBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || isSelected) 2.dp else 1.dp,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "themeOptionBorderWidth"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "themeOptionScale"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(8.dp),
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

// ── Shared UI components ────────────────────────────────────────────────────

@Composable
internal fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) FocusBorder.copy(alpha = 0.5f) else subtleBorder,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "cardBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (hasFocus) 2.dp else 1.dp,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "cardBorderWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { hasFocus = it.hasFocus },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
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
            .padding(vertical = 4.dp),
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

// ── Focus-aware button wrappers ─────────────────────────────────────────────

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
        shape = RoundedCornerShape(8.dp),
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
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused },
        content = content
    )
}
