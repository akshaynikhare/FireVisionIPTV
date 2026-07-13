package com.cadnative.firevisioniptv.presentation.ui.screens.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.DisplayPairingCode
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation
import com.cadnative.firevisioniptv.presentation.ui.theme.SteelBlue
import com.cadnative.firevisioniptv.presentation.ui.theme.softShadow

@Composable
internal fun PinSection(
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
                .softShadow(Elevation.Level2, MaterialTheme.shapes.medium)
                .border(
                    width = 1.dp,
                    color = Amber.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = Dimens.Space6, vertical = Dimens.Space5),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pin,
                color = Amber,
                style = DisplayPairingCode
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
                shape = MaterialTheme.shapes.small
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
