package com.cadnative.firevisioniptv.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttpClient for use outside of Hilt-injected classes
 * (PairingActivity, PairingViewModel, AppUpdater, SettingsViewModel raw calls).
 *
 * No certificate pinning — the Let's Encrypt leaf rotates ~every 90 days, which would
 * break a hardcoded pin (and did). Standard system-CA TLS validation still applies.
 */
object PinnedHttpClient {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val instance: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun get(url: String, headers: Map<String, String> = emptyMap()): okhttp3.Response {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return instance.newCall(builder.build()).execute()
    }

    fun post(url: String, jsonBody: String, headers: Map<String, String> = emptyMap()): okhttp3.Response {
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder().url(url).post(body)
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return instance.newCall(builder.build()).execute()
    }
}
