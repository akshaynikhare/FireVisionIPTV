package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.DiagonalGradientBackground
import com.cadnative.firevisioniptv.presentation.ui.theme.Error
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium

/**
 * Full-screen "Update Available" screen shown once at launch (over Home) when a
 * newer app version is published. Stateless — the hosting composable owns the
 * [UpdateInfo] and download state and supplies the callbacks.
 *
 * [isDownloading] swaps the action buttons for a progress indicator;
 * [downloadError], if present, is shown below the actions. Both actions are
 * D-pad focusable; "Update Now" auto-focuses so a single OK press updates.
 */
@Composable
fun UpdateAvailableScreen(
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    downloadError: String?,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val hPadding = if (isCompact) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv
    val updateFocus = remember { FocusRequester() }

    LaunchedEffect(isDownloading) {
        if (!isDownloading) runCatching { updateFocus.requestFocus() }
    }

    DiagonalGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.Center)
                .padding(horizontal = hPadding, vertical = Dimens.ScreenPaddingVertical),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Icon medallion ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(Dimens.StateMedallionSize)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdateAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconLarge)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Space5))

            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.Space2))

            Text(
                text = buildString {
                    append("Version ${updateInfo.versionName}")
                    if (updateInfo.fileSize.isNotEmpty()) append("  ·  ${updateInfo.fileSize}")
                    if (updateInfo.isMandatory) append("  ·  Recommended")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (updateInfo.releaseNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.Space5))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeMedium,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.Space4)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Space6))

            if (isDownloading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.IconMedium),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Dimens.Space3))
                    Text(
                        text = "Downloading update…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                val stackButtons = isCompact
                val actions: @Composable () -> Unit = {
                    FocusAwareButton(
                        onClick = onUpdateNow,
                        modifier = Modifier
                            .focusRequester(updateFocus)
                            .then(if (stackButtons) Modifier.fillMaxWidth() else Modifier),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Update Now", fontWeight = FontWeight.SemiBold)
                    }
                    FocusAwareOutlinedButton(
                        onClick = onDismiss,
                        modifier = if (stackButtons) Modifier.fillMaxWidth() else Modifier
                    ) {
                        Text("Not Now")
                    }
                }

                if (stackButtons) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
                    ) { actions() }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space4)) { actions() }
                }
            }

            if (downloadError != null) {
                Spacer(modifier = Modifier.height(Dimens.Space4))
                Text(
                    text = downloadError,
                    style = MaterialTheme.typography.bodySmall,
                    color = Error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
