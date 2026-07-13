package com.cadnative.firevisioniptv.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.cadnative.firevisioniptv.presentation.ui.player.isMobileDevice
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder
import com.cadnative.firevisioniptv.presentation.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onMultiviewClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchEditing by remember { mutableStateOf(false) }
    var fieldHadFocus by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isMobile = isMobileDevice(LocalContext.current)
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val horizontalPadding = if (isPortrait) {
        Dimens.ScreenPaddingHorizontalMobile
    } else {
        Dimens.ScreenPaddingHorizontalTv
    }

    val fauxFieldFocusRequester = remember { FocusRequester() }

    // TV: land on the faux field (no IME); mobile keeps the real field + keyboard
    LaunchedEffect(Unit) {
        if (isMobile) focusRequester.requestFocus() else fauxFieldFocusRequester.requestFocus()
    }
    LaunchedEffect(searchEditing) {
        if (searchEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    if (searchEditing) {
        androidx.activity.compose.BackHandler {
            searchEditing = false
            keyboardController?.hide()
            runCatching { fauxFieldFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = Dimens.ScreenPaddingVertical)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isMobile && !searchEditing) {
                // A TextField consumes D-pad arrows for cursor movement and pops the
                // IME on focus, trapping TV navigation. This plain focusable stands in
                // until the user presses OK to edit.
                TvSearchFieldPlaceholder(
                    query = searchQuery,
                    onActivate = { searchEditing = true },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(fauxFieldFocusRequester)
                )
            } else TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.onQueryChange(it)
                },
                placeholder = {
                    Text(
                        text = "Search channels, categories...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
                        exit = fadeOut(tween(DURATION_FAST, easing = EaseOutQuart))
                    ) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.onQueryChange("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (isMobile) {
                            if (it.hasFocus) keyboardController?.show()
                        } else if (it.hasFocus) {
                            fieldHadFocus = true
                        } else if (fieldHadFocus) {
                            // Only a real focus loss ends editing — the initial
                            // hasFocus=false on attach must not bounce us back
                            fieldHadFocus = false
                            searchEditing = false
                        }
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                    unfocusedIndicatorColor = subtleBorder,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            VoiceSearchButton(
                onResult = { text ->
                    searchQuery = text
                    viewModel.onQueryChange(text)
                },
                onStatusChange = { voiceStatus = it }
            )
        }

        voiceStatus?.let { status ->
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
            label = "searchState"
        ) { state ->
            when (state) {
                "loading" -> SearchResultsSkeleton(
                    showShimmer = !LocalPerfProfile.current.reduceMotion
                )
                "error" -> ErrorState(
                    message = uiState.error ?: "Search failed",
                    onRetry = { viewModel.onQueryChange(searchQuery) }
                )
                "recent" -> RecentSearches(
                    searches = uiState.recentSearches,
                    onSearchClick = { query ->
                        searchQuery = query
                        viewModel.onQueryChange(query)
                    },
                    onClearHistory = { viewModel.clearHistory() }
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
                                    onFavoriteClick = { viewModel.toggleFavorite(channel.id) },
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
}
