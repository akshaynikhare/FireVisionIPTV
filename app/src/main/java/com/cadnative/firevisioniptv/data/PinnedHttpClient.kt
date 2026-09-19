package com.cadnative.firevisioniptv.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    suspend fun getCancellable(url: String, headers: Map<String, String> = emptyMap()): Response {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (key, value) -> builder.addHeader(key, value) }
        return executeCancellable(builder.build())
    }

    suspend fun postCancellable(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap()
    ): Response {
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder().url(url).post(body)
        headers.forEach { (key, value) -> builder.addHeader(key, value) }
        return executeCancellable(builder.build())
    }

    private suspend fun executeCancellable(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = instance.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) continuation.resume(response) else response.close()
                }
            })
        }
}
