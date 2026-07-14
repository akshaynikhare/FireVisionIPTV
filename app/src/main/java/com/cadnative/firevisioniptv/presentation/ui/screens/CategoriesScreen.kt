package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@Composable
fun CategoriesScreen(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    LaunchedEffect(Unit) {
        viewModel.loadChannels()
    }

    ScreenScaffold(title = "Categories", modifier = modifier) {
        val contentState = when {
            uiState.isLoading && uiState.categories.isEmpty() -> "loading"
            uiState.error != null && uiState.categories.isEmpty() -> "error"
            uiState.categories.isEmpty() -> "empty"
            else -> "content"
        }

        Crossfade(
            targetState = contentState,
            animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
            label = "categoriesState"
        ) { state ->
            when (state) {
                "loading" -> LoadingIndicator(message = "Loading categories...")
                "error" -> ErrorState(
                    message = uiState.error ?: "Failed to load categories",
                    onRetry = { viewModel.loadChannels() }
                )
                "empty" -> EmptyState(
                    message = "No categories available",
                    onRetry = { viewModel.loadChannels() }
                )
                else -> {
                    val categoriesData = remember(uiState.channels) {
                        uiState.channels
                            .groupBy { it.category.ifBlank { "Other" } }
                            .map { (name, channels) ->
                                Triple(
                                    name,
                                    channels.size,
                                    channels.firstOrNull { it.thumbnailPath != null }?.thumbnailPath
                                        ?: channels.firstOrNull { it.logoUrl != null }?.logoUrl
                                )
                            }
                            .sortedBy { it.first }
                    }

                    LazyVerticalGrid(
                        columns = if (isCompact) GridCells.Fixed(2)
                                  else GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = if (isCompact) Dimens.ScreenPaddingHorizontalMobile
                                    else Dimens.ScreenPaddingHorizontalTv,
                            end = if (isCompact) Dimens.ScreenPaddingHorizontalMobile
                                  else Dimens.ScreenPaddingHorizontalTv,
                            top = 0.dp,
                            bottom = if (isCompact) Dimens.ScreenPaddingVerticalMobile
                                     else Dimens.ScreenPaddingVertical
                        ),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (isCompact) Dimens.CategoryCardGap else Dimens.GridGap
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            if (isCompact) Dimens.CategoryCardGap else Dimens.GridGap
                        )
                    ) {
                        itemsIndexed(categoriesData) { index, (category, count, imageUrl) ->
                            CategoryCard(
                                name = category,
                                channelCount = count,
                                imageUrl = imageUrl,
                                isFavorite = category in uiState.favoriteCategoryNames,
                                onClick = { onCategoryClick(category) },
                                onToggleFavorite = { viewModel.toggleCategoryFavorite(category) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isCompact) Dimens.CategoryCardHeightMobile else 130.dp)
                                    .animateItemEntrance(index)
                            )
                        }
                    }
                }
            }
        }
    }
}
