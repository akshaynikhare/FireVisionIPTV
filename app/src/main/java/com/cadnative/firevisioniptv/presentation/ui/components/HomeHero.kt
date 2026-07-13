package com.cadnative.firevisioniptv.presentation.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryIcon
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.Void800
import com.cadnative.firevisioniptv.presentation.ui.theme.Void900
import com.cadnative.firevisioniptv.presentation.ui.theme.Void950
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private const val HERO_HEIGHT_FRACTION = 0.35f
private const val SAFE_MARGIN_FRACTION = 0.05f

/** Hero height as a fraction of screen height — shared with the home skeleton. */
@Composable
fun rememberHeroHeight(): Dp {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    return remember(screenHeightDp) { (screenHeightDp * HERO_HEIGHT_FRACTION).dp }
}

/**
 * YouTube-TV-style focus-driven hero: full-width static backdrop of the
 * focused/featured channel, left scrim for text legibility, and a
 * "Watch now" button that starts playback. Content crossfades when the
 * hero channel changes (snaps on reduce-motion devices).
 */
@Composable
fun HomeHero(
    channel: ChannelUiModel,
    onWatchNow: (String) -> Unit,
    watchNowFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val heroHeight = rememberHeroHeight()
    val reduceMotion = LocalPerfProfile.current.reduceMotion
    val swapSpec = if (reduceMotion) snap<Float>() else tween(DURATION_NORMAL, easing = EaseOutQuart)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val safeMargin = remember(screenWidthDp) { (screenWidthDp * SAFE_MARGIN_FRACTION).dp }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(Void900)
    ) {
        Crossfade(targetState = channel, animationSpec = swapSpec, label = "heroBackdrop") { hero ->
            HeroBackdrop(channel = hero, heroHeight = heroHeight, safeMargin = safeMargin)
        }

        // Left-to-right scrim so text always sits on solid ground
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Void950, Void950.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // Bottom-up scrim — grounds the info block and adds cinematic depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Void950.copy(alpha = 0.55f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.55f)
                .padding(horizontal = safeMargin)
                .padding(bottom = safeMargin)
        ) {
            Crossfade(targetState = channel, animationSpec = swapSpec, label = "heroInfo") { hero ->
                HeroInfo(hero = hero)
            }
            Spacer(modifier = Modifier.height(Dimens.Space5))
            WatchNowButton(
                onClick = { onWatchNow(channel.id) },
                focusRequester = watchNowFocusRequester,
                reduceMotion = reduceMotion
            )
        }
    }
}

/**
 * Cinematic hero text block: channel name, a LIVE badge, the now/next EPG
 * program titles, and a live program-progress bar (elapsed within the
 * currently-airing program). All EPG-derived fields degrade gracefully when
 * the guide is unavailable.
 */
@Composable
private fun HeroInfo(hero: ChannelUiModel) {
    Column {
        Text(
            text = hero.name,
            style = MaterialTheme.typography.displaySmall,
            color = OnVideo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(Dimens.Space2))

        Text(
            text = hero.category.ifBlank { "Live TV" },
            style = MaterialTheme.typography.titleMedium,
            color = OnVideo.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        hero.nowProgramTitle?.let { nowTitle ->
            Spacer(modifier = Modifier.height(Dimens.Space3))
            Text(
                text = "Now: $nowTitle",
                style = MaterialTheme.typography.titleMedium,
                color = OnVideo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hero.nowProgramStartMs != null && hero.nowProgramEndMs != null) {
                Spacer(modifier = Modifier.height(Dimens.Space2))
                HeroProgressBar(
                    startMs = hero.nowProgramStartMs,
                    endMs = hero.nowProgramEndMs
                )
            }
        }
        hero.nextProgramTitle?.let { nextTitle ->
            Spacer(modifier = Modifier.height(Dimens.Space2))
            Text(
                text = "Next: $nextTitle",
                style = MaterialTheme.typography.bodyMedium,
                color = OnVideo.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val HeroProgressBarHeight = 4.dp

/**
 * Live program-progress bar: how far into the currently-airing program we
 * are, from EPG start/end times. Ticks once a minute (no continuous
 * animation) — this is a live signal, never a saved playback position.
 */
@Composable
private fun HeroProgressBar(startMs: Long, endMs: Long) {
    var nowMillis by remember(startMs, endMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startMs, endMs) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000L)
        }
    }
    val fraction = if (endMs <= startMs) 0f
    else ((nowMillis - startMs).toFloat() / (endMs - startMs)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroProgressBarHeight)
            .clip(ShapeSmall)
            .background(OnVideo.copy(alpha = 0.24f))
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(ShapeSmall)
                    .background(Amber)
            )
        }
    }
}

