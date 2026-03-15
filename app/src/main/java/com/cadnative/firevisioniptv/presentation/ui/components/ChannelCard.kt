package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusGlow
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

    // Scale up on focus — cinematic lift effect
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = EaseOutQuart),
        label = "cardScale"
    )

    // Subtle image zoom on focus — intentionally slower for Ken Burns-lite drift
    val imageScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = DURATION_NORMAL * 2, easing = EaseOutQuart),
        label = "imageScale"
    )

    // Gradient intensifies on focus for better text readability
    val gradientAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.95f else 0.9f,
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = EaseOutQuart),
        label = "gradientAlpha"
    )

    // Subtle glow behind card on focus
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.15f else 0f,
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = EaseOutQuart),
        label = "cardGlow"
    )

    Box(
        modifier = modifier
            .width(200.dp)
            .height(120.dp)
    ) {
        // Ambient glow layer (behind the card) — always rendered, alpha drives visibility
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    this.alpha = glowAlpha
                    scaleX = 1.12f
                    scaleY = 1.12f
                }
                .background(
                    FocusGlow,
                    MaterialTheme.shapes.medium
                )
        )

        Card(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
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
            ChannelCardContent(
                channel = channel,
                isFocused = isFocused,
                imageScale = imageScale,
                gradientAlpha = gradientAlpha
            )
        }
    }
}

/** Extracted from Card's ColumnScope to avoid AnimatedVisibility receiver conflict. */
@Composable
private fun ChannelCardContent(
    channel: ChannelUiModel,
    isFocused: Boolean,
    imageScale: Float,
    gradientAlpha: Float
) {
    val catColor = categoryColor(channel.category)

    Box(modifier = Modifier.fillMaxSize()) {
        // Prefer live thumbnail over static logo
        var thumbnailFile by remember(channel.thumbnailPath) { mutableStateOf<File?>(null) }
        LaunchedEffect(channel.thumbnailPath) {
            thumbnailFile = withContext(Dispatchers.IO) {
                channel.thumbnailPath?.let { path ->
                    File(path).takeIf { it.exists() }
                }
            }
        }
        val imageModel = thumbnailFile ?: channel.logoUrl

        // Image with subtle zoom on focus
        AsyncImage(
            model = imageModel,
            contentDescription = channel.name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = imageScale
                    scaleY = imageScale
                },
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

        // Cinematic gradient overlay — deepens on focus
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = gradientAlpha)
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

        // Top-right row: favorite star + health indicator
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated favorite star
            AnimatedVisibility(
                visible = channel.isFavorite,
                enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)) + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart)
                ),
                exit = fadeOut(tween(DURATION_FAST)) + scaleOut(
                    targetScale = 0.6f,
                    animationSpec = tween(DURATION_FAST)
                )
            ) {
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

        // Long-press hint when focused — animated fade-in
        AnimatedVisibility(
            visible = isFocused,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_FAST))
        ) {
            Text(
                text = "Hold to favorite",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
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
            .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { }
}
