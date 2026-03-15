package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@Composable
fun HomeScreen(
    onNavigateToChannels: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadChannels()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.channels.isEmpty() -> {
                LoadingIndicator(message = "Loading channels...")
            }
            uiState.error != null && uiState.channels.isEmpty() -> {
                ErrorState(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() }
                )
            }
            uiState.channels.isEmpty() -> {
                EmptyState(message = "No channels available")
            }
            else -> {
                HomeContent(
                    channels = uiState.channels,
                    onChannelClick = onChannelClick,
                    onNavigateToChannels = onNavigateToChannels,
                    onToggleFavorite = { channelId -> viewModel.toggleFavorite(channelId) }
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    channels: List<ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onNavigateToChannels: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val channelsByCategory = remember(channels) {
        channels.groupBy { it.category.ifBlank { "Other" } }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 28.dp)
    ) {
        // Hero banner
        item {
            HeroBanner(
                channels = channels.take(5),
                onChannelClick = onChannelClick,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }

        // Category rows
        channelsByCategory.forEach { (category, categoryChannels) ->
            item(key = "category_$category") {
                ChannelRow(
                    title = category,
                    channels = categoryChannels,
                    onChannelClick = onChannelClick,
                    onSeeAllClick = { onNavigateToChannels(category) },
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.padding(bottom = 36.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    channels: List<ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) return

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = (screenWidth * 0.28f).coerceIn(200.dp, 400.dp)
    val cardHeight = (cardWidth * 0.56f).coerceIn(110.dp, 224.dp)

    Column(modifier = modifier.padding(horizontal = 40.dp)) {
        Text(
            text = "Featured",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel.id) },
                    onFavoriteClick = { onToggleFavorite(channel.id) },
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    title: String,
    channels: List<ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = categoryColor(title)

    Column(modifier = modifier.padding(horizontal = 40.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = catColor
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelMedium,
                    color = catColor.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel.id) },
                    onFavoriteClick = { onToggleFavorite(channel.id) }
                )
            }
        }
    }
}
