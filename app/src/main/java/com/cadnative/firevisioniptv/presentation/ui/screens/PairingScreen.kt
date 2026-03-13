package com.cadnative.firevisioniptv.presentation.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.cadnative.firevisioniptv.presentation.ui.theme.FireVisionTheme

/**
 * Compose replacement for the XML-based PairingActivity layout.
 *
 * This screen displays a PIN code for TV pairing alongside a QR code
 * for account registration. It accepts all UI state as parameters so
 * that the existing PairingActivity business logic remains unchanged.
 *
 * Usage from PairingActivity:
 * ```
 * setContent {
 *     FireVisionTheme {
 *         PairingScreen(
 *             pin = currentPin ?: "------",
 *             statusMessage = statusMsg,
 *             statusColor = statusClr,
 *             countdownText = countdown,
 *             isLoading = loading,
 *             showRetryButton = showRetry,
 *             showCountdown = showCd,
 *             qrCodeBitmap = qrBitmap,
 *             serverUrl = serverUrl,
 *             onRetryClick = { requestNewPairing() },
 *             onPairManuallyClick = { /* navigate to SettingsActivity */ },
 *             onUseDefaultClick = { useDefaultChannelList() }
 *         )
 *     }
 * }
 * ```
 *
 * @param pin The 6-character PIN to display (e.g. "------" or "A1B2C3")
 * @param statusMessage Current status text (e.g. "Waiting for confirmation...")
 * @param statusColor Color for the status message text
 * @param countdownText Countdown timer text (e.g. "Expires in: 9:42")
 * @param isLoading Whether to show the loading spinner overlay
 * @param showRetryButton Whether to show the "Generate New PIN" button
 * @param showCountdown Whether to show the countdown timer text
 * @param qrCodeBitmap The generated QR code bitmap for registration URL
 * @param serverUrl The server URL to display at the bottom
 * @param onRetryClick Callback when "Generate New PIN" is clicked
 * @param onPairManuallyClick Callback when "Pair Manually" is clicked
 * @param onUseDefaultClick Callback when "Use Default" is clicked
 */
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
    onPairManuallyClick: () -> Unit,
    onUseDefaultClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Pair Your TV",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main content: left PIN section | divider | right QR section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: PIN display
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Enter PIN",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // PIN display with background card
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pin,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status message
                    Text(
                        text = statusMessage,
                        color = statusColor,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    // Countdown timer
                    if (showCountdown) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = countdownText,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPairManuallyClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = "Pair Manually",
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = onUseDefaultClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = "Use Default",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(0.7f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Right side: QR code for registration
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Don't have an account?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Scan to Create Account",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // QR code
                    if (qrCodeBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "QR Code for Signup",
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
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "tv.cadnative.com/user/register.html",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom section: retry button, instructions, server URL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showRetryButton) {
                    Button(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Generate New PIN",
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                }

                Text(
                    text = "Visit your dashboard to enter this PIN",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = serverUrl,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp
                )
            }
        }
    }
}
