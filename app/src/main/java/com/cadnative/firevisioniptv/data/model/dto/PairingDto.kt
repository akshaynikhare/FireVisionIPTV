package com.cadnative.firevisioniptv.data.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Body of `POST /api/v1/tv/pairing/request`. The server labels the pending PIN with
 * these so the user can tell which device they are confirming on the web dashboard.
 */
data class PairingRequestBody(
    @SerializedName("deviceName")
    val deviceName: String,

    @SerializedName("deviceModel")
    val deviceModel: String
)

/**
 * Response to a pairing request. [pin] and [expiresAt] are only populated when
 * [success] is true; [expiresAt] is an ISO-8601 timestamp.
 */
data class PairingRequestResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("pin")
    val pin: String? = null,

    @SerializedName("expiresAt")
    val expiresAt: String? = null,

    @SerializedName("error")
    val error: String? = null
)

/**
 * Response to a pairing-status poll.
 *
 * [status] is one of `pending`, `completed`, `expired` or `invalid`. The server
 * deliberately withholds the user's identity from this unauthenticated endpoint, so
 * [username] is usually absent — callers fall back to a generic greeting.
 */
data class PairingStatusResponse(
    @SerializedName("paired")
    val paired: Boolean = false,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("channelListCode")
    val channelListCode: String? = null,

    @SerializedName("username")
    val username: String? = null
)
