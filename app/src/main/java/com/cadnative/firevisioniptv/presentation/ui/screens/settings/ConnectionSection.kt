package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.SettingsUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareButton
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareOutlinedButton
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.Warning
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

@Composable
internal fun ConnectionSection(
    uiState: SettingsUiState,
    onResetPairing: () -> Unit,
    onPairDevice: () -> Unit,
    onNavigateToSelfHost: () -> Unit,
    onServerUrlChange: (String) -> Unit,
    onTvCodeChange: (String) -> Unit,
    onSave: () -> Boolean,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.isPaired) {
            PairedStatusBanner(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                onResetPairing = onResetPairing
            )
        } else if (uiState.isDefaultMode) {
            DemoModeBanner(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                onPairDevice = onPairDevice
            )
            SelfHostSetupRow(onSetupServer = onNavigateToSelfHost)
        } else {
            UnpairedSetupCard(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                settingsSaved = uiState.settingsSaved,
                onServerUrlChange = onServerUrlChange,
                onTvCodeChange = onTvCodeChange,
                onSave = onSave,
                onPairDevice = onPairDevice,
                onTestConnection = onTestConnection,
                isTestingConnection = uiState.isTestingConnection,
                connectionTestResult = uiState.connectionTestResult
            )
        }
    }
}

@Composable
private fun StatusBanner(
    idleBorderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) FocusBorder.copy(alpha = 0.5f) else idleBorderColor,
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
        shape = ShapeMedium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        content()
    }
}

/**
 * Banner body layout: a leading [content] Row (status dot + text, weighted)
 * plus a trailing [action]. Side-by-side on TV; on compact/portrait the action
 * stacks full-width below so the button is easy to read and tap.
 */
@Composable
private fun BannerLayout(
    isCompact: Boolean,
    content: @Composable RowScope.() -> Unit,
    action: @Composable () -> Unit
) {
    if (isCompact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
            action()
        }
    } else {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
            action()
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PairedStatusBanner(
    serverUrl: String,
    tvCode: String,
    onResetPairing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    StatusBanner(idleBorderColor = Success.copy(alpha = 0.3f), modifier = modifier) {
        BannerLayout(
            isCompact = isCompact,
            content = {
                StatusDot(Success)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Paired",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$serverUrl  ·  $tvCode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            action = {
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
        )
    }
}

@Composable
private fun DemoModeBanner(
    serverUrl: String,
    tvCode: String,
    onPairDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    StatusBanner(idleBorderColor = Warning.copy(alpha = 0.3f), modifier = modifier) {
        BannerLayout(
            isCompact = isCompact,
            content = {
                StatusDot(Warning)
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
            },
            action = {
                FocusAwareOutlinedButton(onClick = onPairDevice) {
                    Text("Pair Now", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun SelfHostSetupRow(
    onSetupServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    StatusBanner(idleBorderColor = subtleBorder, modifier = modifier) {
        BannerLayout(
            isCompact = isCompact,
            content = {
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
            },
            action = {
                FocusAwareOutlinedButton(onClick = onSetupServer) {
                    Text("Setup", fontWeight = FontWeight.SemiBold)
                }
            }
        )
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
    onTestConnection: () -> Unit,
    isTestingConnection: Boolean,
    connectionTestResult: String?,
    modifier: Modifier = Modifier
) {
    var validationError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    SettingsCard(title = "Device Setup", modifier = modifier) {
        SetupTextField(
            label = "Server URL",
            value = serverUrl,
            placeholder = "https://tv.cadnative.com",
            onValueChange = {
                onServerUrlChange(it)
                validationError = null
            },
            keyboardController = keyboardController
        )

        Spacer(modifier = Modifier.height(10.dp))

        SetupTextField(
            label = "TV Pairing Code",
            value = tvCode,
            placeholder = "Enter TV code",
            onValueChange = {
                onTvCodeChange(it)
                validationError = null
            },
            keyboardController = keyboardController
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
                Text("Save Settings", fontWeight = FontWeight.SemiBold)
            }

            FocusAwareOutlinedButton(onClick = onPairDevice) {
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

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusAwareOutlinedButton(
                onClick = { if (!isTestingConnection && serverUrl.isNotBlank()) onTestConnection() }
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

/**
 * D-pad friendly text field: focusing does not open the keyboard;
 * OK/Enter enters edit mode and shows it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardController: SoftwareKeyboardController?
) {
    var editing by remember { mutableStateOf(false) }

    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium
    )
    Spacer(modifier = Modifier.height(6.dp))
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.hasFocus && !editing) keyboardController?.hide()
                if (!it.hasFocus) editing = false
            }
            .onKeyEvent { event ->
                if (!editing &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    editing = true
                    keyboardController?.show()
                    true
                } else false
            },
        shape = ShapeSmall,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
