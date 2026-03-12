package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for the channels API response.
 * 
 * This response wrapper contains a list of channels and optional metadata
 * about the response such as total count and pagination info.
 */
data class ChannelsResponse(
    @SerializedName("channels")
    val channels: List<ChannelDto>,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("page")
    val page: Int? = null,
    
    @SerializedName("page_size")
    val pageSize: Int? = null
)
