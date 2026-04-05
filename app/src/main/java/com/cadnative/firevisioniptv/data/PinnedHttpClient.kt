package com.cadnative.firevisioniptv.data

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttpClient with certificate pinning for use outside of Hilt-injected classes
 * (PairingActivity, PairingViewModel, UpdateManager, SettingsViewModel raw calls).
 *
 * This ensures all HTTP calls to tv.cadnative.com get the same cert pinning
 * as the Retrofit client in NetworkModule.
 */
object PinnedHttpClient {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val certificatePinner = CertificatePinner.Builder()
        .add("tv.cadnative.com", "sha256/vP6G0qWePuvOXaIjJSRs2qQ2MSjo9KdUfsQU8TEqlWI=")
        .build()

    val instance: OkHttpClient = OkHttpClient.Builder()
        .certificatePinner(certificatePinner)
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
