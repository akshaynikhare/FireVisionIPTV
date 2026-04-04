package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.theme.*
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
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
                        "empty" -> EmptyState(
                            message = "No channels available",
                            onRetry = { viewModel.refresh() }
                        )
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
    val listState = rememberLazyListState()

    // Auto-scroll to keep the selected category visible
    val selectedIndex = if (selectedCategory == null) 0
        else categories.indexOf(selectedCategory).let { if (it >= 0) it + 1 else 0 }
    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(maxOf(0, selectedIndex - 1))
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.12f else 1f,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "allChipScale"
            )
            val isSelected = selectedCategory == null
            val borderStroke: BorderStroke? = when {
                isFocused -> BorderStroke(2.5.dp, FocusBorder)
                !isSelected -> BorderStroke(1.dp, subtleBorder)
                else -> null
            }
            FilterChip(
                selected = isSelected,
                onClick = onAllSelected,
                label = {
                    Text(
                        text = "All",
                        fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isFocused && !isSelected -> Amber
                            else -> Color.Unspecified
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Amber,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = if (isFocused) Amber.copy(alpha = 0.15f) else Color.Transparent
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
                targetValue = if (isFocused) 1.12f else 1f,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "chipScale_$category"
            )
            val isSelected = selectedCategory == category
            val catColor = categoryColor(category)
            val borderStroke: BorderStroke? = when {
                isFocused -> BorderStroke(2.5.dp, FocusBorder)
                !isSelected -> BorderStroke(1.dp, subtleBorder)
                else -> null
            }
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isFocused && !isSelected -> Amber
                            else -> Color.Unspecified
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = catColor,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                    containerColor = if (isFocused) Amber.copy(alpha = 0.15f) else Color.Transparent
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
private fun ChannelsGrid(
    channels: List<com.cadnative.firevisioniptv.presentation.model.ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { onToggleFavorite(channel.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .animateItemEntrance(index)
            )
        }
    }
}
