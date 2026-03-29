package com.cabletech.iptvplayer.playlist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches raw M3U/M3U8 content from a remote URL using OkHttp.
 *
 * Returns the content string on success, or throws [PlaylistFetchException] on failure.
 */
class PlaylistFetcher(private val client: OkHttpClient) {

    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw PlaylistFetchException(
                    "HTTP ${response.code} fetching playlist: $url"
                )
            }
            response.body?.string()
                ?: throw PlaylistFetchException("Empty response body for: $url")
        }
    }
}

class PlaylistFetchException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
