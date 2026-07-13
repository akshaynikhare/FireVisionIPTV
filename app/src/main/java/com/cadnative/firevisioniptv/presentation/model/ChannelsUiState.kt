package com.cadnative.firevisioniptv.presentation.model

enum class ErrorType {
    NONE,
    AUTH_REQUIRED,
    NETWORK_ERROR,
    SERVER_ERROR,
    UNKNOWN
}

data class ChannelsUiState(
    val channels: List<ChannelUiModel> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isInitialLoadComplete: Boolean = false,
    val error: String? = null,
    val errorType: ErrorType = ErrorType.NONE,
    val selectedCategory: String? = null,
    // HomeScreen: Featured & Recently Watched
    val featuredChannels: List<ChannelUiModel> = emptyList(),
    val recentlyWatched: List<ChannelUiModel> = emptyList(),
    // HomeScreen: personalized "For You" — channels similar to what the user
    // watches most (same categories as recently-watched), excluding channels
    // already in Recently Watched. Derived client-side; no backend change.
    val forYou: List<ChannelUiModel> = emptyList(),
    // Last channel opened from Home — used to restore focus on return from the player
    val lastPlayedChannelId: String? = null,
    // HomeScreen: Popular categories slider
    val popularCategories: List<PopularCategoryUiModel> = emptyList(),
    // Category logos (category name → up to 4 channel logo URLs for collage)
    val categoryLogos: Map<String, List<String>> = emptyMap(),
    // Category favorite names
    val favoriteCategoryNames: Set<String> = emptySet(),
    // QR code bitmap pointing to the "how to add channels" guide
    val guideQrBitmap: android.graphics.Bitmap? = null
)
