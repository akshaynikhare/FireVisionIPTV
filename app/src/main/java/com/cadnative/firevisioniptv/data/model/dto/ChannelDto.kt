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
    val channelImg: String?,

    val tvgLogo: String? = null,

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
    val metadata: ChannelMetadataDto? = null,
    val alternateStreams: List<AlternateStreamDto>? = null
) {
    /** Resolved logo URL: prefers tvgLogo, falls back to channelImg */
    val logoUrl: String?
        get() = tvgLogo?.takeIf { it.isNotEmpty() } ?: channelImg?.takeIf { it.isNotEmpty() }
}

data class ChannelMetadataDto(
    @SerializedName("language")
    val language: String? = null
)

data class AlternateStreamDto(
    @SerializedName("streamUrl")
    val streamUrl: String,
    @SerializedName("quality")
    val quality: String? = null
)
