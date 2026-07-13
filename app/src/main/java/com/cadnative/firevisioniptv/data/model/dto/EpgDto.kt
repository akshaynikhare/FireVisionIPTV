package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class EpgProgramDto(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("start") val start: String,
    @SerializedName("end") val end: String,
    @SerializedName("icon") val icon: String?
)

data class EpgChannelDto(
    @SerializedName("channelId") val channelId: String,
    @SerializedName("channelName") val channelName: String?,
    @SerializedName("tvgLogo") val tvgLogo: String?,
    @SerializedName("programs") val programs: List<EpgProgramDto>?
)

data class EpgGuideResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("channels") val channels: List<EpgChannelDto>?
)
