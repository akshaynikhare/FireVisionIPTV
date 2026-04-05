package com.cadnative.firevisioniptv.presentation.ui.screens

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

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
            // Section 1: Pairing (state-dependent) — stagger index 0
            if (uiState.isPaired) {
                PairedStatusBanner(
                    tvCode = uiState.tvCode,
                    onResetPairing = {
                        viewModel.resetPairing()
                        onResetPairing()
                    },
                    modifier = Modifier.animateItemEntrance(index = 0)
                )
            } else {
                UnpairedSetupCard(
                    serverUrl = uiState.serverUrl,
                    tvCode = uiState.tvCode,
                    settingsSaved = uiState.settingsSaved,
                    qrCodeBitmap = uiState.qrCodeBitmap,
                    isPortrait = isPortrait,
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
            if (isPortrait) {
                // Portrait: stack vertically
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
            } else {
                // Landscape: side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ChannelsCard(
                        scanProgress = scanProgress,
                        onCheckLiveliness = { viewModel.triggerLivelinessCheck() },
                        isClearingCache = uiState.isClearingCache,
                        cacheCleared = uiState.cacheCleared,
                        onClearCache = { viewModel.clearCache() },
                        modifier = Modifier
                            .weight(1f)
                            .animateItemEntrance(index = 1)
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
                        modifier = Modifier
                            .weight(1f)
                            .animateItemEntrance(index = 2)
                    )
                }
            }

            // Section 3: Appearance
            AppearanceCard(
                currentTheme = uiState.theme,
                onThemeChange = { viewModel.setTheme(it) },
                modifier = Modifier.animateItemEntrance(index = 3)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ── Section 1: Pairing ──────────────────────────────────────────────────────

@Composable
private fun PairedStatusBanner(
    tvCode: String,
    onResetPairing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasFocus by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) FocusBorder.copy(alpha = 0.5f) else subtleBorder,
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Success)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Paired",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "TV Code: $tvCode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            FocusAwareOutlinedButton(
                onClick = onResetPairing,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Text(
                    "Reset Pairing",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun UnpairedSetupCard(
    serverUrl: String,
    tvCode: String,
    settingsSaved: Boolean,
    qrCodeBitmap: Bitmap?,
    isPortrait: Boolean,
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
        if (isPortrait) {
            // Portrait: stack vertically — form fields then QR
            Column(modifier = Modifier.fillMaxWidth()) {
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

                if (qrCodeBitmap != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    QrCodeSection(qrCodeBitmap = qrCodeBitmap)
                }
            }
        } else {
            // Landscape: side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                if (qrCodeBitmap != null) {
                    QrCodeSection(qrCodeBitmap = qrCodeBitmap, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
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

    Spacer(modifier = Modifier.height(14.dp))

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

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

@Composable
private fun QrCodeSection(
    qrCodeBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text(
            text = "Scan to pair",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Image(
                bitmap = qrCodeBitmap.asImageBitmap(),
                contentDescription = "Pairing QR Code",
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "or enter PIN on dashboard",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
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
        Text(
            text = "Stream Health",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
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

        // Cache section
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = subtleBorder)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Local Cache",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Clear cached channels and thumbnails. Channels will be re-fetched from the server.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

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
                FocusAwareOutlinedButton(
                    onClick = onClearCache,
                    border = BorderStroke(1.dp, Warning.copy(alpha = 0.5f))
                ) {
                    Text(
                        "Clear Local Cache",
                        fontWeight = FontWeight.SemiBold,
                        color = Warning
                    )
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
        SettingsRow(title = "Version", subtitle = appVersion, value = "")

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

            if (isDownloading) {
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
                        text = "Downloading update...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (updateInfo.downloadUrl.isNotEmpty()) {
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Warning,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
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
            FocusAwareOutlinedButton(
                onClick = onCheckForUpdate,
                border = BorderStroke(1.dp, subtleBorder)
            ) {
                Text("Check for Updates", fontWeight = FontWeight.Medium)
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
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeOption(label = "Dark",   value = "dark",   currentTheme = currentTheme, onSelect = onThemeChange)
            ThemeOption(label = "Light",  value = "light",  currentTheme = currentTheme, onSelect = onThemeChange)
            ThemeOption(label = "System", value = "system", currentTheme = currentTheme, onSelect = onThemeChange)
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
private fun SettingsCard(
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

// ── Focus-aware button wrappers ─────────────────────────────────────────────

@Composable
private fun FocusAwareButton(
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
private fun FocusAwareOutlinedButton(
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
