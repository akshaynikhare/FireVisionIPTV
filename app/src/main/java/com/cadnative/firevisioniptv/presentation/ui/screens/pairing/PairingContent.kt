package com.cadnative.firevisioniptv.presentation.ui.screens.pairing

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Flame300
import com.cadnative.firevisioniptv.presentation.ui.theme.Flame50
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy

@Composable
internal fun PairingContent(
    pin: String,
    statusMessage: String,
    statusColor: Color,
    countdownText: String,
    isLoading: Boolean,
    showRetryButton: Boolean,
    showCountdown: Boolean,
    qrCodeBitmap: Bitmap?,
    serverUrl: String,
    isTvDevice: Boolean,
    pairingUrl: String,
    onRetryClick: () -> Unit,
    onUseDefaultClick: () -> Unit,
    onUseOwnPlaylistClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    // Size the QR to the available height so the PIN below it always fits (reserve
    // room for the label, the PIN box, and padding).
    val qrSize = (configuration.screenHeightDp - 260).coerceIn(180, 320).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(if (isPortrait) 12.dp else if (isTvDevice) 24.dp else 16.dp)
    ) {
        if (isPortrait) {
            PortraitLayout(
                pin, statusMessage, statusColor, countdownText, showCountdown,
                showRetryButton, qrCodeBitmap, serverUrl, isTvDevice, pairingUrl,
                onRetryClick, onUseDefaultClick, onUseOwnPlaylistClick
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT — title, steps, actions
                InfoColumn(
                    isTvDevice = isTvDevice,
                    serverUrl = serverUrl,
                    onUseOwnPlaylistClick = onUseOwnPlaylistClick,
                    onUseDefaultClick = onUseDefaultClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 40.dp)
                )

                // RIGHT — big QR (or browser) on top, PIN below
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isTvDevice) {
                        QrSection(
                            qrCodeBitmap = qrCodeBitmap,
                            modifier = Modifier.fillMaxWidth(),
                            qrSize = qrSize
                        )
                    } else {
                        OpenBrowserSection(
                            pairingUrl = pairingUrl,
                            serverUrl = serverUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PinSection(
                        pin = pin,
                        statusMessage = statusMessage,
                        statusColor = statusColor,
                        countdownText = countdownText,
                        showCountdown = showCountdown,
                        showRetryButton = showRetryButton,
                        onRetryClick = onRetryClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(ScrimHeavy),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = Amber,
                    strokeWidth = 4.dp
                )
            }
        }
    }
}

/** Left panel: title, setup steps, and the two source actions. */
@Composable
private fun InfoColumn(
    isTvDevice: Boolean,
    serverUrl: String,
    onUseOwnPlaylistClick: () -> Unit,
    onUseDefaultClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (isTvDevice) "Pair Your TV" else "Pair Your Device",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isTvDevice) {
            StepText("1. Visit $serverUrl and sign in")
            StepText("2. Enter this PIN to link your TV")
            StepText("3. Add channels and start watching!")
        } else {
            StepText("1. Tap 'Pair in Browser' and sign in")
            StepText("2. Your device links automatically")
            StepText("3. Add channels and start watching!")
        }

        Spacer(modifier = Modifier.height(36.dp))

        AddPlaylistButton(onClick = onUseOwnPlaylistClick)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Use your own M3U or Xtream Codes playlist — no pairing needed",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        DemoModeButton(onClick = onUseDefaultClick)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Demo mode — pair later for your personal channels",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Portrait: everything stacked (title, steps, QR, PIN, actions). */
@Composable
private fun PortraitLayout(
    pin: String,
    statusMessage: String,
    statusColor: Color,
    countdownText: String,
    showCountdown: Boolean,
    showRetryButton: Boolean,
    qrCodeBitmap: Bitmap?,
    serverUrl: String,
    isTvDevice: Boolean,
    pairingUrl: String,
    onRetryClick: () -> Unit,
    onUseDefaultClick: () -> Unit,
    onUseOwnPlaylistClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isTvDevice) "Pair Your TV" else "Pair Your Device",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        StepText("1. Visit $serverUrl and sign in", TextAlign.Center)
        StepText("2. Enter this PIN to link your TV", TextAlign.Center)
        StepText("3. Add channels and start watching!", TextAlign.Center)

        Spacer(modifier = Modifier.height(20.dp))
        if (isTvDevice) {
            QrSection(qrCodeBitmap = qrCodeBitmap, modifier = Modifier.fillMaxWidth())
        } else {
            OpenBrowserSection(pairingUrl = pairingUrl, serverUrl = serverUrl, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(20.dp))
        PinSection(
            pin = pin,
            statusMessage = statusMessage,
            statusColor = statusColor,
            countdownText = countdownText,
            showCountdown = showCountdown,
            showRetryButton = showRetryButton,
            onRetryClick = onRetryClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        AddPlaylistButton(onClick = onUseOwnPlaylistClick)
        Spacer(modifier = Modifier.height(10.dp))
        DemoModeButton(onClick = onUseDefaultClick)
    }
}

@Composable
private fun AddPlaylistButton(onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber,
            contentColor = Color.Black
        )
    ) {
        Text(
            text = "Use My Own Playlist (M3U / Xtream)",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DemoModeButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, Flame300.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Flame50.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = "Browse Demo Channels",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StepText(text: String, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align
    )
}
