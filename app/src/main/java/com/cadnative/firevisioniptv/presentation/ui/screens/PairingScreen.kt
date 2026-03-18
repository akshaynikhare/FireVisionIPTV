package com.cadnative.firevisioniptv.presentation.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    onRetryClick: () -> Unit,
    onUseDefaultClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

            Text(
                text = "Enter the PIN on your dashboard or scan the QR code",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main content: left PIN | divider | right QR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: PIN display
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Your PIN",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // PIN box with amber border
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

                    // Status
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

                // Divider — thin amber line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.6f)
                        .background(Amber.copy(alpha = 0.3f))
                )

                // Right: QR code
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp),
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
                                .size(200.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
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
                                .size(200.dp)
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

            // Bottom bar
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
                        text = "Skip \u2014 Use Default Channels",
                        color = TextDim.copy(alpha = 0.7f),
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
