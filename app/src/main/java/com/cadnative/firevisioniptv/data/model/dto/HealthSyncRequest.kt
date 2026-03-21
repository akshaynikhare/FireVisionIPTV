package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class HealthSyncRequest(
    @SerializedName("deviceId")
    val deviceId: String,

    @SerializedName("results")
    val results: List<HealthSyncItem>
)

data class HealthSyncItem(
    @SerializedName("channelId")
    val channelId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("responseTimeMs")
    val responseTimeMs: Long? = null,

    @SerializedName("timestamp")
    val timestamp: Long
)
