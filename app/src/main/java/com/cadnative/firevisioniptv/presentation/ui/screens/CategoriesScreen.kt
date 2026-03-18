package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.EmptyState
import com.cadnative.firevisioniptv.presentation.ui.components.ErrorState
import com.cadnative.firevisioniptv.presentation.ui.components.LoadingIndicator
import com.cadnative.firevisioniptv.presentation.ui.theme.*
import com.cadnative.firevisioniptv.presentation.viewmodel.ChannelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadChannels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
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
                    "empty" -> EmptyState(message = "No categories available")
                    else -> {
                        val categoriesWithCount = remember(uiState.channels) {
                            uiState.channels
                                .groupBy { it.category.ifBlank { "Other" } }
                                .map { (name, channels) -> name to channels.size }
                                .sortedBy { it.first }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 240.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(categoriesWithCount) { index, (category, count) ->
                                CategoryCard(
                                    name = category,
                                    channelCount = count,
                                    logoUrls = uiState.categoryLogos[category] ?: emptyList(),
                                    isFavorite = category in uiState.favoriteCategoryNames,
                                    onClick = { onCategoryClick(category) },
                                    onToggleFavorite = { viewModel.toggleCategoryFavorite(category) },
                                    modifier = Modifier.animateItemEntrance(index)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    name: String,
    channelCount: Int,
    logoUrls: List<String>,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var longPressHandled by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = EaseOutQuart),
        label = "categoryScale"
    )
    val catColor = categoryColor(name)
    val catIcon = categoryIcon(name)

    Card(
        onClick = {
            if (!longPressHandled) {
                onClick()
            }
            longPressHandled = false
        },
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_MENU,
                    KeyEvent.KEYCODE_BOOKMARK -> {
                        onToggleFavorite()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        if (keyEvent.nativeKeyEvent.isLongPress) {
                            longPressHandled = true
                            onToggleFavorite()
                            true
                        } else false
                    }
                    else -> false
                }
            },
        shape = MaterialTheme.shapes.medium,
        border = when {
            isFocused -> BorderStroke(2.dp, FocusBorder)
            else -> BorderStroke(1.dp, SubtleBorder)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Category color gradient background (always present)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                catColor.copy(alpha = 0.18f),
                                catColor.copy(alpha = 0.04f)
                            )
                        )
                    )
            )

            // Layer 2: Stacked channel logos — fanned out with decreasing opacity
            if (logoUrls.isNotEmpty()) {
                val logoAlphas = listOf(0.45f, 0.32f, 0.22f, 0.14f)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(width = (logoUrls.size * 18 + 24).dp, height = 42.dp)
                ) {
                    logoUrls.take(4).forEachIndexed { index, url ->
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            modifier = Modifier
                                .size(38.dp)
                                .offset(x = (index * 18).dp)
                                .alpha(logoAlphas.getOrElse(index) { 0.1f })
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Layer 3: Category icon — top-left, prominent
            Icon(
                imageVector = catIcon,
                contentDescription = null,
                tint = catColor.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 18.dp)
                    .size(28.dp)
            )

            // Category accent line at top
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(catColor.copy(alpha = 0.8f))
            )

            // Favorite badge — top right
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    tint = Amber,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            // "Hold to fav" hint when focused and not yet favorite
            if (isFocused && !isFavorite) {
                Text(
                    text = "Hold to favorite",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            // Text content — bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = catColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$channelCount channels",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = EmphasisMedium)
                )
            }
        }
    }
}
