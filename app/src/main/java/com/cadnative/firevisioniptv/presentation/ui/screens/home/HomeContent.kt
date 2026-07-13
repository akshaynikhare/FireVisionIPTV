package com.cadnative.firevisioniptv.presentation.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.PopularCategoryUiModel
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.ChannelRowSkeleton
import com.cadnative.firevisioniptv.presentation.ui.components.HomeHero
import com.cadnative.firevisioniptv.presentation.ui.components.rememberHeroHeight
import com.cadnative.firevisioniptv.presentation.ui.components.rememberShimmerBrush
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Void800
import kotlinx.coroutines.delay

private const val HERO_SWAP_DEBOUNCE_MS = 300L

@Composable
fun HomeContent(
    channels: List<ChannelUiModel>,
    featuredChannels: List<ChannelUiModel>,
    recentlyWatched: List<ChannelUiModel>,
    forYou: List<ChannelUiModel>,
    popularCategories: List<PopularCategoryUiModel>,
    lastPlayedChannelId: String?,
    onChannelClick: (String) -> Unit,
    onNavigateToChannels: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val horizontalPadding =
        if (isPortrait) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv

    val channelsByCategory = remember(channels) {
        channels.groupBy { it.category.ifBlank { "Other" } }
    }
    val categoryEntries = remember(channelsByCategory) {
        channelsByCategory.entries.toList()
    }
    val bannerChannels = remember(featuredChannels, channels) {
        featuredChannels.ifEmpty { channels.take(5) }
    }

    // Hero follows D-pad focus in the featured row, debounced so fast
    // scrubbing doesn't thrash image decodes. Defaults to the first
    // featured channel so the hero renders immediately.
    var focusedFeatured by remember(bannerChannels) { mutableStateOf(bannerChannels.firstOrNull()) }
    var heroChannel by remember(bannerChannels) { mutableStateOf(bannerChannels.firstOrNull()) }
    LaunchedEffect(focusedFeatured) {
        if (heroChannel != focusedFeatured) {
            delay(HERO_SWAP_DEBOUNCE_MS)
            heroChannel = focusedFeatured
        }
    }

    // Restore focus to the last played channel in exactly one section,
    // preferring the section closest to the top.
    val featuredFocusId = remember(lastPlayedChannelId, bannerChannels) {
        lastPlayedChannelId?.takeIf { id -> bannerChannels.any { it.id == id } }
    }
    val recentFocusId = remember(lastPlayedChannelId, featuredFocusId, recentlyWatched) {
        if (featuredFocusId != null) null
        else lastPlayedChannelId?.takeIf { id -> recentlyWatched.any { it.id == id } }
    }
    val categoryFocusId = if (featuredFocusId == null && recentFocusId == null) lastPlayedChannelId else null

    // Initial focus lands on "Watch now" so Home-load → OK starts playback.
    // Restoration to the last played channel (above) wins when it exists.
    val watchNowFocusRequester = remember { FocusRequester() }
    val hasRestoreTarget = remember(featuredFocusId, recentFocusId, categoryFocusId, channels) {
        featuredFocusId != null || recentFocusId != null ||
            (categoryFocusId != null && channels.any { it.id == categoryFocusId })
    }
    LaunchedEffect(hasRestoreTarget) {
        if (!hasRestoreTarget) runCatching { watchNowFocusRequester.requestFocus() }
    }

    // Stable entrance offset for category rows based on how many
    // optional sections are present (avoids mutable var during composition)
    val categoryRowOffset = remember(
        recentlyWatched.isNotEmpty(),
        forYou.isNotEmpty(),
        popularCategories.isNotEmpty()
    ) {
        var offset = 2 // Hero + featured row are always present
        if (recentlyWatched.isNotEmpty()) offset++
        if (forYou.isNotEmpty()) offset++
        if (popularCategories.isNotEmpty()) offset++
        offset
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Dimens.ScreenPaddingVertical)
    ) {
        item(key = "hero") {
            heroChannel?.let { hero ->
                HomeHero(
                    channel = hero,
                    onWatchNow = onChannelClick,
                    watchNowFocusRequester = watchNowFocusRequester,
                    modifier = Modifier
                        .padding(bottom = Dimens.RowGap)
                        .animateItemEntrance(index = 0)
                )
            }
        }

        item(key = "featured") {
            FeaturedRow(
                channels = bannerChannels,
                onChannelClick = onChannelClick,
                onToggleFavorite = onToggleFavorite,
                onChannelFocused = { focusedFeatured = it },
                focusChannelId = featuredFocusId,
                horizontalPadding = horizontalPadding,
                modifier = Modifier
                    .padding(bottom = Dimens.RowGap)
                    .animateItemEntrance(index = 1)
            )
        }

        if (recentlyWatched.isNotEmpty()) {
            item(key = "recently_watched") {
                ChannelRow(
                    // Live re-tune shortcut — quick jump back to channels the
                    // user was just watching (never a resumed file position).
                    title = "Recently Watched",
                    channels = recentlyWatched,
                    onChannelClick = onChannelClick,
                    onToggleFavorite = onToggleFavorite,
                    focusChannelId = recentFocusId,
                    horizontalPadding = horizontalPadding,
                    modifier = Modifier
                        .padding(bottom = Dimens.RowGap)
                        .animateItemEntrance(index = 2)
                )
            }
        }

        if (forYou.isNotEmpty()) {
            item(key = "for_you") {
                ChannelRow(
                    title = "For You",
                    channels = forYou,
                    onChannelClick = onChannelClick,
                    onToggleFavorite = onToggleFavorite,
                    focusChannelId = null,
                    horizontalPadding = horizontalPadding,
                    modifier = Modifier
                        .padding(bottom = Dimens.RowGap)
                        .animateItemEntrance(index = if (recentlyWatched.isNotEmpty()) 3 else 2)
                )
            }
        }

        if (popularCategories.isNotEmpty()) {
            item(key = "popular_categories") {
                PopularCategoriesSlider(
                    categories = popularCategories,
                    onCategoryClick = onNavigateToChannels,
                    horizontalPadding = horizontalPadding,
                    modifier = Modifier
                        .padding(bottom = Dimens.RowGap)
                        .animateItemEntrance(index = categoryRowOffset - 1)
                )
            }
        }

        itemsIndexed(
            items = categoryEntries,
            key = { _, entry -> "category_${entry.key}" }
        ) { index, (category, categoryChannels) ->
            ChannelRow(
                title = category,
                channels = categoryChannels,
                onChannelClick = onChannelClick,
                onToggleFavorite = onToggleFavorite,
                onSeeAllClick = { onNavigateToChannels(category) },
                focusChannelId = categoryFocusId,
                horizontalPadding = horizontalPadding,
                modifier = Modifier
                    .padding(bottom = Dimens.RowGap)
                    .animateItemEntrance(index = categoryRowOffset + index)
            )
        }
    }
}