/** Static hero backdrop: channel thumbnail (preferred) or logo on solid [Void900]. */
@Composable
private fun HeroBackdrop(
    channel: ChannelUiModel,
    heroHeight: Dp,
    safeMargin: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val (widthPx, heightPx) = remember(density, screenWidthDp, heroHeight) {
        with(density) { screenWidthDp.dp.roundToPx() to heroHeight.roundToPx() }
    }
    val placeholderPainter = remember { ColorPainter(Void800) }

    var thumbnailFile by remember(channel.id, channel.thumbnailPath) { mutableStateOf<File?>(null) }
    LaunchedEffect(channel.id, channel.thumbnailPath) {
        thumbnailFile = withContext(Dispatchers.IO) {
            channel.thumbnailPath?.let { path -> File(path).takeIf { it.exists() } }
        }
    }

    val file = thumbnailFile
    if (file != null) {
        AsyncImage(
            model = remember(file) {
                ImageRequest.Builder(context)
                    .data(file)
                    .size(widthPx, heightPx)
                    // Thumbnails are opaque JPEG video frames — RGB_565 halves memory.
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            modifier = modifier.fillMaxSize()
        )
    } else {
        // No captured frame: a large, faint category-icon watermark on the right
        // keeps the hero from reading as empty black. The channel logo (if any)
        // sits on top of it.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Icon(
                imageVector = categoryIcon(channel.category),
                contentDescription = null,
                tint = categoryColor(channel.category).copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(1f)
                    .padding(end = safeMargin)
            )
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = remember(channel.logoUrl) {
                        ImageRequest.Builder(context)
                            .data(channel.logoUrl)
                            .size(heightPx, heightPx)
                            .build()
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.7f)
                        .fillMaxWidth(0.35f)
                        .padding(end = safeMargin)
                )
            }
        }
    }
}

@Composable
private fun WatchNowButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    reduceMotion: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(DURATION_FAST, easing = EaseOutQuart),
        label = "watchNowScale"
    )

    // Near-black label on the amber (focused) fill, white on the dark (resting)
    // fill — set explicitly on content so it can't be diluted by content-color
    // propagation inside the button.
    val labelColor = if (isFocused) Void950 else OnVideo

    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = if (isFocused) BorderStroke(2.dp, FocusBorder) else null,
        colors = if (isFocused) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Void950
            )
        } else {
            ButtonDefaults.buttonColors(containerColor = Void800, contentColor = OnVideo)
        },
        contentPadding = PaddingValues(horizontal = Dimens.Space5, vertical = Dimens.Space3),
        modifier = Modifier
            .focusRequester(focusRequester)
            // The hero is a fixed-height box and this button is the bottom-anchored
            // Column's last child; unbounded height lets it measure at its natural
            // size instead of being squeezed (which was clipping the label).
            .wrapContentHeight(unbounded = true)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = labelColor,
            modifier = Modifier.size(Dimens.IconMedium)
        )
        Spacer(modifier = Modifier.width(Dimens.Space2))
        Text(
            text = "Watch now",
            // Use the Manrope label style (not the tall display face, whose caps
            // get clipped inside a button's constrained content row).
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            maxLines = 1
        )
    }
}
