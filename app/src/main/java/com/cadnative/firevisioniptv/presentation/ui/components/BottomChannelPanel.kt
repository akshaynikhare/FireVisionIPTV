package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

// Flush, square-topped sheet — edge-to-edge, no rounded corner or drag handle.
private val PanelShape = RoundedCornerShape(0.dp)

// How long the one-shot scroll/focus effects wait for the channel list to load
private const val CONTENT_WAIT_TIMEOUT_MS = 2_000L

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BottomChannelPanel(
    currentChannel: ChannelUiModel?,
    recentChannels: List<ChannelUiModel>,
    overlayEpg: Map<String, Pair<EpgProgram?, EpgProgram?>>,
    channels: List<ChannelUiModel>,
    categories: List<String>,
    selectedCategory: String?,
    isLoading: Boolean,
    isVisible: Boolean,
    onChannelClick: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val isMobile = isMobileDevice(LocalContext.current)
    val channelListState = rememberLazyListState()
    val categoryFocusRequester = remember { FocusRequester() }
    val channelFocusRequester = remember { FocusRequester() }
    // One shared ticker for every progress bar in the panel — per-card tickers
    // are N un-batched wakeups/minute on a low-end box
    val nowMillis = rememberMinuteTicker()

    // Pin the recently watched channels first for quick recall (most recent first)
    val pinnedRecents = remember(recentChannels, currentChannel?.id) {
        recentChannels.filter { it.id != currentChannel?.id }
    }
    val displayChannels = remember(pinnedRecents, channels) {
        if (pinnedRecents.isNotEmpty()) {
            val pinnedIds = pinnedRecents.mapTo(HashSet()) { it.id }
            pinnedRecents + channels.filter { it.id !in pinnedIds }
        } else {
            channels
        }
    }

    // What the detail strip describes: the focused card, else the playing channel
    var focusedChannel by remember { mutableStateOf<ChannelUiModel?>(null) }
    val stripChannel = focusedChannel ?: currentChannel

    val latestChannels by rememberUpdatedState(displayChannels)
    val latestCategories by rememberUpdatedState(categories)

    // One-shot scroll to the current channel per overlay open / category switch.
    // Deliberately NOT keyed on list content: EPG ticks, Room re-emissions, and
    // favorite toggles must never yank the scroll position mid-browse.
    LaunchedEffect(isVisible, selectedCategory) {
        if (!isVisible) return@LaunchedEffect
        withTimeoutOrNull(CONTENT_WAIT_TIMEOUT_MS) {
            snapshotFlow { latestChannels.isNotEmpty() }.first { it }
        } ?: return@LaunchedEffect
        val index = latestChannels.indexOfFirst { it.id == currentChannel?.id }
        if (index >= 0) {
            channelListState.scrollToItem(
                index = maxOf(0, index - 1), // show one before for context
                scrollOffset = 0
            )
        }
    }

    // One-shot initial focus per overlay open — after this, focus moves only
    // when the user moves it (a category click keeps focus on the chip).
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            focusedChannel = null // stale strip target from the last session
            return@LaunchedEffect
        }
        withTimeoutOrNull(CONTENT_WAIT_TIMEOUT_MS) {
            snapshotFlow { latestChannels.isNotEmpty() || latestCategories.isNotEmpty() }.first { it }
        } ?: return@LaunchedEffect
        // Small delay to let the overlay animation begin and attach focus nodes
        delay(100)
        runCatching {
            if (latestChannels.isNotEmpty()) channelFocusRequester.requestFocus()
            else categoryFocusRequester.requestFocus()
        }
    }

    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffsetY.coerceAtLeast(0f) }
            .background(color = SurfaceDark.copy(alpha = 0.95f), shape = PanelShape)
            // Hard focus trap: while the overlay is open, D-pad can never dump
            // focus onto the invisible root or a hidden bar behind it. BACK
            // still bubbles to the root handler for dismissal.
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup()
            .padding(top = 12.dp, bottom = 18.dp)
    ) {
        // Mobile-only swipe/tap-to-dismiss strip — no visual handle and kept thin
        // so it doesn't waste vertical space (TV has no touch, so it's omitted).
        if (isMobile) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clickable { currentOnDismiss() }
                    .pointerInput(Unit) {
                        val thresholdPx = 80.dp.toPx()
                        detectVerticalDragGestures(
                            onDragStart = { dragOffsetY = 0f },
                            onDragEnd = {
                                if (dragOffsetY > thresholdPx) currentOnDismiss()
                                dragOffsetY = 0f
                            },
                            onDragCancel = { dragOffsetY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    }
            )
        }

        OverlayDetailStrip(
            channel = stripChannel,
            epg = overlayEpgKey(stripChannel?.tvgId)?.let { overlayEpg[it] },
            nowMillis = nowMillis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category chips — focusable for TV D-Pad navigation
        if (categories.isNotEmpty()) {
            OverlayCategoryChips(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier
                    .focusRequester(categoryFocusRequester)
                    .focusRestorer()
                    .focusProperties { down = channelFocusRequester }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isLoading && displayChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OverlayChannelCardHeight),
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
                    .focusRestorer()
                    // ▲ goes to the chips only when they exist — with no
                    // categories it stays a harmless wall instead of a dead key
                    .focusProperties {
                        up = if (categories.isNotEmpty()) categoryFocusRequester
                        else FocusRequester.Default
                    }
            ) {
                itemsIndexed(displayChannels, key = { _, ch -> ch.id }) { index, channel ->
                    OverlayChannelItem(
                        channel = channel,
                        isCurrentChannel = channel.id == currentChannel?.id,
                        nowMillis = nowMillis,
                        recentIndex = index.takeIf { it < pinnedRecents.size },
                        onClick = { onChannelClick(channel.id) },
                        onFavoriteClick = { onFavoriteClick(channel.id) },
                        onFocused = { focusedChannel = channel }
                    )
                }
            }
        }
    }
}
