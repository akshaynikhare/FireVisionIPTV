package com.cadnative.firevisioniptv.presentation.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.cadnative.firevisioniptv.data.source.remote.playlist.StreamUrlTemplate
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.presentation.ui.screens.player.VideoPlayer
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.viewmodel.MultiviewViewModel
import com.cadnative.firevisioniptv.presentation.viewmodel.MultiviewViewModel.Companion.MAX_PANES
import kotlin.math.min

/**
 * Sports multiview. A layout selector (2/3/4) sets how many streams show at once;
 * only the focused pane plays audio. Press OK on a pane to switch which channel it
 * shows. Pane count is capped (and halved on low-end boxes) for decoder safety.
 */
@Composable
fun MultiviewScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialChannelId: String? = null,
    viewModel: MultiviewViewModel = hiltViewModel()
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    // Cap panes on genuinely low-RAM boxes only (RAM, not core count, gates decoders).
    val lowRam = remember {
        (context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager)?.isLowRamDevice == true
    }
    val maxPanes = if (lowRam) 2 else MAX_PANES

    BackHandler { onNavigateBack() }

    if (channels.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No channels available for multiview",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val channelsById = remember(channels) { channels.associateBy { it.id } }
    val maxAllowed = min(channels.size, maxPanes)

    // assignments = channel id per pane; its size is the current pane count.
    var assignments by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(channels) {
        if (assignments.isEmpty()) {
            val preferred = initialChannelId?.takeIf { id -> channels.any { it.id == id } }
            assignments = if (preferred != null) {
                listOfNotNull(preferred, channels.firstOrNull { it.id != preferred }?.id)
            } else {
                channels.take(min(channels.size, 2)).map { it.id }
            }
        }
    }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var pickerForPane by remember { mutableStateOf(-1) }

    fun setCount(newCount: Int) {
        val ids = assignments.toMutableList()
        if (newCount > ids.size) {
            val cycle = channels.map { it.id }
            val used = ids.toMutableSet()
            var i = 0
            while (ids.size < newCount) {
                // prefer channels not already shown, else cycle through the list
                val next = cycle.firstOrNull { it !in used } ?: cycle[i % cycle.size]
                ids.add(next); used.add(next); i++
            }
        } else {
            while (ids.size > newCount) ids.removeAt(ids.lastIndex)
        }
        assignments = ids
        if (focusedIndex >= newCount) focusedIndex = newCount - 1
    }

    if (assignments.isEmpty()) return
    val count = assignments.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Layout selector — only when more than 2 streams are possible.
        if (maxAllowed > 2) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layout",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                (2..maxAllowed).forEach { n ->
                    LayoutChip(count = n, selected = n == count, onClick = { setCount(n) })
                }
            }
        }

        // Mobile portrait / compact width: stack every pane full-width at a natural
        // 16:9 aspect and scroll if they overflow — a wide 16:9 stream fills the pane
        // instead of letterboxing inside a tall, narrow side-by-side cell.
        // TV / landscape: keep the weighted grid (1 row for 2–3 panes, 2×2 for 4).
        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val isCompact = configuration.screenWidthDp < 600
        val stackVertically = isPortrait || isCompact

        if (stackVertically) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (0 until count).forEach { index ->
                    val channel = channelsById[assignments[index]]
                    if (channel != null) {
                        MultiviewPane(
                            channel = channel,
                            isFocused = focusedIndex == index,
                            requestInitialFocus = index == 0,
                            onFocused = { focusedIndex = index },
                            onOpenPicker = { pickerForPane = index },
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        )
                    }
                }
            }
        } else {
            val rows: List<List<Int>> = when {
                count <= 3 -> listOf((0 until count).toList())
                else -> listOf(listOf(0, 1), listOf(2, 3))
            }
            rows.forEach { rowIndices ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowIndices.forEach { index ->
                        val channel = channelsById[assignments[index]]
                        if (channel != null) {
                            MultiviewPane(
                                channel = channel,
                                isFocused = focusedIndex == index,
                                requestInitialFocus = index == 0,
                                onFocused = { focusedIndex = index },
                                onOpenPicker = { pickerForPane = index },
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickerForPane >= 0) {
        ChannelPickerOverlay(
            channels = channels,
            currentId = assignments.getOrNull(pickerForPane),
            onPick = { id ->
                assignments = assignments.toMutableList().also { it[pickerForPane] = id }
                pickerForPane = -1
            },
            onDismiss = { pickerForPane = -1 }
        )
    }
}

@Composable
private fun MultiviewPane(
    channel: Channel,
    isFocused: Boolean,
    requestInitialFocus: Boolean,
    onFocused: () -> Unit,
    onOpenPicker: () -> Unit,
    viewModel: MultiviewViewModel,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    if (requestInitialFocus) {
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }

    // One player per pane, keyed by channel — swapping the channel recreates just
    // this pane's player; the old one is released on dispose.
    val player = remember(channel.id) { viewModel.createPlayer() }
    DisposableEffect(channel.id) {
        channel.streamUrl?.let { url ->
            player.setMediaItem(
                MediaItem.Builder().setUri(StreamUrlTemplate.resolve(context, url)).build()
            )
            player.prepare()
            player.playWhenReady = true
        }
        onDispose { player.release() }
    }
    LaunchedEffect(isFocused) { player.volume = if (isFocused) 1f else 0f }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Amber else MaterialTheme.colorScheme.outline,
                shape = shape
            )
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onOpenPicker(); true
                } else false
            }
    ) {
        VideoPlayer(exoPlayer = player, onPlayerViewCreated = {}, modifier = Modifier.fillMaxSize())
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LayoutChip(count: Int, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    focused -> Amber
                    selected -> Amber.copy(alpha = 0.25f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown &&
                    (e.key == Key.DirectionCenter || e.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (focused) Color.Black else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChannelPickerOverlay(
    channels: List<Channel>,
    currentId: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { initialFocus.requestFocus() } }

    // Start at the pane's current channel — on large playlists the picker
    // otherwise opens thousands of rows away from the selection.
    val selectedIndex = remember(channels, currentId) {
        channels.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val maxPickerHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .heightIn(max = maxPickerHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = "Choose channel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            // Lazy: playlists run to thousands of channels, and composing them
            // all at once while multiple panes are decoding video kills low-end
            // boxes. Only the visible rows exist.
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, ch ->
                    PickerRow(
                        label = ch.name,
                        selected = ch.id == currentId,
                        focusRequester = if (index == selectedIndex) initialFocus else null,
                        onClick = { onPick(ch.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Amber.copy(alpha = 0.20f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown &&
                    (e.key == Key.DirectionCenter || e.key == Key.Enter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (selected) "✓  $label" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Amber else MaterialTheme.colorScheme.onSurface
        )
    }
}
