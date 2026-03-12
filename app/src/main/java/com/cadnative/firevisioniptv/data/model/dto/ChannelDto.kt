package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Channel API responses.
 * 
 * This DTO matches the API response structure and is used for network communication.
 * It will be mapped to ChannelEntity for local storage.
 */
data class ChannelDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("tvg_logo")
    val tvgLogo: String?,
    
    @SerializedName("group_title")
    val groupTitle: String?,
    
    @SerializedName("tvg_language")
    val tvgLanguage: String?,
    
    @SerializedName("tvg_country")
    val tvgCountry: String?,
    
    @SerializedName("tvg_id")
    val tvgId: String?,
    
    @SerializedName("tvg_name")
    val tvgName: String?,
    
    @SerializedName("is_active")
    val isActive: Boolean = true
)
