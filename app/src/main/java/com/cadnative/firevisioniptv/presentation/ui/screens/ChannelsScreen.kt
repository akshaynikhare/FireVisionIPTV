package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

/**
 * Channels screen with grid layout and category filtering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadChannels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Channels",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                    ChannelsGrid(
                        channels = uiState.channels,
                        onChannelClick = onChannelClick,
                        onToggleFavorite = { channelId ->
                            viewModel.toggleFavorite(channelId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelsGrid(
    channels: List<com.cadnative.firevisioniptv.presentation.model.ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(channels) { channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { onToggleFavorite(channel.id) }
            )
        }
    }
}
