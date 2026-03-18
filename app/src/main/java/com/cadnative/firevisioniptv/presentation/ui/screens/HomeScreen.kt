package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
    val cardWidth = (screenWidth * 0.28f).coerceIn(200.dp, 400.dp)
    val cardHeight = (cardWidth * 0.56f).coerceIn(110.dp, 224.dp)

    Column(modifier = modifier.padding(horizontal = 40.dp)) {
        Text(
            text = "Featured",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
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
                PopularCategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category.name) }
                )
            }
        }
    }
}

@Composable
private fun PopularCategoryCard(
    category: PopularCategoryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = EaseOutQuart),
        label = "popularCatScale"
    )
    val catColor = categoryColor(category.name)
    val catIcon = categoryIcon(category.name)

    Card(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .height(100.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused },
        shape = MaterialTheme.shapes.medium,
        border = when {
            isFocused -> BorderStroke(2.dp, FocusBorder)
            else -> BorderStroke(1.dp, SubtleBorder)
        },
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Color gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                catColor.copy(alpha = 0.18f),
                                catColor.copy(alpha = 0.04f)
                            )
                        )
                    )
            )

            // Background image overlay if available
            if (category.imageUrl != null) {
                AsyncImage(
                    model = category.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.25f)
                )
            }

            // Category icon — top-right, decorative
            Icon(
                imageVector = catIcon,
                contentDescription = null,
                tint = catColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(24.dp)
            )

            // Category accent line at top
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(catColor.copy(alpha = 0.8f))
            )

            // Favorite badge
            if (category.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    tint = Amber,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(16.dp)
                )
            }

            // Text content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = catColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category.channelCount} live",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary.copy(alpha = EmphasisMedium)
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
