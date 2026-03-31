package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthChecking
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthOffline
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthOnline
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthUnknown
import com.cadnative.firevisioniptv.presentation.ui.theme.SubtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceElevated
import com.cadnative.firevisioniptv.presentation.ui.theme.TextDim
import com.cadnative.firevisioniptv.presentation.ui.theme.TextPrimary
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryIcon

@Composable
fun ChannelCard(
    channel: ChannelUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var longPressHandled by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = EaseOutQuart),
        label = "cardScale"
    )

    val catColor = categoryColor(channel.category)
    val catIcon = categoryIcon(channel.category)

    Card(
        onClick = {
            if (!longPressHandled) onClick()
            longPressHandled = false
        },
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_MENU,
                    KeyEvent.KEYCODE_BOOKMARK -> {
                        onFavoriteClick()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        if (keyEvent.nativeKeyEvent.isLongPress) {
                            longPressHandled = true
                            onFavoriteClick()
                            true
                        } else false
                    }
                    else -> false
                }
            },
        shape = MaterialTheme.shapes.medium,
        border = when {
            isFocused -> BorderStroke(2.dp, FocusBorder)
            else -> BorderStroke(1.dp, SubtleBorder)
        },
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        ChannelCardContent(
            channel = channel,
            isFocused = isFocused,
            catColor = catColor,
            catIcon = catIcon
        )
    }
}

@Composable
private fun ChannelCardContent(
    channel: ChannelUiModel,
    isFocused: Boolean,
    catColor: androidx.compose.ui.graphics.Color,
    catIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Layer 1: Logo / image as full-bleed background ──────────
        // Category gradient base (always visible behind logo)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            catColor.copy(alpha = 0.18f),
                            catColor.copy(alpha = 0.04f)
                        )
                    )
                )
        )

        // Logo or thumbnail fills the card
        var thumbnailFile by remember(channel.thumbnailPath) { mutableStateOf<File?>(null) }
        LaunchedEffect(channel.thumbnailPath) {
            thumbnailFile = withContext(Dispatchers.IO) {
                channel.thumbnailPath?.let { path ->
                    File(path).takeIf { it.exists() }
                }
            }
        }

        if (channel.logoUrl != null) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        } else if (thumbnailFile != null) {
            AsyncImage(
                model = thumbnailFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = catIcon,
                contentDescription = null,
                tint = catColor.copy(alpha = 0.3f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            )
        }

        // ── Layer 2: Translucent scrim for text readability ─────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceElevated.copy(alpha = 0f),
                            SurfaceElevated.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // ── Layer 3: Text, badges, accent line ──────────────────────
        // Accent line at top
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(catColor.copy(alpha = 0.8f))
        )

        // Health indicator — top-right
        if (channel.healthStatus != ChannelHealthStatus.UNKNOWN) {
            HealthIndicatorDot(
                status = channel.healthStatus,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

        // Favorite badge — top-left
        if (channel.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = Amber,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(16.dp)
            )
        }

        // "Hold to favorite" hint when focused
        if (isFocused && !channel.isFavorite) {
            Text(
                text = "Hold to favorite",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        // Channel name + category — bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = catColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channel.nowProgramTitle != null) {
                Text(
                    text = channel.nowProgramTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = channel.category,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary.copy(alpha = EmphasisMedium)
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
        ChannelHealthStatus.UNRESPONSIVE -> HealthChecking
        ChannelHealthStatus.UNKNOWN -> HealthUnknown
    }

    val label = when (status) {
        ChannelHealthStatus.ONLINE -> "Stream online"
        ChannelHealthStatus.CHECKING -> "Checking stream"
        ChannelHealthStatus.OFFLINE -> "Stream offline"
        ChannelHealthStatus.UNRESPONSIVE -> "Stream unresponsive"
        ChannelHealthStatus.UNKNOWN -> "Stream status unknown"
    }

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
            .size(8.dp)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = alpha))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { }
}
