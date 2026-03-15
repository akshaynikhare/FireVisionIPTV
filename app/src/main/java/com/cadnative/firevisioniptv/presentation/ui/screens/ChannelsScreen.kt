package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.SubtleBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    initialCategory: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCategory) {
        viewModel.loadChannels(initialCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.selectedCategory ?: "All Channels",
                        style = MaterialTheme.typography.headlineMedium,
                        color = uiState.selectedCategory?.let { categoryColor(it) }
                            ?: MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    if (uiState.selectedCategory != null) {
                        IconButton(onClick = { viewModel.loadChannels(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.categories.isNotEmpty()) {
                CategoryChips(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.loadChannels(category)
                    },
                    onAllSelected = {
                        viewModel.loadChannels(null)
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
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
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    onAllSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = onAllSelected,
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
                border = if (selectedCategory != null) BorderStroke(1.dp, SubtleBorder) else null,
                shape = RoundedCornerShape(8.dp)
            )
        }
        items(categories) { category ->
            val catColor = categoryColor(category)
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        fontWeight = if (selectedCategory == category) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = catColor,
                    selectedLabelColor = MaterialTheme.colorScheme.background
                ),
                border = if (selectedCategory != category) BorderStroke(1.dp, SubtleBorder) else null,
                shape = RoundedCornerShape(8.dp)
            )
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
        columns = GridCells.Adaptive(minSize = 200.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
