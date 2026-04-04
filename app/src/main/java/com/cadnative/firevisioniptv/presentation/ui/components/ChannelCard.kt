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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryIcon

private const val LONG_PRESS_THRESHOLD_MS = 600L

@Composable
fun ChannelCard(
    channel: ChannelUiModel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var longPressHandled by remember { mutableStateOf(false) }
    var selectKeyDownTime by remember { mutableLongStateOf(0L) }

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
            .onPreviewKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val action = keyEvent.nativeKeyEvent.action

                // Menu / Bookmark → instant favorite toggle
                if (action == KeyEvent.ACTION_DOWN &&
                    (code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_BOOKMARK)
                ) {
                    onFavoriteClick()
                    return@onPreviewKeyEvent true
                }

                // D-pad center / Enter → track hold duration for long-press
                if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
                    when (action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                                selectKeyDownTime = System.currentTimeMillis()
                                longPressHandled = false
                            }
                            // Check on repeated key events (held down)
                            if (selectKeyDownTime > 0 &&
                                System.currentTimeMillis() - selectKeyDownTime >= LONG_PRESS_THRESHOLD_MS &&
                                !longPressHandled
                            ) {
                                longPressHandled = true
                                onFavoriteClick()
                                return@onPreviewKeyEvent true
                            }
                            // Don't consume — let Card handle normal press
                            return@onPreviewKeyEvent false
                        }
                        KeyEvent.ACTION_UP -> {
                            val wasLongPress = longPressHandled
                            selectKeyDownTime = 0L
                            // If long-press was handled, consume the UP to prevent Card's onClick
                            if (wasLongPress) {
                                return@onPreviewKeyEvent true
                            }
                            return@onPreviewKeyEvent false
                        }
                    }
                }

                false
            },
        shape = MaterialTheme.shapes.medium,
        border = when {
            isFocused -> BorderStroke(2.dp, FocusBorder)
            else -> BorderStroke(1.dp, subtleBorder)
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
    val surfaceColor = MaterialTheme.colorScheme.surface
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
        var thumbnailFile by remember(channel.id, channel.thumbnailPath) { mutableStateOf<File?>(null) }
        LaunchedEffect(channel.id, channel.thumbnailPath) {
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
                    .fillMaxSize(0.6f)
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp),
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
                .fillMaxHeight(0.50f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0f),
                            surfaceColor.copy(alpha = 0.5f),
                            surfaceColor.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // ── Layer 3: Text, badges, accent line ──────────────────────
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
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = Amber,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(16.dp)
            )
        }

        // Channel name + category — bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0f),
                            surfaceColor.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = channel.category,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = EmphasisMedium)
            )
            if (channel.nowProgramTitle != null) {
                Text(
                    text = channel.nowProgramTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = catColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
