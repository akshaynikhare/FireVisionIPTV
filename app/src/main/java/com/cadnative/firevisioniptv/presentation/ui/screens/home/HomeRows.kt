package com.cadnative.firevisioniptv.presentation.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.PopularCategoryUiModel
import com.cadnative.firevisioniptv.presentation.ui.components.CategoryCard
import com.cadnative.firevisioniptv.presentation.ui.components.ChannelCard
import com.cadnative.firevisioniptv.presentation.ui.components.SectionHeader
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor

internal const val COMPACT_WIDTH_DP = 600

/** Responsive featured card size: fraction of screen width, clamped per form factor. */
@Composable
internal fun rememberHeroCardSize(): Pair<Dp, Dp> {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(screenWidthDp) {
        val screenWidth = screenWidthDp.dp
        val width = if (screenWidthDp < COMPACT_WIDTH_DP) {
            (screenWidth * 0.42f).coerceIn(140.dp, 220.dp)
        } else {
            (screenWidth * 0.22f).coerceIn(200.dp, 320.dp)
        }
        width to (width * 0.6f).coerceIn(90.dp, 192.dp)
    }
}

/**
 * Featured channel row beneath the hero. Focusing a card hoists it via
 * [onChannelFocused] so the hero backdrop follows D-pad browsing.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FeaturedRow(
    channels: List<ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onChannelFocused: (ChannelUiModel) -> Unit,
    focusChannelId: String?,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    val (cardWidth, cardHeight) = rememberHeroCardSize()
    val rowState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusIndex = remember(focusChannelId, channels) {
        channels.indexOfFirst { it.id == focusChannelId }
    }

    LaunchedEffect(focusIndex) {
        if (focusIndex >= 0) {
            rowState.scrollToItem(focusIndex)
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(modifier = modifier.padding(horizontal = horizontalPadding)) {
        SectionHeader(
            title = "Featured",
            accentColor = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Dimens.RowTitleGap))
        LazyRow(
            state = rowState,
            modifier = Modifier.focusRestorer(),
            // Vertical room so a focused card's scale-up + glow isn't clipped by the row.
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.HeroCardGap)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel.id) },
                    onFavoriteClick = { onToggleFavorite(channel.id) },
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .onFocusChanged { if (it.isFocused) onChannelFocused(channel) }
                        .then(
                            if (channel.id == focusChannelId) Modifier.focusRequester(focusRequester)
                            else Modifier
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PopularCategoriesSlider(
    categories: List<PopularCategoryUiModel>,
    onCategoryClick: (String) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < COMPACT_WIDTH_DP
    val cardWidth = if (isCompact) Dimens.CategoryCardWidthMobile else Dimens.CategoryCardWidthTv
    val cardHeight = if (isCompact) Dimens.CategoryCardHeightMobile else Dimens.CategoryCardHeightTv

    Column(modifier = modifier.padding(horizontal = horizontalPadding)) {
        SectionHeader(
            title = "Popular Categories",
            accentColor = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Dimens.RowTitleGap))
        LazyRow(
            state = rememberLazyListState(),
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CategoryCardGap)
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
                        .width(cardWidth)
                        .height(cardHeight)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChannelRow(
    title: String,
    channels: List<ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    focusChannelId: String?,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < COMPACT_WIDTH_DP
    val cardWidth = if (isCompact) Dimens.ChannelCardWidthMobile else Dimens.ChannelCardWidthTv
    val cardHeight = if (isCompact) Dimens.ChannelCardHeightMobile else Dimens.ChannelCardHeightTv

    val rowState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusIndex = remember(focusChannelId, channels) {
        channels.indexOfFirst { it.id == focusChannelId }
    }

    LaunchedEffect(focusIndex) {
        if (focusIndex >= 0) {
            rowState.scrollToItem(focusIndex)
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(modifier = modifier.padding(horizontal = horizontalPadding)) {
        SectionHeader(
            title = title,
            accentColor = categoryColor(title),
            onSeeAllClick = onSeeAllClick
        )
        Spacer(modifier = Modifier.height(Dimens.RowTitleGap))
        LazyRow(
            state = rowState,
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel.id) },
                    onFavoriteClick = { onToggleFavorite(channel.id) },
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .then(
                            if (channel.id == focusChannelId) Modifier.focusRequester(focusRequester)
                            else Modifier
                        )
                )
            }
        }
    }
}
