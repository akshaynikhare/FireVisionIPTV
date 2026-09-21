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
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.components.categoryLabel
import com.cadnative.firevisioniptv.R
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
import com.cadnative.firevisioniptv.domain.model.CategorySentinels

private const val HERO_SWAP_DEBOUNCE_MS = 300L

// Seeding focus has to outlast the hero's entrance animation placing its node.
private const val FOCUS_SEED_ATTEMPTS = 5
private const val FOCUS_SEED_RETRY_MS = 50L

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
    onMultiviewClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isCompact = configuration.screenWidthDp < COMPACT_WIDTH_DP
    val horizontalPadding =
        if (isPortrait) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv
    val rowGap = if (isCompact) Dimens.RowGapMobile else Dimens.RowGap
    val screenPaddingVertical =
        if (isCompact) Dimens.ScreenPaddingVerticalMobile else Dimens.ScreenPaddingVertical

    // On phones the default hero (0.35 of screen height) is TV-huge. Force a
    // banner-proportioned height off screen *width* so it takes a sensible
    // slice of a portrait screen; capped so it never dominates a short screen.
    // TV/landscape passes no override — HomeHero keeps its own height.
    val compactHeroHeight = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val byWidth = configuration.screenWidthDp * 0.60f
        val cap = configuration.screenHeightDp * 0.30f
        // cap can undercut the 180dp floor on short/landscape screens — cap wins
        byWidth.coerceAtLeast(180f).coerceAtMost(cap).dp
    }

    val channelsByCategory = remember(channels) {
        channels.groupBy { it.category.ifBlank { CategorySentinels.OTHER } }
    }
    val categoryEntries = remember(channelsByCategory) {
        channelsByCategory.entries.toList()
    }
    // Sticky rather than derived directly: featuredChannels arrives after channels
    // on a warm start, and re-keying the hero on every emission disposes whatever
    // currently holds focus — which drops focus to the root and, on the next key
    // press, back onto the rail. Only swap when the identities actually change.
    // Only the *identity* list is sticky. The models themselves are always the
    // newest emission, so a favourite toggle, a health result or an EPG refresh
    // shows straight away — making the whole list sticky kept stale names, logos
    // and favourite state on screen until membership happened to change.
    var bannerChannels by remember { mutableStateOf(emptyList<ChannelUiModel>()) }
    var bannerIds by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(featuredChannels, channels) {
        val next = featuredChannels.ifEmpty { channels.take(5) }
        // An empty emission keeps the previous models rather than tearing the
        // hero down for a frame.
        if (next.isEmpty()) return@LaunchedEffect
        bannerChannels = next
        val nextIds = next.map { it.id }
        if (nextIds != bannerIds) bannerIds = nextIds
    }

    // Hero follows D-pad focus in the featured row, debounced so fast
    // scrubbing doesn't thrash image decodes. Defaults to the first
    // featured channel so the hero renders immediately.
    // Keyed on the first channel's id, not the list instance: a reordered list with
    // the same head should not reset the hero out from under the user.
    val bannerHeadId = bannerIds.firstOrNull()
    // Ids, not models: holding a model here would pin whatever snapshot was
    // current when the key last changed, which is how the hero went stale.
    var focusedFeaturedId by remember(bannerHeadId) { mutableStateOf(bannerHeadId) }
    var heroChannelId by remember(bannerHeadId) { mutableStateOf(bannerHeadId) }
    LaunchedEffect(focusedFeaturedId) {
        if (heroChannelId != focusedFeaturedId) {
            delay(HERO_SWAP_DEBOUNCE_MS)
            heroChannelId = focusedFeaturedId
        }
    }
    val heroChannel = bannerChannels.firstOrNull { it.id == heroChannelId }
        ?: bannerChannels.firstOrNull()

    // Restore focus to the last played channel in exactly one section,
    // preferring the section closest to the top.
    val featuredFocusId = remember(lastPlayedChannelId, bannerIds) {
        lastPlayedChannelId?.takeIf { it in bannerIds }
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
    // Keyed on the hero's id and retried, because a single attempt loses every
    // race it can enter: the requester lives inside a LazyColumn item gated on
    // heroChannel, behind an entrance animation, so on a cold start the node is
    // routinely unplaced when the effect first runs. requestFocus throws then,
    // runCatching swallows it, and focus stays wherever it was — which is the
    // rail, since the rail is simply the first focusable in traversal order and
    // nothing else claims focus on this screen.
    //
    // The latch matters as much as the retry: without it, any later recomposition
    // that re-keys this effect would yank focus back to Watch now after the user
    // had already moved.
    var focusSeeded by remember { mutableStateOf(false) }
    LaunchedEffect(bannerHeadId, hasRestoreTarget) {
        if (focusSeeded || hasRestoreTarget || heroChannel == null) return@LaunchedEffect
        repeat(FOCUS_SEED_ATTEMPTS) {
            if (runCatching { watchNowFocusRequester.requestFocus() }.isSuccess) {
                focusSeeded = true
                return@LaunchedEffect
            }
            delay(FOCUS_SEED_RETRY_MS)
        }
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
        contentPadding = PaddingValues(vertical = screenPaddingVertical)
    ) {
        item(key = "hero") {
            heroChannel?.let { hero ->
                HomeHero(
                    channel = hero,
                    onWatchNow = onChannelClick,
                    watchNowFocusRequester = watchNowFocusRequester,
                    modifier = Modifier
                        .padding(bottom = rowGap)
                        // requiredHeight overrides HomeHero's internal .height() so
                        // the phone hero shrinks; TV/landscape is left untouched.
                        .then(if (isCompact) Modifier.requiredHeight(compactHeroHeight) else Modifier)
                        .animateItemEntrance(index = 0)
                )
            }
        }

        item(key = "featured") {
            FeaturedRow(
                channels = bannerChannels,
                onChannelClick = onChannelClick,
                onToggleFavorite = onToggleFavorite,
                onMultiviewClick = onMultiviewClick,
                onChannelFocused = { focusedFeaturedId = it.id },
                focusChannelId = featuredFocusId,
                horizontalPadding = horizontalPadding,
                modifier = Modifier
                    .padding(bottom = rowGap)
                    .animateItemEntrance(index = 1)
            )
        }

        if (recentlyWatched.isNotEmpty()) {
            item(key = "recently_watched") {
                ChannelRow(
                    // Live re-tune shortcut — quick jump back to channels the
                    // user was just watching (never a resumed file position).
                    title = stringResource(R.string.home_row_recent),
                    channels = recentlyWatched,
                    onChannelClick = onChannelClick,
                    onToggleFavorite = onToggleFavorite,
                    onMultiviewClick = onMultiviewClick,
                    focusChannelId = recentFocusId,
                    horizontalPadding = horizontalPadding,
                    modifier = Modifier
                        .padding(bottom = rowGap)
                        .animateItemEntrance(index = 2)
                )
            }
        }

        if (forYou.isNotEmpty()) {
            item(key = "for_you") {
                ChannelRow(
                    title = stringResource(R.string.home_row_for_you),
                    channels = forYou,
                    onChannelClick = onChannelClick,
                    onToggleFavorite = onToggleFavorite,
                    onMultiviewClick = onMultiviewClick,
                    focusChannelId = null,
                    horizontalPadding = horizontalPadding,
                    modifier = Modifier
                        .padding(bottom = rowGap)
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
                        .padding(bottom = rowGap)
                        .animateItemEntrance(index = categoryRowOffset - 1)
                )
            }
        }

        itemsIndexed(
            items = categoryEntries,
            key = { _, entry -> "category_${entry.key}" }
        ) { index, (category, categoryChannels) ->
            ChannelRow(
                title = categoryLabel(category),
                channels = categoryChannels,
                onChannelClick = onChannelClick,
                onToggleFavorite = onToggleFavorite,
                onMultiviewClick = onMultiviewClick,
                onSeeAllClick = { onNavigateToChannels(category) },
                focusChannelId = categoryFocusId,
                horizontalPadding = horizontalPadding,
                modifier = Modifier
                    .padding(bottom = rowGap)
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
    val rowGap = if (isCompact) Dimens.RowGapMobile else Dimens.RowGap
    val screenPaddingVertical =
        if (isCompact) Dimens.ScreenPaddingVerticalMobile else Dimens.ScreenPaddingVertical
    val shimmerBrush = if (showShimmer) rememberShimmerBrush() else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = screenPaddingVertical)
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
                modifier = Modifier.padding(top = rowGap, bottom = rowGap)
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
