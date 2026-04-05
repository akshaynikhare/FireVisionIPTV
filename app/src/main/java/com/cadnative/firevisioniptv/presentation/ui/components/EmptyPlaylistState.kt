package com.cadnative.firevisioniptv.presentation.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateFadeIn
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue

@Composable
fun EmptyPlaylistState(
    qrCodeBitmap: Bitmap?,
    onRetry: () -> Unit,
    isMobile: Boolean = false,
    channelManagerUrl: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(40.dp)
                .animateFadeIn()
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = "Empty playlist",
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Your channel list is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            if (isMobile && channelManagerUrl.isNotEmpty()) {
                // Mobile: show button to open channel manager in browser
                Text(
                    text = "Add channels to your playlist to start watching",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    modifier = Modifier.height(48.dp)
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

                Text(
                    text = channelManagerUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            } else {
                // TV: show QR code
                Text(
                    text = "Scan the QR code to learn how to add channels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (qrCodeBitmap != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeAwareQrCode(
                        bitmap = qrCodeBitmap,
                        contentDescription = "QR code — how to add channels"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Text(
                text = "Add channels on the web, then press Refresh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // TV-focusable refresh button
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.05f else 1f,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "refreshBtnScale"
            )
            val border = if (isFocused) {
                BorderStroke(2.dp, FocusBorder.copy(alpha = 0.5f))
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(
                onClick = onRetry,
                border = border,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .onFocusChanged { isFocused = it.isFocused }
            ) {
                Text(
                    text = "Refresh",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
