package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class StreamPlayReport(
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("timestamp")
    val timestamp: Long
)
