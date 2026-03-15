package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for the channels API response.
 *
 * API returns: { "success": true, "data": [ ... ] }
 */
data class ChannelsResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("data")
    val data: List<ChannelDto> = emptyList()
)
