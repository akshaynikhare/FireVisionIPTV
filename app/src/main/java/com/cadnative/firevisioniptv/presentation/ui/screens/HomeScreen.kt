package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.cadnative.firevisioniptv.presentation.model.PopularCategoryUiModel
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.theme.*
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@Composable
fun HomeScreen(
    onNavigateToChannels: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onChannelClick: (String) -> Unit,
    onPairDevice: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ViewModel init{} already calls loadChannels() + refresh(), no need to duplicate here

    Box(modifier = modifier.fillMaxSize()) {
        val contentState = when {
            uiState.isLoading && uiState.channels.isEmpty() -> "loading"
            uiState.error != null && uiState.channels.isEmpty() -> "error"
            uiState.channels.isEmpty() -> "empty"
            else -> "content"
        }

        Crossfade(
            targetState = contentState,
            animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
            label = "homeState"
        ) { state ->
            when (state) {
                "loading" -> LoadingIndicator(message = "Loading channels...")
                "error" -> ErrorState(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() },
                    errorType = uiState.errorType,
                    onPairDevice = onPairDevice
                )
                "empty" -> EmptyState(message = "No channels available")
                else -> HomeContent(
                    channels = uiState.channels,
                    featuredChannels = uiState.featuredChannels,
                    recentlyWatched = uiState.recentlyWatched,
                    popularCategories = uiState.popularCategories,
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
    featuredChannels: List<ChannelUiModel>,
    recentlyWatched: List<ChannelUiModel>,
    popularCategories: List<PopularCategoryUiModel>,
    onChannelClick: (String) -> Unit,
    onNavigateToChannels: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val channelsByCategory = remember(channels) {
        channels.groupBy { it.category.ifBlank { "Other" } }
    }

    val categoryEntries = remember(channelsByCategory) {
        channelsByCategory.entries.toList()
    }

    // Compute stable entrance offset for category rows based on how many
    // optional sections are present (avoids mutable var during composition)
    val categoryRowOffset = remember(recentlyWatched.isNotEmpty(), popularCategories.isNotEmpty()) {
        var offset = 1 // Hero banner is always present
        if (recentlyWatched.isNotEmpty()) offset++
        if (popularCategories.isNotEmpty()) offset++
        offset
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 28.dp)
    ) {
        // Hero banner — use featured (most watched) channels, fallback to first 5
        item {
            val bannerChannels = featuredChannels.ifEmpty { channels.take(5) }
            HeroBanner(
                channels = bannerChannels,
                onChannelClick = onChannelClick,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .animateItemEntrance(index = 0)
            )
        }

        // Recently Watched row
        if (recentlyWatched.isNotEmpty()) {
            item {
                ChannelRow(
                    title = "Recently Watched",
                    channels = recentlyWatched,
                    onChannelClick = onChannelClick,
                    onSeeAllClick = { },
                    onToggleFavorite = onToggleFavorite,
                    showSeeAll = false,
                    modifier = Modifier
                        .padding(bottom = 36.dp)
                        .animateItemEntrance(index = 1)
                )
            }
        }

        // Popular Categories slider
        if (popularCategories.isNotEmpty()) {
            item {
                PopularCategoriesSlider(
                    categories = popularCategories,
                    onCategoryClick = onNavigateToChannels,
                    modifier = Modifier
                        .padding(bottom = 36.dp)
                        .animateItemEntrance(index = categoryRowOffset - 1)
                )
            }
        }

        // Category rows — staggered entrance
        itemsIndexed(
            items = categoryEntries,
            key = { _, entry -> "category_${entry.key}" }
        ) { index, (category, categoryChannels) ->
            ChannelRow(
                title = category,
                channels = categoryChannels,
                onChannelClick = onChannelClick,
                onSeeAllClick = { onNavigateToChannels(category) },
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier
                    .padding(bottom = 36.dp)
                    .animateItemEntrance(index = categoryRowOffset + index)
            )
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
    val cardWidth = (screenWidth * 0.35f).coerceIn(280.dp, 500.dp)
    val cardHeight = (cardWidth * 0.56f).coerceIn(160.dp, 280.dp)

    Column(modifier = modifier.padding(horizontal = 40.dp)) {
        Text(
            text = "Featured",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(20.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
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
private fun PopularCategoriesSlider(
    categories: List<PopularCategoryUiModel>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 40.dp)) {
        Text(
            text = "Popular Categories",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Amber
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(categories, key = { it.name }) { category ->
                CategoryCard(
                    name = category.name,
                    channelCount = category.channelCount,
                    imageUrl = category.imageUrl,
                    isFavorite = category.isFavorite,
                    onClick = { onCategoryClick(category.name) },
                    subtitle = "${category.channelCount} live",
                    modifier = Modifier
                        .width(180.dp)
                        .height(100.dp)
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
    showSeeAll: Boolean = true,
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
            if (showSeeAll) {
                TextButton(onClick = onSeeAllClick) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelMedium,
                        color = catColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp)
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
