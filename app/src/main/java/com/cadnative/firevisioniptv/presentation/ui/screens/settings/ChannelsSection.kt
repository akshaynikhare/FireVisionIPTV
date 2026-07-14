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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareButton
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareOutlinedButton
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingRowLayout
import com.cadnative.firevisioniptv.presentation.ui.screens.SettingsCard
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthChecking
import com.cadnative.firevisioniptv.presentation.ui.theme.Success
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

@Composable
internal fun ChannelsSection(
    scanProgress: ScanProgress,
    onCheckLiveliness: () -> Unit,
    isClearingCache: Boolean,
    cacheCleared: Boolean,
    onClearCache: () -> Unit,
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
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
        },
        action = {
            // Button persists through clearing and completion — removing the
            // focused node resets TV focus to the section rail.
            FocusAwareOutlinedButton(onClick = { if (!isClearingCache) onClearCache() }) {
                if (isClearingCache) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
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
