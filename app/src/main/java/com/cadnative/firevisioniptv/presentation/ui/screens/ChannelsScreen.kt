package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_TILE
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import com.cadnative.firevisioniptv.presentation.ui.theme.*
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onPairDevice: () -> Unit = {},
    initialCategory: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Hoisted so scroll position survives navigation to the player and back
    val gridState = rememberLazyGridState()

    LaunchedEffect(initialCategory) {
        viewModel.loadChannels(initialCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.selectedCategory ?: "All Channels",
                        style = MaterialTheme.typography.headlineMedium,
                        color = uiState.selectedCategory?.let { categoryColor(it) }
                            ?: MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    if (uiState.selectedCategory != null || initialCategory != null) {
                        IconButton(onClick = {
                            if (initialCategory != null) {
                                onNavigateBack()
                            } else {
                                viewModel.loadChannels(null)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.categories.isNotEmpty()) {
                CategoryChips(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.loadChannels(category)
                    },
                    onAllSelected = {
                        viewModel.loadChannels(null)
                    },
                    showAllChip = initialCategory == null
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val contentState = when {
                    uiState.isLoading && uiState.channels.isEmpty() -> "loading"
                    uiState.error != null && uiState.channels.isEmpty() -> "error"
                    uiState.channels.isEmpty() -> "empty"
                    else -> "content"
                }

                Crossfade(
                    targetState = contentState,
                    animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
                    label = "channelsState"
                ) { state ->
                    when (state) {
                        "loading" -> ChannelsGridLoadingSkeleton()
                        "error" -> ErrorState(
                            message = uiState.error ?: "Unknown error",
                            onRetry = { viewModel.refresh() },
                            errorType = uiState.errorType,
                            onPairDevice = onPairDevice
                        )
                        "empty" -> {
                            if (uiState.selectedCategory != null) {
                                EmptyState(
                                    message = "No channels in this category",
                                    onRetry = { viewModel.refresh() }
                                )
                            } else {
                                val ctx = LocalContext.current
                                EmptyPlaylistState(
                                    qrCodeBitmap = uiState.guideQrBitmap,
                                    onRetry = { viewModel.refresh() },
                                    isMobile = isMobileDevice(ctx),
                                    channelManagerUrl = AppPreferences.getServerUrl(ctx) + "/user/channels"
                                )
                            }
                        }
                        else -> ChannelsGrid(
                            channels = uiState.channels,
                            gridState = gridState,
                            onChannelClick = onChannelClick,
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChannelsGridLoadingSkeleton(modifier: Modifier = Modifier) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    ChannelGridSkeleton(
        columns = if (isCompact) 3 else 5,
        rows = 3,
        cardWidth = if (isCompact) Dimens.ChannelCardWidthMobile else Dimens.ChannelCardWidthTv,
        cardHeight = Dimens.GridCardHeight,
        showShimmer = !LocalPerfProfile.current.reduceMotion,
        modifier = modifier.padding(
            horizontal = if (isCompact) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv,
            vertical = Dimens.GridGap
        )
    )
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    onAllSelected: () -> Unit,
    showAllChip: Boolean = true,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    // Auto-scroll to keep the selected category visible
    val allChipOffset = if (showAllChip) 1 else 0
    val selectedIndex = if (selectedCategory == null) 0
        else categories.indexOf(selectedCategory).let { if (it >= 0) it + allChipOffset else 0 }
    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(maxOf(0, selectedIndex - 1))
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(
            horizontal = if (isCompact) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showAllChip) {
            item {
                CategoryChip(
                    label = "All",
                    isSelected = selectedCategory == null,
                    selectedContainerColor = Amber,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onAllSelected
                )
            }
        }
        items(categories, key = { it }) { category ->
            CategoryChip(
                label = category,
                isSelected = selectedCategory == category,
                selectedContainerColor = categoryColor(category),
                selectedLabelColor = MaterialTheme.colorScheme.background,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    selectedContainerColor: Color,
    selectedLabelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderStroke: BorderStroke? = when {
        isFocused -> BorderStroke(2.5.dp, FocusBorder)
        !isSelected -> BorderStroke(1.dp, subtleBorder)
        else -> null
    }
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFocused && !isSelected) Amber else Color.Unspecified
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedContainerColor,
            selectedLabelColor = selectedLabelColor,
            containerColor = if (isFocused) Amber.copy(alpha = 0.15f) else Color.Transparent
        ),
        border = borderStroke,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .tvFocusVisuals(
                focused = isFocused,
                shape = MaterialTheme.shapes.small,
                focusedScale = FOCUS_SCALE_TILE
            )
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChannelsGrid(
    channels: List<ChannelUiModel>,
    gridState: LazyGridState,
    onChannelClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = if (isCompact) 100.dp else 140.dp),
        modifier = modifier
            .fillMaxSize()
            .focusRestorer(),
        contentPadding = PaddingValues(
            horizontal = if (isCompact) Dimens.ScreenPaddingHorizontalMobile else Dimens.ScreenPaddingHorizontalTv,
            vertical = Dimens.GridGap
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GridGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.GridGap)
    ) {
        itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { onToggleFavorite(channel.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.GridCardHeight)
                    .animateItemEntrance(index)
            )
        }
    }
}
