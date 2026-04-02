package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import android.view.KeyEvent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_ENTRANCE
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_EXIT
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.theme.*

@Composable
fun ChannelOverlay(
    isVisible: Boolean,
    currentChannel: ChannelUiModel?,
    channels: List<ChannelUiModel>,
    categories: List<String>,
    selectedCategory: String?,
    isLoadingChannels: Boolean,
    isSwitchingChannel: Boolean,
    nowProgram: EpgProgram? = null,
    nextProgram: EpgProgram? = null,
    onChannelClick: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent {
                onInteraction()
                false // don't consume — let focus system handle navigation
            }
    ) {
        // Scrim
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(DURATION_ENTRANCE, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            )
        }

        // Now Playing info bar — top
        AnimatedVisibility(
            visible = isVisible && currentChannel != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(DURATION_ENTRANCE, easing = EaseOutQuart)
            ) + fadeIn(tween(DURATION_ENTRANCE, easing = EaseOutQuart)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(DURATION_EXIT, easing = EaseOutQuart)
            ) + fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            currentChannel?.let { channel ->
                NowPlayingBar(
                    channel = channel,
                    nowProgram = nowProgram,
                    nextProgram = nextProgram
                )
            }
        }

        // Channel switching indicator
        AnimatedVisibility(
            visible = isSwitchingChannel,
            enter = fadeIn(tween(DURATION_NORMAL)),
            exit = fadeOut(tween(DURATION_NORMAL)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                color = Amber,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }

        // Bottom panel — slides up
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(DURATION_ENTRANCE, easing = EaseOutQuart)
            ) + fadeIn(tween(DURATION_ENTRANCE, easing = EaseOutQuart)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(DURATION_EXIT, easing = EaseOutQuart)
            ) + fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomChannelPanel(
                currentChannel = currentChannel,
                channels = channels,
                categories = categories,
                selectedCategory = selectedCategory,
                isLoading = isLoadingChannels,
                isVisible = isVisible,
                onChannelClick = onChannelClick,
                onCategorySelected = onCategorySelected,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}

private val epgTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatEpgTime(program: EpgProgram): String {
    val zone = ZoneId.systemDefault()
    val start = program.startTime.atZone(zone).format(epgTimeFormatter)
    val end = program.endTime.atZone(zone).format(epgTimeFormatter)
    return "$start-$end"
}

@Composable
private fun NowPlayingBar(
    channel: ChannelUiModel,
    nowProgram: EpgProgram?,
    nextProgram: EpgProgram?,
    modifier: Modifier = Modifier
) {
    val catColor = categoryColor(channel.category)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Channel logo
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    modifier = Modifier.size(40.dp)
                ) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Channel name
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Category pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = catColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = channel.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = catColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // "LIVE" badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Error.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // EPG Now/Next rows
            if (nowProgram != null) {
                Text(
                    text = "Now: ${nowProgram.title}  ${formatEpgTime(nowProgram)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (nextProgram != null) {
                Text(
                    text = "Next: ${nextProgram.title}  ${formatEpgTime(nextProgram)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BottomChannelPanel(
    currentChannel: ChannelUiModel?,
    channels: List<ChannelUiModel>,
    categories: List<String>,
    selectedCategory: String?,
    isLoading: Boolean,
    isVisible: Boolean,
    onChannelClick: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val channelListState = rememberLazyListState()
    val categoryFocusRequester = remember { FocusRequester() }
    val channelFocusRequester = remember { FocusRequester() }

    // Auto-scroll to the current channel when overlay appears
    val currentIndex = channels.indexOfFirst { it.id == currentChannel?.id }
    LaunchedEffect(channels, currentChannel?.id) {
        if (currentIndex >= 0) {
            channelListState.scrollToItem(
                index = maxOf(0, currentIndex - 1), // show one before for context
                scrollOffset = 0
            )
        }
    }

    // Request focus on the category chips when overlay data arrives or visibility changes
    LaunchedEffect(categories, channels, isVisible) {
        if (!isVisible) return@LaunchedEffect
        // Small delay to let the overlay animation begin and attach focus nodes
        kotlinx.coroutines.delay(100)
        try {
            if (categories.isNotEmpty()) {
                categoryFocusRequester.requestFocus()
            } else if (channels.isNotEmpty()) {
                channelFocusRequester.requestFocus()
            }
        } catch (_: Exception) {
            // Focus request can fail if not yet attached
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SurfaceDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(
                    TextDim.copy(alpha = 0.4f),
                    RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Section title
        Text(
            text = "Switch Channel",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category chips — focusable for TV D-Pad navigation
        if (categories.isNotEmpty()) {
            OverlayCategoryChips(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier
                    .focusRequester(categoryFocusRequester)
                    .focusProperties { down = channelFocusRequester }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            try { channelFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        } else false
                    }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Channel cards row
        if (isLoading && channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Amber,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            LazyRow(
                state = channelListState,
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .focusRequester(channelFocusRequester)
                    .focusProperties { up = categoryFocusRequester }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            try { categoryFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        } else false
                    }
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                    val isCurrentChannel = channel.id == currentChannel?.id
                    OverlayChannelItem(
                        channel = channel,
                        isCurrentChannel = isCurrentChannel,
                        onClick = { onChannelClick(channel.id) },
                        onFavoriteClick = { onFavoriteClick(channel.id) },
                        modifier = Modifier.animateItemEntrance(index)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayCategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // "All" chip
        item {
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.08f else 1f,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "allChipScale"
            )
            val borderStroke: BorderStroke? = when {
                isFocused -> BorderStroke(2.dp, FocusBorder)
                selectedCategory != null -> BorderStroke(1.dp, SubtleBorder)
                else -> null
            }
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = {
                    Text(
                        text = "All",
                        fontWeight = if (selectedCategory == null) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Amber,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = borderStroke,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }

        items(categories) { category ->
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.08f else 1f,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "chipScale_$category"
            )
            val isSelected = selectedCategory == category
            val catColor = categoryColor(category)
            val borderStroke: BorderStroke? = when {
                isFocused -> BorderStroke(2.dp, FocusBorder)
                !isSelected -> BorderStroke(1.dp, SubtleBorder)
                else -> null
            }
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = catColor,
                    selectedLabelColor = MaterialTheme.colorScheme.background
                ),
                border = borderStroke,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }
    }
}

@Composable
private fun OverlayChannelItem(
    channel: ChannelUiModel,
    isCurrentChannel: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.width(180.dp).height(110.dp)) {
        ChannelCard(
            channel = channel,
            onClick = onClick,
            onFavoriteClick = onFavoriteClick,
            modifier = Modifier.fillMaxSize()
        )

        // "NOW" badge for the currently playing channel
        if (isCurrentChannel) {
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 8.dp),
                color = Amber,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "NOW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
