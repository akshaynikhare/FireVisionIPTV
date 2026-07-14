package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.cadnative.firevisioniptv.presentation.model.GuideFilter
import com.cadnative.firevisioniptv.presentation.ui.components.CategoryChip
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor

/**
 * Category filter chips above the grid: All / ★ Favorites / one chip per category.
 * D-pad up from the grid reaches these; selecting one filters the rows in place.
 * Categories come straight from the loaded channels, so no genre is hard-coded here.
 */
@Composable
internal fun GuideFilterBar(
    categories: List<String>,
    selectedFilter: GuideFilter,
    hasFavorites: Boolean,
    onSelectFilter: (GuideFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    // Keep the selected chip in view as focus moves along the row.
    val selectedIndex = indexOfSelected(selectedFilter, categories, hasFavorites)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.scrollToItem(maxOf(0, selectedIndex - 1))
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Space1),
        contentPadding = PaddingValues(
            horizontal = if (isCompact) Dimens.ScreenPaddingHorizontalMobile
            else Dimens.ScreenPaddingHorizontalTv
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GuideFilterChipGap)
    ) {
        item("all") {
            CategoryChip(
                label = "All",
                isSelected = selectedFilter is GuideFilter.All,
                selectedContainerColor = Amber,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onSelectFilter(GuideFilter.All) },
                shape = ShapeMedium
            )
        }
        if (hasFavorites) {
            item("favorites") {
                CategoryChip(
                    label = "★ Favorites",
                    isSelected = selectedFilter is GuideFilter.Favorites,
                    selectedContainerColor = Amber,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { onSelectFilter(GuideFilter.Favorites) },
                    shape = ShapeMedium
                )
            }
        }
        items(categories.size, key = { categories[it] }) { i ->
            val category = categories[i]
            CategoryChip(
                label = category,
                isSelected = selectedFilter is GuideFilter.Category && selectedFilter.name == category,
                selectedContainerColor = categoryColor(category),
                selectedLabelColor = MaterialTheme.colorScheme.background,
                onClick = { onSelectFilter(GuideFilter.Category(category)) },
                shape = ShapeMedium
            )
        }
    }
}

private fun indexOfSelected(
    selectedFilter: GuideFilter,
    categories: List<String>,
    hasFavorites: Boolean
): Int {
    val favOffset = if (hasFavorites) 1 else 0
    return when (selectedFilter) {
        is GuideFilter.All -> 0
        is GuideFilter.Favorites -> if (hasFavorites) 1 else 0
        is GuideFilter.Category -> {
            val at = categories.indexOf(selectedFilter.name)
            if (at >= 0) at + 1 + favOffset else 0
        }
    }
}
