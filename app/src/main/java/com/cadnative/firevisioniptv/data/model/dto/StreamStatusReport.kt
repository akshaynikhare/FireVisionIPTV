package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class StreamStatusReport(
    @SerializedName("status")
    val status: String,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("error_message")
    val errorMessage: String? = null
)
