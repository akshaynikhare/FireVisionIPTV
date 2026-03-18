package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class HealthSyncRequest(
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("results")
    val results: List<HealthSyncItem>
)

data class HealthSyncItem(
    @SerializedName("channel_id")
    val channelId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("response_time_ms")
    val responseTimeMs: Long? = null,

    @SerializedName("timestamp")
    val timestamp: Long
)
