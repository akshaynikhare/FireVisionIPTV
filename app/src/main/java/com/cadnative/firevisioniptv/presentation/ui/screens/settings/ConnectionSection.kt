package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.SettingsUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.StatusDot
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareOutlinedButton
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.Warning
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

// Source type strings mirror AppPreferences.SOURCE_M3U / SOURCE_XTREAM.
private const val SOURCE_M3U = "m3u"
private const val SOURCE_XTREAM = "xtream"

/**
 * Single "Channel Source" surface: shows the one active source (paired, demo,
 * M3U, or Xtream) and a shared "Change source ▸" entry to the unified AddSource
 * screen. The per-source setup forms live in AddSource, not here.
 */
@Composable
internal fun ConnectionSection(
    uiState: SettingsUiState,
    onResetPairing: () -> Unit,
    onPairDevice: () -> Unit,
    onNavigateToSelfHost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSource = uiState.isPaired || uiState.isDefaultMode ||
        (uiState.sourceType == SOURCE_M3U && uiState.m3uUrl.isNotBlank()) ||
        (uiState.sourceType == SOURCE_XTREAM && uiState.xtreamHost.isNotBlank())

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            // Check the active playlist source first: a lingering demo/paired flag
            // must not mask a bring-your-own M3U/Xtream source that's now in use.
            uiState.sourceType == SOURCE_M3U && uiState.m3uUrl.isNotBlank() ->
                PlaylistSourceBanner(title = "M3U playlist", detail = uiState.m3uUrl)
            uiState.sourceType == SOURCE_XTREAM && uiState.xtreamHost.isNotBlank() ->
                PlaylistSourceBanner(title = "Xtream Codes", detail = uiState.xtreamHost)
            uiState.isPaired -> PairedStatusBanner(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                onResetPairing = onResetPairing
            )
            uiState.isDefaultMode -> DemoModeBanner(
                serverUrl = uiState.serverUrl,
                tvCode = uiState.tvCode,
                onPairDevice = onPairDevice
            )
            else -> NoSourceCard(onSetup = onNavigateToSelfHost)
        }

        // One shared entry to switch sources (pairing / self-hosted / playlist).
        if (hasSource) ChangeSourceRow(onChangeSource = onNavigateToSelfHost)
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
private fun PlaylistSourceBanner(
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    StatusBanner(idleBorderColor = SteelBlue.copy(alpha = 0.3f), modifier = modifier) {
        BannerLayout(
            isCompact = isCompact,
            content = {
                StatusDot(SteelBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            action = {}
        )
    }
}

@Composable
private fun NoSourceCard(
    onSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    StatusBanner(idleBorderColor = subtleBorder, modifier = modifier) {
        BannerLayout(
            isCompact = isCompact,
            content = {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No channel source",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pair your TV or add a playlist to start watching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            action = {
                FocusAwareOutlinedButton(onClick = onSetup) {
                    Text("Set up", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun ChangeSourceRow(
    onChangeSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    StatusBanner(idleBorderColor = subtleBorder, modifier = modifier) {
        BannerLayout(
            isCompact = isCompact,
            content = {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Change source",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Switch to pairing, a self-hosted server, or your own playlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            action = {
                FocusAwareOutlinedButton(onClick = onChangeSource) {
                    Text("Change  ▸", fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}
