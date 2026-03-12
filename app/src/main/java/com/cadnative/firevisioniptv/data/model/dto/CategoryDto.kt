package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Category API responses.
 * 
 * This DTO matches the API response structure and is used for network communication.
 * It will be mapped to CategoryEntity for local storage.
 */
data class CategoryDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("display_order")
    val displayOrder: Int = 0,
    
    @SerializedName("channel_count")
    val channelCount: Int = 0
)
