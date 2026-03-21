package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

data class StreamStatusReport(
    @SerializedName("status")
    val status: String,

    @SerializedName("deviceId")
    val deviceId: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("errorMessage")
    val errorMessage: String? = null
)
