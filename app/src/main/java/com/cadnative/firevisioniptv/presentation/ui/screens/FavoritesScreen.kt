package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.model.PopularCategoryUiModel
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val hasAnyContent = uiState.favorites.isNotEmpty() || uiState.favoriteCategories.isNotEmpty()
            val contentState = when {
                uiState.isLoading && !hasAnyContent -> "loading"
                uiState.error != null && !hasAnyContent -> "error"
                !hasAnyContent -> "empty"
                else -> "content"
            }

            Crossfade(
                targetState = contentState,
                animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                label = "favoritesState"
            ) { state ->
                when (state) {
                    "loading" -> LoadingIndicator(message = "Loading favorites...")
                    "error" -> ErrorState(
                        message = uiState.error ?: "Failed to load favorites",
                        onRetry = { viewModel.retryLoadFavorites() }
                    )
                    "empty" -> EmptyState(message = "No favorites yet")
                    else -> FavoritesContent(
                        favorites = uiState.favorites,
                        favoriteCategories = uiState.favoriteCategories,
                        onChannelClick = onChannelClick,
                        onCategoryClick = onCategoryClick,
                        onRemoveFavorite = { channelId ->
                            viewModel.removeFavorite(channelId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesContent(
    favorites: List<com.cadnative.firevisioniptv.presentation.model.ChannelUiModel>,
    favoriteCategories: List<PopularCategoryUiModel>,
    onChannelClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Favorite Categories section
        if (favoriteCategories.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(favoriteCategories, key = { it.name }) { category ->
                            CategoryCard(
                                name = category.name,
                                channelCount = category.channelCount,
                                imageUrl = category.imageUrl,
                                isFavorite = true,
                                onClick = { onCategoryClick(category.name) },
                                subtitle = "${category.channelCount} channels",
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(100.dp)
                            )
                        }
                    }
                    if (favorites.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Channels",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Favorite Channels grid
        itemsIndexed(favorites, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { onRemoveFavorite(channel.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .animateItemEntrance(index)
            )
        }
    }
}
