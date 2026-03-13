package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Channel API responses.
 *
 * Matches the actual server response fields from /api/v1/channels.
 */
data class ChannelDto(
    @SerializedName("channelId")
    val id: String,

    @SerializedName("channelName")
    val name: String,

    @SerializedName("channelUrl")
    val url: String,

    @SerializedName("channelImg")
    val tvgLogo: String?,

    @SerializedName("channelGroup")
    val groupTitle: String?,

    @SerializedName("channelDrmKey")
    val drmKey: String? = null,

    @SerializedName("channelDrmType")
    val drmType: String? = null,

    val tvgLanguage: String? = null,
    val tvgCountry: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val isActive: Boolean = true,
    val metadata: ChannelMetadataDto? = null
)

data class ChannelMetadataDto(
    @SerializedName("language")
    val language: String? = null
)
