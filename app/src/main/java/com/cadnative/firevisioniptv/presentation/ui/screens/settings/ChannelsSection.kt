package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.AppSpinner
import com.cadnative.firevisioniptv.presentation.ui.components.Status
import com.cadnative.firevisioniptv.presentation.ui.components.StatusText
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareButton
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareOutlinedButton
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingRowLayout
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthChecking
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeLarge
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

@Composable
internal fun ChannelsSection(
    scanProgress: ScanProgress,
    onCheckLiveliness: () -> Unit,
    isClearingCache: Boolean,
    cacheCleared: Boolean,
    onClearCache: () -> Unit,
    isResettingGuide: Boolean,
    guideReset: Boolean,
    onResetGuide: () -> Unit,
    onResetAppData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val dividerGap = if (isCompact) 14.dp else 10.dp
    SettingsCard(title = "Channels", modifier = modifier) {
        StreamHealthRow(scanProgress = scanProgress, onCheckLiveliness = onCheckLiveliness)

        Spacer(modifier = Modifier.height(dividerGap))
        HorizontalDivider(color = subtleBorder)
        Spacer(modifier = Modifier.height(dividerGap))

        CacheRow(
            isClearingCache = isClearingCache,
            cacheCleared = cacheCleared,
            onClearCache = onClearCache
        )

        Spacer(modifier = Modifier.height(dividerGap))
        HorizontalDivider(color = subtleBorder)
        Spacer(modifier = Modifier.height(dividerGap))

        GuideDataRow(
            isResettingGuide = isResettingGuide,
            guideReset = guideReset,
            onResetGuide = onResetGuide
        )

        Spacer(modifier = Modifier.height(dividerGap))
        HorizontalDivider(color = subtleBorder)
        Spacer(modifier = Modifier.height(dividerGap))

        ResetAppDataRow(onResetAppData = onResetAppData)
    }
}

@Composable
private fun StreamHealthRow(
    scanProgress: ScanProgress,
    onCheckLiveliness: () -> Unit
) {
    // The row and its button stay composed while scanning: removing the focused
    // node drops TV focus and snaps the section rail back to Connection.
    SettingRowLayout(
        text = {
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
                            .clip(ShapeSmall),
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
                Text(
                    text = "Stream Health",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatusText(
                    text = if (scanProgress.total > 0)
                        "Last scan: ${scanProgress.scanned}/${scanProgress.total} checked"
                    else
                        "Scan channels to check if streams are online",
                    status = if (scanProgress.total > 0) Status.SUCCESS else Status.NEUTRAL
                )
            }
        },
        action = {
            // Guarded click instead of enabled=false — a disabled button is not
            // focusable, which would drop focus just like removing it.
            FocusAwareButton(
                onClick = { if (!scanProgress.isScanning) onCheckLiveliness() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (scanProgress.isScanning) {
                    AppSpinner(color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (scanProgress.isScanning) "Scanning..." else "Check Liveness",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun CacheRow(
    isClearingCache: Boolean,
    cacheCleared: Boolean,
    onClearCache: () -> Unit
) {
    SettingRowLayout(
        text = {
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
                StatusText(
                    text = "Cache cleared — refreshing channels",
                    status = Status.SUCCESS
                )
            }
            if (!cacheCleared) {
                Text(
                    text = "Clear cached channels, guide data, and thumbnails",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        action = {
            // Button persists through clearing and completion — removing the
            // focused node resets TV focus to the section rail.
            FocusAwareOutlinedButton(onClick = { if (!isClearingCache) onClearCache() }) {
                if (isClearingCache) {
                    AppSpinner()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isClearingCache) "Clearing..." else "Clear Cache",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun GuideDataRow(
    isResettingGuide: Boolean,
    guideReset: Boolean,
    onResetGuide: () -> Unit
) {
    SettingRowLayout(
        text = {
            Text(
                text = "Guide Data",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            AnimatedVisibility(
                visible = guideReset,
                enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
                exit = fadeOut(tween(DURATION_NORMAL, easing = EaseOutQuart))
            ) {
                StatusText(
                    text = "Guide data reset — schedule re-downloaded",
                    status = Status.SUCCESS
                )
            }
            if (!guideReset) {
                Text(
                    text = "Purge the program guide and re-download the schedule",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        action = {
            FocusAwareOutlinedButton(onClick = { if (!isResettingGuide) onResetGuide() }) {
                if (isResettingGuide) {
                    AppSpinner()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isResettingGuide) "Resetting..." else "Reset Guide",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun ResetAppDataRow(onResetAppData: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    SettingRowLayout(
        text = {
            Text(
                text = "Reset App Data",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Erase pairing, favorites, and all settings — like a fresh install",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        action = {
            FocusAwareOutlinedButton(
                onClick = { showConfirm = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "Reset App",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )

    if (showConfirm) {
        ResetAppDataConfirmDialog(
            onConfirm = {
                showConfirm = false
                onResetAppData()
            },
            onDismiss = { showConfirm = false }
        )
    }
}

@Composable
private fun ResetAppDataConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // D-pad lands on Cancel first so a stray OK press can't wipe the app.
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { cancelFocus.requestFocus() }
    }

    // Custom Dialog instead of M3 AlertDialog: the platform dialog's dim is too
    // weak on TV and AlertDialog doesn't expose the scrim, so the full-screen
    // backdrop is drawn explicitly behind the card.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = EmphasisMedium)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, subtleBorder),
                modifier = Modifier
                    .padding(Dimens.Space5)
                    .widthIn(max = Dimens.DialogMaxWidth)
            ) {
                Column(modifier = Modifier.padding(Dimens.Space5)) {
                    Text(
                        text = "Reset app data?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Dimens.Space3))
                    Text(
                        text = "This erases pairing, favorites, playback history, and all " +
                            "settings. The app will close and start fresh, like a new install.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.Space5))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                    ) {
                        FocusAwareOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(cancelFocus)
                        ) {
                            Text(text = "Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        FocusAwareButton(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(text = "Erase Everything", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