/** Skeleton matching the real home layout: full-bleed hero block, featured row, one channel row. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isCompact = configuration.screenWidthDp < COMPACT_WIDTH_DP
    val horizontalPadding =
        if (isPortrait) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv
    val showShimmer = !LocalPerfProfile.current.reduceMotion
    val heroHeight = rememberHeroHeight()
    val (heroCardWidth, heroCardHeight) = rememberHeroCardSize()
    val cardWidth = if (isCompact) Dimens.ChannelCardWidthMobile else Dimens.ChannelCardWidthTv
    val cardHeight = if (isCompact) Dimens.ChannelCardHeightMobile else Dimens.ChannelCardHeightTv
    val shimmerBrush = if (showShimmer) rememberShimmerBrush() else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = Dimens.ScreenPaddingVertical)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(Void800)
        ) {
            if (shimmerBrush != null) {
                Box(modifier = Modifier.fillMaxSize().background(shimmerBrush))
            }
        }
        Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
            ChannelRowSkeleton(
                cardWidth = heroCardWidth,
                cardHeight = heroCardHeight,
                count = 4,
                showShimmer = showShimmer,
                modifier = Modifier.padding(top = Dimens.RowGap, bottom = Dimens.RowGap)
            )
            ChannelRowSkeleton(
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                count = 6,
                showShimmer = showShimmer
            )
        }
    }
}
