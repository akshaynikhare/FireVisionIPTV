package com.cadnative.firevisioniptv.presentation.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.components.ThemeAwareQrCode
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.Warning
import com.cadnative.firevisioniptv.presentation.viewmodel.SettingsViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private const val SERVER_GUIDE_URL = "https://github.com/akshaynikhare/FireVisionIPTVServer/blob/main/docs/workflow/SELF_HOSTING_GUIDE.md"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfHostSetupScreen(
    onNavigateBack: () -> Unit,
    onPairDevice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val guideQrBitmap = remember {
        if (!isPortrait) generateQrBitmap(SERVER_GUIDE_URL) else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isPortrait) 16.dp else 24.dp, vertical = if (isPortrait) 8.dp else 16.dp)
    ) {
        // Back button + title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = if (isPortrait) 8.dp else 16.dp)
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
                text = "Self-Hosted Server Setup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isPortrait) {
            // Portrait: stack vertically, no QR code
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
            // Landscape/TV: 2-column layout with QR code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsCard(
                    title = "Setup Guide",
                    modifier = Modifier.weight(1f)
                ) {
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
                    Spacer(modifier = Modifier.height(12.dp))

                    guideQrBitmap?.let { bitmap ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            ThemeAwareQrCode(
                                bitmap = bitmap,
                                contentDescription = "Server setup guide QR code",
                                size = 200.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SetupStep(number = "1", text = "Deploy FireVision IPTV Server on your machine")
                    Spacer(modifier = Modifier.height(6.dp))
                    SetupStep(number = "2", text = "Add your IPTV channels via the web dashboard")
                    Spacer(modifier = Modifier.height(6.dp))
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
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SetupGuideCardPortrait() {
    val context = LocalContext.current

    SettingsCard(
        title = "Setup Guide",
        modifier = Modifier.fillMaxWidth()
    ) {
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
            Text(
                text = "View Setup Guide",
                fontWeight = FontWeight.Medium,
                color = SteelBlue
            )
        }
    }
}

@Composable
private fun SetupStep(
    number: String,
    text: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(4.dp),
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
    val keyboardController = LocalSoftwareKeyboardController.current
    var serverUrlEditing by remember { mutableStateOf(false) }
    var tvCodeEditing by remember { mutableStateOf(false) }

    SettingsCard(title = "Device Setup", modifier = modifier) {
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
            placeholder = { Text("https://your-server.com") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.hasFocus && !serverUrlEditing) keyboardController?.hide()
                    if (!it.hasFocus) serverUrlEditing = false
                }
                .onKeyEvent { event ->
                    if (!serverUrlEditing &&
                        event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    ) {
                        serverUrlEditing = true
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
            onValueChange = {
                onTvCodeChange(it)
                validationError = null
            },
            placeholder = { Text("Enter TV code") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.hasFocus && !tvCodeEditing) keyboardController?.hide()
                    if (!it.hasFocus) tvCodeEditing = false
                }
                .onKeyEvent { event ->
                    if (!tvCodeEditing &&
                        event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    ) {
                        tvCodeEditing = true
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
