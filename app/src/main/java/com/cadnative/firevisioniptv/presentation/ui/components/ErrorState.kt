package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.cadnative.firevisioniptv.presentation.ui.animation.animateFadeIn

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.UNKNOWN,
    onPairDevice: (() -> Unit)? = null
) {
    val icon = when (errorType) {
        ErrorType.AUTH_REQUIRED -> Icons.Default.Link
        ErrorType.NETWORK_ERROR -> Icons.Default.WifiOff
        else -> Icons.Default.Warning
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .padding(40.dp)
                .animateFadeIn()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = message,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (errorType == ErrorType.AUTH_REQUIRED && onPairDevice != null) {
                Button(
                    onClick = onPairDevice,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Pair Now",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            OutlinedButton(
                onClick = onRetry,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
