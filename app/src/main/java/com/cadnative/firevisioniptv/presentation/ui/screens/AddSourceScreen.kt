package com.cadnative.firevisioniptv.presentation.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.components.AppSpinner
import com.cadnative.firevisioniptv.presentation.ui.components.AppTextField
import com.cadnative.firevisioniptv.presentation.ui.components.Status
import com.cadnative.firevisioniptv.presentation.ui.components.StatusText
import com.cadnative.firevisioniptv.presentation.ui.components.ThemeAwareQrCode
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder
import com.cadnative.firevisioniptv.presentation.viewmodel.SettingsViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private const val SERVER_GUIDE_URL = "https://github.com/akshaynikhare/FireVisionIPTVServer/blob/main/docs/workflow/SELF_HOSTING_GUIDE.md"

private enum class SourceTab(val label: String) {
    SELF_HOST("Self-hosted server"),
    M3U("M3U"),
    XTREAM("Xtream")
}

/**
 * Unified "Add a different source" screen — the single Advanced entry reached from
 * both onboarding (Pairing) and Settings → Connection. A segmented toggle picks one
 * of three bring-your-own sources: a self-hosted FireVision server (URL + pairing
 * code), an M3U playlist URL, or Xtream Codes login. Replaces the former
 * SelfHostSetupScreen, which conflated all three on one scrolling page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(
    onNavigateBack: () -> Unit,
    onPairDevice: () -> Unit,
    modifier: Modifier = Modifier,
    onPlaylistLoaded: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Advance to Home once a bring-your-own playlist loads successfully.
    LaunchedEffect(uiState.playlistResult) {
        if (uiState.playlistResult == "Playlist loaded") onPlaylistLoaded()
    }

    // Preselect the tab for the source that's currently in use, so opening this
    // screen from "Change source" reflects the active config instead of resetting.
    var tab by rememberSaveable {
        mutableStateOf(
            when (uiState.sourceType) {
                "m3u" -> SourceTab.M3U
                "xtream" -> SourceTab.XTREAM
                else -> SourceTab.SELF_HOST
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = if (isPortrait) Dimens.Space4 else Dimens.Space5,
                vertical = if (isPortrait) Dimens.Space2 else Dimens.Space4
            )
    ) {
        // Back + title, with the source toggle on the same row (landscape) so the
        // content below gets the full remaining height instead of a separate row.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isPortrait) 8.dp else 16.dp)
        ) {
            var backFocused by remember { mutableStateOf(false) }
            val backScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (backFocused) 1.1f else 1f,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "backScale"
            )
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .graphicsLayer { scaleX = backScale; scaleY = backScale }
                    .onFocusChanged { backFocused = it.isFocused }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add a different source",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isPortrait) {
                Spacer(modifier = Modifier.weight(1f))
                SourceTabSelector(selected = tab, onSelect = { tab = it })
            }
        }

        if (isPortrait) {
            SourceTabSelector(selected = tab, onSelect = { tab = it })
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (tab) {
            SourceTab.SELF_HOST -> SelfHostContent(
                uiState = uiState,
                viewModel = viewModel,
                onPairDevice = onPairDevice,
                isPortrait = isPortrait
            )
            SourceTab.M3U -> M3uCard(
                initialUrl = uiState.m3uUrl,
                isLoading = uiState.isLoadingPlaylist,
                result = uiState.playlistResult,
                onSave = { url -> viewModel.saveM3uPlaylist(url) },
                modifier = Modifier.fillMaxWidth()
            )
            SourceTab.XTREAM -> XtreamCard(
                initialHost = uiState.xtreamHost,
                isLoading = uiState.isLoadingPlaylist,
                result = uiState.playlistResult,
                onSave = { host, user, pass -> viewModel.saveXtreamPlaylist(host, user, pass) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SourceTabSelector(
    selected: SourceTab,
    onSelect: (SourceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SourceTab.entries.forEach { tab ->
            val active = tab == selected
            // Active state uses the app's amber primary — same family as the D-pad
            // focus highlight — so "selected" and "focused" don't read as two themes.
            val activeColor = MaterialTheme.colorScheme.primary
            FocusAwareOutlinedButton(
                onClick = { onSelect(tab) },
                border = BorderStroke(1.dp, if (active) activeColor else subtleBorder)
            ) {
                Text(
                    text = tab.label,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) activeColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SelfHostContent(
    uiState: com.cadnative.firevisioniptv.presentation.model.SettingsUiState,
    viewModel: SettingsViewModel,
    onPairDevice: () -> Unit,
    isPortrait: Boolean
) {
    val guideQrBitmap = remember(isPortrait) {
        if (!isPortrait) generateQrBitmap(SERVER_GUIDE_URL) else null
    }

    if (isPortrait) {
        SetupGuideCardPortrait()
        Spacer(modifier = Modifier.height(16.dp))
        ServerConfigCard(
            serverUrl = uiState.serverUrl,
            tvCode = uiState.tvCode,
            settingsSaved = uiState.settingsSaved,
            isTestingConnection = uiState.isTestingConnection,
            connectionTestResult = uiState.connectionTestResult,
            onServerUrlChange = { viewModel.onServerUrlChange(it) },
            onTvCodeChange = { viewModel.onTvCodeChange(it) },
            onSave = { viewModel.saveServerSettings() },
            onPairDevice = onPairDevice,
            onTestConnection = { viewModel.testConnection() },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(title = null, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = "Scan to view the server setup guide, or visit:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SERVER_GUIDE_URL,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                guideQrBitmap?.let { bitmap ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ThemeAwareQrCode(
                            bitmap = bitmap,
                            contentDescription = "Server setup guide QR code",
                            size = 180.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SetupStep(number = "1", text = "Deploy FireVision IPTV Server on your machine")
                Spacer(modifier = Modifier.height(4.dp))
                SetupStep(number = "2", text = "Add your IPTV channels via the web dashboard")
                Spacer(modifier = Modifier.height(4.dp))
                SetupStep(number = "3", text = "Enter your server URL and pairing code here")
            }

            ServerConfigCard(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                settingsSaved = uiState.settingsSaved,
                isTestingConnection = uiState.isTestingConnection,
                connectionTestResult = uiState.connectionTestResult,
                onServerUrlChange = { viewModel.onServerUrlChange(it) },
                onTvCodeChange = { viewModel.onTvCodeChange(it) },
                onSave = { viewModel.saveServerSettings() },
                onPairDevice = onPairDevice,
                onTestConnection = { viewModel.testConnection() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun M3uCard(
    initialUrl: String,
    isLoading: Boolean,
    result: String?,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pre-fill with the currently saved URL so editing an active source starts
    // from its real value instead of a blank field.
    var m3uUrl by remember { mutableStateOf(initialUrl) }
    SettingsCard(title = "M3U Playlist", modifier = modifier) {
        Text(
            text = "Paste an M3U / M3U8 playlist URL — no FireVision server required.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppTextField(value = m3uUrl, onValueChange = { m3uUrl = it }, placeholder = "https://example.com/playlist.m3u", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        LoadPlaylistRow(isLoading = isLoading, result = result, onClick = { onSave(m3uUrl) })
    }
}

@Composable
private fun XtreamCard(
    initialHost: String,
    isLoading: Boolean,
    result: String?,
    onSave: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pre-fill the host from the saved source; credentials stay blank (they live
    // in encrypted storage and aren't surfaced here).
    var host by remember { mutableStateOf(initialHost) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    SettingsCard(title = "Xtream Codes", modifier = modifier) {
        Text(
            text = "Enter your Xtream Codes login — no FireVision server required.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppTextField(value = host, onValueChange = { host = it }, placeholder = "http://server:port", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        AppTextField(value = username, onValueChange = { username = it }, placeholder = "Username", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        AppTextField(value = password, onValueChange = { password = it }, placeholder = "Password", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        LoadPlaylistRow(isLoading = isLoading, result = result, onClick = { onSave(host, username, password) })
    }
}

@Composable
private fun LoadPlaylistRow(
    isLoading: Boolean,
    result: String?,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FocusAwareButton(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Load Playlist", fontWeight = FontWeight.SemiBold)
        }
        if (isLoading) {
            AppSpinner()
        }
        result?.let {
            StatusText(
                text = it,
                status = if (it == "Playlist loaded") Status.SUCCESS else Status.WARNING,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SetupGuideCardPortrait() {
    val context = LocalContext.current
    SettingsCard(title = null, modifier = Modifier.fillMaxWidth()) {
        SetupStep(number = "1", text = "Deploy FireVision IPTV Server on your machine")
        Spacer(modifier = Modifier.height(6.dp))
        SetupStep(number = "2", text = "Add your IPTV channels via the web dashboard")
        Spacer(modifier = Modifier.height(6.dp))
        SetupStep(number = "3", text = "Enter your server URL and pairing code below")
        Spacer(modifier = Modifier.height(14.dp))
        FocusAwareOutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SERVER_GUIDE_URL))
                context.startActivity(intent)
            },
            border = BorderStroke(1.dp, SteelBlue.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SteelBlue
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "View Setup Guide", fontWeight = FontWeight.Medium, color = SteelBlue)
        }
    }
}

@Composable
private fun SetupStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = ShapeSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerConfigCard(
    serverUrl: String,
    tvCode: String,
    settingsSaved: Boolean,
    isTestingConnection: Boolean,
    connectionTestResult: String?,
    onServerUrlChange: (String) -> Unit,
    onTvCodeChange: (String) -> Unit,
    onSave: () -> Boolean,
    onPairDevice: () -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var validationError by remember { mutableStateOf<String?>(null) }

    SettingsCard(title = null, modifier = modifier) {
        Text(
            text = "Server URL",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        AppTextField(
            value = serverUrl,
            onValueChange = { onServerUrlChange(it); validationError = null },
            placeholder = "https://your-server.com",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "TV Pairing Code",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        AppTextField(
            value = tvCode,
            onValueChange = { onTvCodeChange(it); validationError = null },
            placeholder = "Enter TV code",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocusAwareButton(
                onClick = {
                    validationError = if (onSave()) null else when {
                        serverUrl.isBlank() || tvCode.isBlank() -> "Server URL and TV code are required"
                        !serverUrl.startsWith("http://") && !serverUrl.startsWith("https://") ->
                            "URL must start with http:// or https://"
                        else -> "Invalid settings"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Connect", fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(
                visible = settingsSaved,
                enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
                exit = fadeOut(tween(DURATION_NORMAL, easing = EaseOutQuart))
            ) {
                StatusText(
                    text = "Saved",
                    status = Status.SUCCESS,
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
                    StatusText(text = error, status = Status.WARNING, fontWeight = FontWeight.Medium)
                }
            }
        }

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
                    AppSpinner()
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Testing...", fontWeight = FontWeight.Medium)
                } else {
                    Text("Test Connection", fontWeight = FontWeight.Medium)
                }
            }
            connectionTestResult?.let { result ->
                StatusText(
                    text = result,
                    status = if (result == "Connected") Status.SUCCESS else Status.WARNING,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Alternative link path: pair against this server via PIN, no code needed.
        Spacer(modifier = Modifier.height(4.dp))
        PairWithPinLink(onClick = onPairDevice)
    }
}

/** Quiet, D-pad-focusable text link — the low-emphasis alternative to entering a code. */
@Composable
private fun PairWithPinLink(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = Modifier.onFocusChanged { focused = it.isFocused }
    ) {
        Text(
            text = "No code? Pair with a PIN instead",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = if (focused) TextDecoration.Underline else null
        )
    }
}

private fun generateQrBitmap(url: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512)
        val w = bitMatrix.width
        val h = bitMatrix.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        for (x in 0 until w) {
            for (y in 0 until h) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}
