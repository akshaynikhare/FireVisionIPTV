package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.theme.FireOrange
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Pairing Status ──────────────────────────────────────────
            PairingStatusCard(
                isPaired = uiState.isPaired,
                tvCode = uiState.tvCode,
                onPairDevice = onPairDevice
            )

            // ── Server Configuration ────────────────────────────────────
            ServerConfigCard(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                settingsSaved = uiState.settingsSaved,
                onServerUrlChange = { viewModel.onServerUrlChange(it) },
                onTvCodeChange = { viewModel.onTvCodeChange(it) },
                onSave = { viewModel.saveServerSettings() }
            )

            // ── Account Registration QR ─────────────────────────────────
            if (uiState.qrCodeBitmap != null) {
                SettingsCard(title = "Account Registration") {
                    Text(
                        text = "Scan with your phone to register an account",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = uiState.qrCodeBitmap!!.asImageBitmap(),
                            contentDescription = "Registration QR Code",
                            modifier = Modifier.size(180.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // ── Auto-Load Channel ───────────────────────────────────────
            SettingsCard(title = "Auto-Load Channel") {
                Text(
                    text = "Long-press on any channel to set it as auto-load on startup",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.autoloadChannelName.isNotEmpty())
                        "Auto-load: ${uiState.autoloadChannelName}"
                    else
                        "No channel set",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                if (uiState.autoloadChannelName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.clearAutoloadChannel() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear Auto-load", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Display ─────────────────────────────────────────────────
            SettingsCard(title = "Display") {
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

            // ── About ───────────────────────────────────────────────────
            SettingsCard(title = "About") {
                SettingsRow(
                    title = "Version",
                    subtitle = uiState.appVersion,
                    value = ""
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPaired) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isPaired) Color(0xFF4CAF50) else FireOrange,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPaired) "Paired" else "Not Paired",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPaired) "TV Code: $tvCode" else "No TV code configured",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isPaired) {
                Button(
                    onClick = onPairDevice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FireOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Pair Now", fontWeight = FontWeight.Bold)
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
    SettingsCard(title = "Server Configuration", modifier = modifier) {
        Text(
            text = "Server URL",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            placeholder = { Text("https://tv.cadnative.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "TV Pairing Code",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = tvCode,
            onValueChange = onTvCodeChange,
            placeholder = { Text("Enter TV code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onSave() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
            if (settingsSaved) {
                Text(
                    text = "Saved",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
