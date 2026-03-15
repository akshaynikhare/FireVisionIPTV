package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.viewmodel.FavoritesViewModel

/**
 * Favorites screen with grid layout and reordering support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Favorites are loaded in ViewModel init

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
            val contentState = when {
                uiState.isLoading && uiState.favorites.isEmpty() -> "loading"
                uiState.error != null && uiState.favorites.isEmpty() -> "error"
                uiState.favorites.isEmpty() -> "empty"
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
                    "empty" -> EmptyState(message = "No favorite channels yet")
                    else -> FavoritesGrid(
                        favorites = uiState.favorites,
                        onChannelClick = onChannelClick,
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
private fun FavoritesGrid(
    favorites: List<com.cadnative.firevisioniptv.presentation.model.ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(favorites, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { onRemoveFavorite(channel.id) },
                modifier = Modifier.animateItemEntrance(index)
            )
        }
    }
}
