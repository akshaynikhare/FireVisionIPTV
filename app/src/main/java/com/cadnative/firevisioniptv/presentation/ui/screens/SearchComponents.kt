package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.animation.animateFadeIn
import com.cadnative.firevisioniptv.presentation.ui.components.ChannelGridSkeleton
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Void700
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

/** Chips that flag categories whose name matches the query — a "matches genre" cue. */
@Composable
internal fun CategoryMatchHints(
    categories: List<String>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        items(categories, key = { it }) { category ->
            val catColor = categoryColor(category)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = catColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "in $category",
                    style = MaterialTheme.typography.labelMedium,
                    color = catColor,
                    modifier = Modifier.padding(horizontal = Dimens.Space3, vertical = Dimens.Space1)
                )
            }
        }
    }
}

@Composable
internal fun SearchResultsSkeleton(
    showShimmer: Boolean,
    modifier: Modifier = Modifier
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompact = screenWidthDp < 600
    val cardWidth = if (isCompact) 100.dp else 140.dp
    val cellWidthDp = if (isCompact) 116 else 156
    val columns = (screenWidthDp / cellWidthDp).coerceAtLeast(2)

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(16.dp)
                .background(Void700, MaterialTheme.shapes.extraSmall)
        )
        Spacer(modifier = Modifier.height(Dimens.RowTitleGap))
        ChannelGridSkeleton(
            columns = columns,
            rows = 3,
            cardWidth = cardWidth,
            cardHeight = Dimens.GridCardHeight,
            showShimmer = showShimmer
        )
    }
}

@Composable
internal fun SearchPrompt(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .animateFadeIn(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Find your channels",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Search by name or category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun RecentSearches(
    searches: List<String>,
    onSearchClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onClearHistory) {
                Text(
                    text = "Clear",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(searches, key = { it }) { search ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, subtleBorder),
                    modifier = Modifier.clickable { onSearchClick(search) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                        Text(
                            text = search,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoResultsState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .animateFadeIn(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try a different search term",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun TvSearchFieldPlaceholder(
    query: String,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onActivate,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.secondary else subtleBorder
        ),
        modifier = modifier
            .height(56.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.IconMedium)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = query.ifEmpty { "Search channels, categories..." },
                style = MaterialTheme.typography.bodyLarge,
                color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
