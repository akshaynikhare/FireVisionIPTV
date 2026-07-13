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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareButton
import com.cadnative.firevisioniptv.presentation.ui.screens.FocusAwareOutlinedButton
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
    SettingsCard(title = "Channels", modifier = modifier) {
        StreamHealthRow(scanProgress = scanProgress, onCheckLiveliness = onCheckLiveliness)

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = subtleBorder)
        Spacer(modifier = Modifier.height(10.dp))

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
}

@Composable
private fun CacheRow(
    isClearingCache: Boolean,
    cacheCleared: Boolean,
    onClearCache: () -> Unit
) {
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
                FocusAwareOutlinedButton(onClick = onClearCache) {
                    Text("Clear Cache", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
