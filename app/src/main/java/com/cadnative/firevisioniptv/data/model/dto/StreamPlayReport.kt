package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class StreamPlayReport(
    @SerializedName("deviceId")
    val deviceId: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("proxyPlay")
    val proxyPlay: Boolean = false
)
