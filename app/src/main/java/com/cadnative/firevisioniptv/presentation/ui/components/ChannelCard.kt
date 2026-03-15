package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import java.io.File
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthChecking
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthOffline
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthOnline
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthUnknown
import com.cadnative.firevisioniptv.presentation.ui.theme.SubtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.TextPrimary
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: ChannelUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .width(200.dp)
            .height(120.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onFavoriteClick
            ),
        shape = MaterialTheme.shapes.medium,
        border = when {
            isFocused -> BorderStroke(2.dp, FocusBorder)
            else -> BorderStroke(1.dp, SubtleBorder)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        val catColor = categoryColor(channel.category)

        Box(modifier = Modifier.fillMaxSize()) {
            // Prefer live thumbnail over static logo (remember avoids disk I/O on every recomposition)
            val imageModel = remember(channel.thumbnailPath, channel.logoUrl) {
                channel.thumbnailPath?.let { path ->
                    File(path).takeIf { it.exists() }
                } ?: channel.logoUrl
            }

            AsyncImage(
                model = imageModel,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Category accent line at top
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(catColor.copy(alpha = 0.8f))
            )

            // Cinematic gradient overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Channel name
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )

            // Top-right row: favorite star (only when favorite) + health indicator
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (channel.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = Amber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (channel.healthStatus != ChannelHealthStatus.UNKNOWN) {
                    HealthIndicatorDot(status = channel.healthStatus)
                }
            }

            // Long-press hint when focused
            if (isFocused) {
                Text(
                    text = "Hold to favorite",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthIndicatorDot(
    status: ChannelHealthStatus,
    modifier: Modifier = Modifier
) {
    val dotColor = when (status) {
        ChannelHealthStatus.ONLINE -> HealthOnline
        ChannelHealthStatus.CHECKING -> HealthChecking
        ChannelHealthStatus.OFFLINE -> HealthOffline
        ChannelHealthStatus.UNKNOWN -> HealthUnknown
    }

    val label = when (status) {
        ChannelHealthStatus.ONLINE -> "Stream online"
        ChannelHealthStatus.CHECKING -> "Checking stream"
        ChannelHealthStatus.OFFLINE -> "Stream offline"
        ChannelHealthStatus.UNKNOWN -> "Stream status unknown"
    }

    // Pulse animation for CHECKING state
    val alpha = if (status == ChannelHealthStatus.CHECKING) {
        val infiniteTransition = rememberInfiniteTransition(label = "healthPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "healthPulseAlpha"
        ).value
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = alpha))
            .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Invisible text for accessibility — TalkBack reads the status
        Text(
            text = "",
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}
