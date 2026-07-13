package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.view.KeyEvent
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthChecking
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelBadge
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthOffline
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthOnline
import com.cadnative.firevisioniptv.presentation.ui.theme.HealthUnknown
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.Void700
import com.cadnative.firevisioniptv.presentation.ui.theme.Void800
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryIcon

private const val LONG_PRESS_THRESHOLD_MS = 600L

/** Initials for the logo-less card placeholder: first letters of up to two words. */
private fun channelMonogram(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    val isMobile = isMobileDevice(LocalContext.current)
    val haptic = LocalHapticFeedback.current

    val catColor = categoryColor(channel.category)
    val catIcon = categoryIcon(channel.category)

    // On mobile: non-clickable Card + combinedClickable in modifier (sole touch handler).
    // On TV: clickable Card + D-pad key handler for long-press favorite.
    val cardShape = MaterialTheme.shapes.medium
    val cardBorder = when {
        isFocused -> BorderStroke(2.dp, FocusBorder)
        else -> BorderStroke(1.dp, subtleBorder)
    }
    // Tonal elevation: container steps up one Void level when focused.
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) Void700 else Void800,
        animationSpec = tween(DURATION_FAST, easing = EaseOutQuart),
        label = "cardContainer"
    )
    val cardColors = CardDefaults.cardColors(containerColor = containerColor)

    // tvFocusVisuals owns scale + brand glow + tonal depth; the Card still
    // draws its own background/border/clip on top.
    val baseModifier = modifier
        .tvFocusVisuals(focused = isFocused, shape = cardShape, glowColor = catColor)
        .onFocusChanged { isFocused = it.isFocused }

    if (isMobile) {
        Card(
            modifier = baseModifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFavoriteClick()
                    }
                ),
            shape = cardShape,
            border = cardBorder,
            colors = cardColors
        ) {
            ChannelCardContent(
                channel = channel,
                catColor = catColor,
                catIcon = catIcon,
                focused = isFocused
            )
        }
    } else {
        Card(
            onClick = {
                if (!longPressHandled) onClick()
                longPressHandled = false
            },
            modifier = baseModifier
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
                                if (selectKeyDownTime > 0 &&
                                    System.currentTimeMillis() - selectKeyDownTime >= LONG_PRESS_THRESHOLD_MS &&
                                    !longPressHandled
                                ) {
                                    longPressHandled = true
                                    onFavoriteClick()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent false
                            }
                            KeyEvent.ACTION_UP -> {
                                val wasLongPress = longPressHandled
                                selectKeyDownTime = 0L
                                if (wasLongPress) {
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent false
                            }
                        }
                    }

                    false
                },
            shape = cardShape,
            border = cardBorder,
            colors = cardColors
        ) {
            ChannelCardContent(
                channel = channel,
                catColor = catColor,
                catIcon = catIcon,
                focused = isFocused
            )
        }
    }
}

@Composable
private fun ChannelCardContent(
    channel: ChannelUiModel,
    catColor: Color,
    catIcon: ImageVector,
    focused: Boolean
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val context = LocalContext.current
    val density = LocalDensity.current
    // Decode at the largest card size used by any screen (~240x140dp), not full-res.
    val (targetWidthPx, targetHeightPx) = remember(density) {
        with(density) { 240.dp.roundToPx() to 140.dp.roundToPx() }
    }
    val placeholderPainter = remember { ColorPainter(Void800) }

    Box(modifier = Modifier.fillMaxSize()) {

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

        // Background: thumbnail screenshot (if available)
        if (thumbnailFile != null) {
            AsyncImage(
                model = remember(thumbnailFile) {
                    ImageRequest.Builder(context)
                        .data(thumbnailFile)
                        .size(targetWidthPx, targetHeightPx)
                        // Thumbnails are opaque JPEG video frames — RGB_565 halves memory.
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val hasLogo = channel.logoUrl != null
            // Missing OR broken logo URLs are common in real playlists — fall back to
            // the designed placeholder on load failure, not just when the URL is null.
            var logoFailed by remember(channel.id, channel.logoUrl) { mutableStateOf(false) }
            val showPlaceholder = !hasLogo || logoFailed

            if (showPlaceholder) {
                // Designed placeholder tile: category-tinted fill + centered monogram,
                // so the card is never an empty black box.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    catColor.copy(alpha = 0.38f),
                                    catColor.copy(alpha = 0.12f)
                                )
                            )
                        )
                )
                Text(
                    text = channelMonogram(channel.name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = catColor.copy(alpha = 0.95f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Backdrop panel so light channel artwork stays legible on the card.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(surfaceColor.copy(alpha = 0.55f))
                )
            }

            if (hasLogo && !logoFailed) {
                AsyncImage(
                    model = remember(channel.logoUrl) {
                        ImageRequest.Builder(context)
                            .data(channel.logoUrl)
                            .size(targetWidthPx, targetHeightPx)
                            .build()
                    },
                    contentDescription = null,
                    onError = { logoFailed = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize(0.6f)
                        .padding(top = Dimens.CardLogoTopPadding),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // ── Layer 2: Single refined bottom scrim for text legibility ──
        // One continuous ramp (transparent → near-opaque) reading from ~55%
        // up, deepening on focus so raised cards keep their text crisp.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0f),
                            surfaceColor.copy(alpha = 0.45f),
                            surfaceColor.copy(alpha = if (focused) 0.97f else 0.9f)
                        )
                    )
                )
        )

        // ── Layer 3: Text, badges ───────────────────────────────────
        // Top-right stack: favorite heart above the stream-health dot so the
        // two never overlap regardless of which are present.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Dimens.CardBadgePadding),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimens.Space1)
        ) {
            FavoriteBadge(isFavorite = channel.isFavorite)
            if (channel.healthStatus != ChannelHealthStatus.UNKNOWN) {
                HealthIndicatorDot(status = channel.healthStatus)
            }
        }

        // Channel name + category — bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(Dimens.CardContentPadding),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = channel.category,
                style = LabelBadge,
                color = catColor.copy(alpha = EmphasisMedium)
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
                color = MaterialTheme.colorScheme.onSurface,
                // Long channel names wrap to a second line instead of a hard
                // ellipsis; focus raises the ceiling so nothing gets clipped.
                maxLines = if (focused) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Favorite heart that pops in when toggled on (a quick spring overshoot),
 * gated on reduceMotion. Renders nothing when not a favorite.
 */
@Composable
private fun FavoriteBadge(
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isFavorite) return
    val reduceMotion = LocalPerfProfile.current.reduceMotion
    // Spring the scale from 0 → 1 with a bouncy overshoot each time this badge
    // (re)appears — i.e. when the channel is favorited. Skipped on low-end boxes.
    val scale = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            scale.snapTo(0f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = "Favorite",
        tint = Amber,
        modifier = modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .size(Dimens.IconSmall)
    )
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
            .size(Dimens.HealthDotSize)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = alpha))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { }
}
