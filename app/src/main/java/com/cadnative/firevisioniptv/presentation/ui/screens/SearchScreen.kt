package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.components.*
import com.cadnative.firevisioniptv.presentation.viewmodel.SearchViewModel

/**
 * Search screen with search input, recent searches, and filtered results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.search(it)
                        },
                        placeholder = { Text("Search channels...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.search("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(message = "Searching...")
                }
                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error ?: "Search failed",
                        onRetry = { viewModel.search(searchQuery) }
                    )
                }
                searchQuery.isEmpty() -> {
                    RecentSearches(
                        searches = uiState.recentSearches,
                        onSearchClick = { query ->
                            searchQuery = query
                            viewModel.search(query)
                        }
                    )
                }
                uiState.results.isEmpty() -> {
                    EmptyState(message = "No channels found")
                }
                else -> {
                    SearchResults(
                        results = uiState.results,
                        onChannelClick = onChannelClick
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Recent Searches",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(searches) { search ->
                TextButton(onClick = { onSearchClick(search) }) {
                    Text(text = search)
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<com.cadnative.firevisioniptv.presentation.model.ChannelUiModel>,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(results) { channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel.id) },
                onFavoriteClick = { /* Handle favorite */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
