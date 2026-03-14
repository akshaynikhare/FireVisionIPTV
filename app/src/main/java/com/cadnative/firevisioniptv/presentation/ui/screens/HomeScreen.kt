package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@Composable
fun HomeScreen(
    onNavigateToChannels: () -> Unit,
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
                    categories = uiState.categories,
                    onChannelClick = onChannelClick,
                    onNavigateToChannels = onNavigateToChannels
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    channels: List<ChannelUiModel>,
    categories: List<String>,
    onChannelClick: (String) -> Unit,
    onNavigateToChannels: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Group channels by category
    val channelsByCategory = remember(channels) {
        channels.groupBy { it.category.ifBlank { "Other" } }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        // Hero banner with first few channels
        item {
            HeroBanner(
                channels = channels.take(5),
                onChannelClick = onChannelClick,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // One row per category
        channelsByCategory.forEach { (category, categoryChannels) ->
            item(key = "category_$category") {
                ChannelRow(
                    title = category,
                    channels = categoryChannels,
                    onChannelClick = onChannelClick,
                    onSeeAllClick = onNavigateToChannels,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    channels: List<ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 48.dp)) {
        Text(
            text = "Featured",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel.id) },
                    onFavoriteClick = { },
                    modifier = Modifier
                        .width(300.dp)
                        .height(180.dp)
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 48.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel.id) },
                    onFavoriteClick = { }
                )
            }
        }
    }
}
