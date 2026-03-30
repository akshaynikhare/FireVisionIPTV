package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class EpgProgramDto(
    @SerializedName("channelEpgId") val channelEpgId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("icon") val icon: String?
)

data class EpgGuideResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: Map<String, List<EpgProgramDto>>?
)
