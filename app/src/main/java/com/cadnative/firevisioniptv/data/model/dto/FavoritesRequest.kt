package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body for syncing favorites to the server.
 * 
 * This request contains the list of favorite channel IDs and their display order
 * to be synchronized with the server.
 */
data class FavoritesRequest(
    @SerializedName("channel_ids")
    val channelIds: List<String>,
    
    @SerializedName("device_id")
    val deviceId: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Individual favorite item with ordering information.
 * Used when the server requires detailed favorite information.
 */
data class FavoriteItemDto(
    @SerializedName("channel_id")
    val channelId: String,
    
    @SerializedName("display_order")
    val displayOrder: Int,
    
    @SerializedName("added_at")
    val addedAt: Long
)

/**
 * Detailed favorites sync request with ordering information.
 */
data class DetailedFavoritesRequest(
    @SerializedName("favorites")
    val favorites: List<FavoriteItemDto>,
    
    @SerializedName("device_id")
    val deviceId: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
