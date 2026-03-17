package com.cadnative.firevisioniptv.presentation.model

data class PopularCategoryUiModel(
    val name: String,
    val channelCount: Int,
    val imageUrl: String?,
    val isFavorite: Boolean
)
