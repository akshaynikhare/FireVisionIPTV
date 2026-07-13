package com.cadnative.firevisioniptv.presentation.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceDark
import com.cadnative.firevisioniptv.presentation.ui.theme.TextSecondary

// Flush, square-topped sheet — edge-to-edge, no rounded corner or drag handle.
private val PanelShape = RoundedCornerShape(0.dp)

@Composable
internal fun BottomChannelPanel(
    currentChannel: ChannelUiModel?,
    lastChannel: ChannelUiModel?,
    channels: List<ChannelUiModel>,
    categories: List<String>,
    selectedCategory: String?,
    isLoading: Boolean,
    isVisible: Boolean,
    onChannelClick: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMobile = isMobileDevice(LocalContext.current)
    val channelListState = rememberLazyListState()
    val categoryFocusRequester = remember { FocusRequester() }
    val channelFocusRequester = remember { FocusRequester() }

    // Pin the last-watched channel first for quick recall
    val pinnedLast = lastChannel?.takeIf { it.id != currentChannel?.id }
    val displayChannels = if (pinnedLast != null) {
        listOf(pinnedLast) + channels.filter { it.id != pinnedLast.id }
    } else {
        channels
    }

    // Auto-scroll to the current channel when overlay appears or channels change
    val currentIndex = displayChannels.indexOfFirst { it.id == currentChannel?.id }
    LaunchedEffect(displayChannels, currentChannel?.id, isVisible) {
        if (isVisible && currentIndex >= 0) {
            channelListState.scrollToItem(
                index = maxOf(0, currentIndex - 1), // show one before for context
                scrollOffset = 0
            )
        }
    }

    // Focus the channel row (scrolled to the current channel) so switching is one press away;
    // fall back to category chips when there are no channels yet
    LaunchedEffect(categories, displayChannels, isVisible) {
        if (!isVisible) return@LaunchedEffect
        // Small delay to let the overlay animation begin and attach focus nodes
        kotlinx.coroutines.delay(100)
        try {
            if (displayChannels.isNotEmpty()) {
                channelFocusRequester.requestFocus()
            } else if (categories.isNotEmpty()) {
                categoryFocusRequester.requestFocus()
            }
        } catch (_: Exception) {
            // Focus request can fail if not yet attached
        }
    }

    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffsetY.coerceAtLeast(0f) }
            .background(color = SurfaceDark.copy(alpha = 0.95f), shape = PanelShape)
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

        Text(
            text = "Switch Channel",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
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
                    .focusProperties { down = channelFocusRequester }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            try { channelFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        } else false
                    }
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
                    .focusProperties { up = categoryFocusRequester }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            try { categoryFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        } else false
                    }
            ) {
                itemsIndexed(displayChannels, key = { _, ch -> ch.id }) { index, channel ->
                    OverlayChannelItem(
                        channel = channel,
                        isCurrentChannel = channel.id == currentChannel?.id,
                        isLastChannel = pinnedLast != null && index == 0,
                        onClick = { onChannelClick(channel.id) },
                        onFavoriteClick = { onFavoriteClick(channel.id) },
                        modifier = Modifier.animateItemEntrance(index)
                    )
                }
            }
        }
    }
}
