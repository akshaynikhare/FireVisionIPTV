package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.PopularCategoryUiModel
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit = {},
    onMultiviewClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Hoisted so scroll position survives navigation to the player and back
    val gridState = rememberLazyGridState()

    ScreenScaffold(title = "Favorites", modifier = modifier) {
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
                "loading" -> ChannelsGridLoadingSkeleton()
                "error" -> ErrorState(
                    message = uiState.error ?: "Failed to load favorites",
                    onRetry = { viewModel.retryLoadFavorites() }
                )
                "empty" -> EmptyState(message = "No favorites yet")
                else -> FavoritesContent(
                    favorites = uiState.favorites,
                    favoriteCategories = uiState.favoriteCategories,
                    gridState = gridState,
                    onChannelClick = onChannelClick,
                    onCategoryClick = onCategoryClick,
                    onRemoveFavorite = viewModel::removeFavorite,
                    onMultiviewClick = onMultiviewClick
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FavoritesContent(
    favorites: List<ChannelUiModel>,
    favoriteCategories: List<PopularCategoryUiModel>,
    gridState: LazyGridState,
    onChannelClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onMultiviewClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val gridGap = if (isCompact) Dimens.CardGapMobile else Dimens.GridGap
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = if (isCompact) 100.dp else 140.dp),
        modifier = modifier
            .fillMaxSize()
            .focusRestorer(),
        contentPadding = PaddingValues(
            horizontal = if (isCompact) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv,
            vertical = if (isCompact) Dimens.ScreenPaddingVerticalMobile else Dimens.GridGap
        ),
        horizontalArrangement = Arrangement.spacedBy(gridGap),
        verticalArrangement = Arrangement.spacedBy(gridGap)
    ) {
        if (favoriteCategories.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader(title = "Categories", accentColor = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(if (isCompact) Dimens.RowTitleGapMobile else Dimens.RowTitleGap))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) Dimens.CardGapMobile else Dimens.CategoryCardGap)
                    ) {
                        items(favoriteCategories, key = { it.name }) { category ->
                            CategoryCard(
                                name = category.name,
                                channelCount = category.channelCount,
                                imageUrl = category.imageUrl,
                                isFavorite = true,
                                onClick = { onCategoryClick(category.name) },
                                subtitle = "${category.channelCount} " +
                                    if (category.channelCount == 1) "channel" else "channels",
                                modifier = Modifier
                                    .width(if (isCompact) Dimens.CategoryCardWidthMobile else Dimens.CategoryCardWidthTv)
                                    .height(if (isCompact) Dimens.CategoryCardHeightMobile else Dimens.CategoryCardHeightTv)
                            )
                        }
                    }
                    if (favorites.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(if (isCompact) Dimens.HeroCardGapMobile else Dimens.HeroCardGap))
                        SectionHeader(title = "Channels", accentColor = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        itemsIndexed(favorites, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { onRemoveFavorite(channel.id) },
                onMultiviewClick = { onMultiviewClick(channel.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.GridCardHeight)
                    .animateItemEntrance(index)
            )
        }
    }
}
