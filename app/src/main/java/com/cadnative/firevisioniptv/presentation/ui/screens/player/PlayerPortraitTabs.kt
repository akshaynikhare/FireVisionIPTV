package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.components.EpgProgressBar
import com.cadnative.firevisioniptv.presentation.ui.components.formatEpgTimeRange
import com.cadnative.firevisioniptv.presentation.ui.components.rememberMinuteTicker
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceElevated
import com.cadnative.firevisioniptv.presentation.ui.theme.TextSecondary

/**
 * Portrait bottom section: Channels (zap list with now-playing + progress per
 * row) and Schedule (today on the current channel). Tap a channel row to zap
 * without leaving the player.
 */
@Composable
internal fun PortraitTabs(
    uiState: PlayerUiState,
    zapChannels: List<ChannelUiModel>,
    onZapTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val nowMillis = rememberMinuteTicker()

    Column(modifier = modifier) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = OnVideo,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    color = Amber,
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Schedule", style = MaterialTheme.typography.labelMedium) },
                selectedContentColor = Amber,
                unselectedContentColor = TextSecondary
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Channels", style = MaterialTheme.typography.labelMedium) },
                selectedContentColor = Amber,
                unselectedContentColor = TextSecondary
            )
        }

        when (selectedTab) {
            0 -> ScheduleTab(
                programs = uiState.schedulePrograms,
                isLoading = uiState.scheduleLoading,
                hasEpgId = !uiState.channel?.tvgId.isNullOrBlank(),
                nowMillis = nowMillis
            )
            else -> ChannelsZapTab(
                channels = zapChannels,
                currentChannelId = uiState.channel?.id,
                nowMillis = nowMillis,
                onZapTo = onZapTo
            )
        }
    }
}

@Composable
private fun ChannelsZapTab(
    channels: List<ChannelUiModel>,
    currentChannelId: String?,
    nowMillis: Long,
    onZapTo: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Keep the playing channel in view when entering or after a zap
    val currentIndex = channels.indexOfFirst { it.id == currentChannelId }
    LaunchedEffect(currentIndex, channels.size) {
        if (currentIndex >= 0) listState.scrollToItem(maxOf(0, currentIndex - 1))
    }

    if (channels.isEmpty()) {
        TabEmptyState("No channels in this category")
        return
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = Dimens.Space2)
    ) {
        itemsIndexed(channels, key = { _, ch -> ch.id }) { _, channel ->
            val isCurrent = channel.id == currentChannelId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCurrent) Amber.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable { onZapTo(channel.id) }
                    .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                ) {
                    Card(
                        shape = ShapeSmall,
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        modifier = Modifier.size(Dimens.PlayerZapRowLogo)
                    ) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Dimens.Space1)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCurrent) Amber else OnVideo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        channel.nowProgramTitle?.let { title ->
                            Text(
                                text = title,
                                style = LabelToast,
                                color = OnVideo.copy(alpha = EmphasisMedium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                val startMs = channel.nowProgramStartMs
                val endMs = channel.nowProgramEndMs
                if (startMs != null && endMs != null) {
                    EpgProgressBar(
                        startMs = startMs,
                        endMs = endMs,
                        nowMillis = nowMillis,
                        modifier = Modifier.padding(
                            start = Dimens.PlayerZapRowLogo + Dimens.Space3,
                            top = Dimens.Space1
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleTab(
    programs: List<EpgProgram>,
    isLoading: Boolean,
    hasEpgId: Boolean,
    nowMillis: Long
) {
    when {
        isLoading && programs.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.Space5),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator(
                    color = Amber,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(Dimens.IconLarge)
                )
            }
            return
        }
        programs.isEmpty() -> {
            TabEmptyState(
                if (hasEpgId) "No guide data for this channel" else "This channel has no program guide"
            )
            return
        }
    }

    val listState = rememberLazyListState()
    val airingIndex = programs.indexOfFirst {
        nowMillis >= it.startTime.toEpochMilli() && nowMillis < it.endTime.toEpochMilli()
    }
    LaunchedEffect(airingIndex, programs.size) {
        if (airingIndex >= 0) listState.scrollToItem(maxOf(0, airingIndex - 1))
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = Dimens.Space2)
    ) {
        itemsIndexed(programs) { index, program ->
            val isAiring = index == airingIndex
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.Space1),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isAiring) Amber.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatEpgTimeRange(program),
                        style = LabelToast,
                        color = if (isAiring) Amber else TextSecondary,
                        modifier = Modifier.width(Dimens.Space6 * 3)
                    )
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isAiring) FontWeight.SemiBold else FontWeight.Normal,
                        color = OnVideo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isAiring) {
                    EpgProgressBar(
                        startMs = program.startTime.toEpochMilli(),
                        endMs = program.endTime.toEpochMilli(),
                        nowMillis = nowMillis
                    )
                }
                val description = program.description
                if (isAiring && !description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnVideo.copy(alpha = EmphasisMedium),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TabEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.Space5),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}
