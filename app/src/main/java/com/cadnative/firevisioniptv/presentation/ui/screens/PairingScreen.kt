package com.cadnative.firevisioniptv.presentation.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue
import com.cadnative.firevisioniptv.presentation.ui.theme.TextDim

@Composable
fun PairingScreen(
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
    isPaired: Boolean = false,
    pairedUsername: String = "",
    channelManagerQrBitmap: Bitmap? = null,
    onRetryClick: () -> Unit,
    onUseDefaultClick: () -> Unit,
    onContinue: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    if (isPaired) {
        PairingSuccessContent(
            username = pairedUsername,
            serverUrl = serverUrl,
            isTvDevice = isTvDevice,
            channelManagerQrBitmap = channelManagerQrBitmap,
            onContinue = onContinue
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(if (isPortrait) 24.dp else 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isPortrait) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Pair Your TV",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step-by-step instructions — device-aware
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isTvDevice) {
                    StepText("1. Go to $serverUrl on your phone or computer")
                    StepText("2. Create an account or sign in")
                    StepText("3. Enter this PIN to link your TV")
                } else {
                    StepText("1. Tap 'Pair in Browser' below to create an account")
                    StepText("2. Your device will be linked automatically")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isPortrait) {
                // Portrait: stack vertically
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

                Spacer(modifier = Modifier.height(24.dp))

                // Horizontal divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.dp)
                        .background(Amber.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isTvDevice) {
                    QrSection(
                        qrCodeBitmap = qrCodeBitmap,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OpenBrowserSection(
                        pairingUrl = pairingUrl,
                        serverUrl = serverUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                // Landscape: side by side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PinSection(
                        pin = pin,
                        statusMessage = statusMessage,
                        statusColor = statusColor,
                        countdownText = countdownText,
                        showCountdown = showCountdown,
                        showRetryButton = showRetryButton,
                        onRetryClick = onRetryClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 24.dp)
                    )

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.6f)
                            .background(Amber.copy(alpha = 0.3f))
                    )

                    if (isTvDevice) {
                        QrSection(
                            qrCodeBitmap = qrCodeBitmap,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 24.dp)
                        )
                    } else {
                        OpenBrowserSection(
                            pairingUrl = pairingUrl,
                            serverUrl = serverUrl,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 24.dp)
                        )
                    }
                }
            }

            // Bottom bar
            if (isPortrait) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visit",
                            color = TextDim,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = serverUrl,
                            color = SteelBlue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "to enter this PIN",
                            color = TextDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onUseDefaultClick) {
                        Text(
                            text = "Browse Demo Channels",
                            color = TextDim.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "Demo mode — pair your device later for your personal channels",
                        color = TextDim.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visit",
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = serverUrl,
                        color = SteelBlue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "to enter this PIN",
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    TextButton(onClick = onUseDefaultClick) {
                        Text(
                            text = "Browse Demo Channels",
                            color = TextDim.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Demo mode — pair later for your channels",
                        color = TextDim.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)),
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

@Composable
private fun PinSection(
    pin: String,
    statusMessage: String,
    statusColor: Color,
    countdownText: String,
    showCountdown: Boolean,
    showRetryButton: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your PIN",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Amber.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 36.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pin,
                color = Amber,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = statusMessage,
            color = statusColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        if (showCountdown) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = countdownText,
                color = SteelBlue,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        if (showRetryButton) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Generate New PIN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QrSection(
    qrCodeBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Scan with your phone",
            color = TextDim,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Scan to Pair",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (qrCodeBitmap != null) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "QR Code for Pairing",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Amber,
                    strokeWidth = 3.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Sign in or register to pair",
            color = SteelBlue,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OpenBrowserSection(
    pairingUrl: String,
    serverUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quick Pair",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap to open your browser and complete pairing",
            color = TextDim,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (pairingUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pairingUrl))
                    context.startActivity(intent)
                }
            },
            enabled = pairingUrl.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Pair in Browser",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Or enter PIN manually at $serverUrl",
            color = TextDim.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PairingSuccessContent(
    username: String,
    serverUrl: String,
    isTvDevice: Boolean,
    channelManagerQrBitmap: Bitmap?,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val channelManagerUrl = "$serverUrl/user/channels"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "You're all set!",
                style = MaterialTheme.typography.displaySmall,
                color = Amber,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome, $username",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isTvDevice) {
                Text(
                    text = "Scan the QR code or visit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = channelManagerUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SteelBlue,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "to add channels to your playlist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (channelManagerQrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = channelManagerQrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for Channel Manager",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Amber,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                Text(
                    text = "Add channels to your playlist to start watching",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channelManagerUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteelBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Channel Manager",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = channelManagerUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(48.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
