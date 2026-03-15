package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
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
                val contentState = when {
                    uiState.isLoading && uiState.channels.isEmpty() -> "loading"
                    uiState.error != null && uiState.channels.isEmpty() -> "error"
                    uiState.channels.isEmpty() -> "empty"
                    else -> "content"
                }

                Crossfade(
                    targetState = contentState,
                    animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                    label = "channelsState"
                ) { state ->
                    when (state) {
                        "loading" -> LoadingIndicator(message = "Loading channels...")
                        "error" -> ErrorState(
                            message = uiState.error ?: "Unknown error",
                            onRetry = { viewModel.refresh() }
                        )
                        "empty" -> EmptyState(message = "No channels available")
                        else -> ChannelsGrid(
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
            val borderColor by animateColorAsState(
                targetValue = if (selectedCategory != null) SubtleBorder else Color.Transparent,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "allChipBorder"
            )
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
                border = if (selectedCategory != null) BorderStroke(1.dp, borderColor) else null,
                shape = RoundedCornerShape(8.dp)
            )
        }
        items(categories) { category ->
            val catColor = categoryColor(category)
            val isSelected = selectedCategory == category
            val borderColor by animateColorAsState(
                targetValue = if (!isSelected) SubtleBorder else Color.Transparent,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "chipBorder_$category"
            )
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
                border = if (!isSelected) BorderStroke(1.dp, borderColor) else null,
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
        itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { onToggleFavorite(channel.id) },
                modifier = Modifier.animateItemEntrance(index)
            )
        }
    }
}
