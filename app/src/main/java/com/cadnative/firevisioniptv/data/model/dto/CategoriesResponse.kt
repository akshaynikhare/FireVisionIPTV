package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for the categories API response.
 * 
 * This response wrapper contains a list of categories and optional metadata
 * about the response such as total count.
 */
data class CategoriesResponse(
    @SerializedName("categories")
    val categories: List<CategoryDto>,
    
    @SerializedName("total")
    val total: Int? = null
)
