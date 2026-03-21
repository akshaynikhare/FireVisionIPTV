package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class FavoritesResponse(
    @SerializedName("channel_ids")
    val channelIds: List<String>,

    @SerializedName("timestamp")
    val timestamp: Long = 0L
)
