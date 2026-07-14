package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.SearchUiState
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.ChannelCard
import com.cadnative.firevisioniptv.presentation.ui.components.ErrorState
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens

/**
 * The search results pane — a [Crossfade] over the six query states
 * (loading / error / recent / prompt / no-results / results). Shared by the
 * mobile layout (full width) and the TV split layout (right column); the
 * caller controls placement via [modifier].
 */
@Composable
internal fun SearchResultsArea(
    uiState: SearchUiState,
    searchQuery: String,
    isMobile: Boolean,
    onChannelClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onMultiviewClick: (String) -> Unit,
    onRetry: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    keyboardController: SoftwareKeyboardController?,
    modifier: Modifier = Modifier
) {
    val searchState = when {
        uiState.isLoading -> "loading"
        uiState.error != null -> "error"
        searchQuery.isBlank() && uiState.recentSearches.isNotEmpty() -> "recent"
        searchQuery.isBlank() -> "prompt"
        uiState.results.isEmpty() -> "no_results"
        else -> "results"
    }

    Crossfade(
        targetState = searchState,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "searchState",
        modifier = modifier
    ) { state ->
        when (state) {
            "loading" -> SearchResultsSkeleton(
                showShimmer = !LocalPerfProfile.current.reduceMotion
            )
            "error" -> ErrorState(
                message = uiState.error ?: "Search failed",
                onRetry = onRetry
            )
            "recent" -> RecentSearches(
                searches = uiState.recentSearches,
                onSearchClick = onRecentSearchClick,
                onClearHistory = onClearHistory
            )
            "prompt" -> SearchPrompt()
            "no_results" -> NoResultsState(query = searchQuery)
            else -> {
                if (isMobile) {
                    LaunchedEffect(Unit) { keyboardController?.hide() }
                }
                Column {
                    Text(
                        text = "${uiState.results.size} result${if (uiState.results.size != 1) "s" else ""} for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = Dimens.RowTitleGap)
                    )
                    // Category-match hint: when the query matches a category name,
                    // surface the matched categories so the user can recognize a
                    // whole-genre match rather than only per-channel name matches.
                    val matchedCategories = remember(uiState.results, searchQuery) {
                        val q = searchQuery.trim()
                        if (q.isBlank()) emptyList()
                        else uiState.results
                            .map { it.category }
                            .filter { it.isNotBlank() && it.contains(q, ignoreCase = true) }
                            .distinct()
                            .take(4)
                    }
                    if (matchedCategories.isNotEmpty()) {
                        CategoryMatchHints(
                            categories = matchedCategories,
                            modifier = Modifier.padding(bottom = Dimens.RowTitleGap)
                        )
                    }
                    val screenWidthDp = LocalConfiguration.current.screenWidthDp
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (screenWidthDp < 600) 100.dp else 140.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GridGap),
                        verticalArrangement = Arrangement.spacedBy(Dimens.GridGap)
                    ) {
                        itemsIndexed(uiState.results, key = { _, channel -> channel.id }) { index, channel ->
                            ChannelCard(
                                channel = channel,
                                onClick = { onChannelClick(channel.id) },
                                onFavoriteClick = { onFavoriteClick(channel.id) },
                                onMultiviewClick = { onMultiviewClick(channel.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.GridCardHeight)
                                    .animateItemEntrance(index)
                            )
                        }
                    }
                }
            }
        }
    }
}
