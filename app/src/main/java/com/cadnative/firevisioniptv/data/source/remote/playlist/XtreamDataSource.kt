package com.cadnative.firevisioniptv.data.source.remote.playlist

import com.cadnative.firevisioniptv.data.model.dto.ChannelDto
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Xtream Codes client. Loads live categories + streams via `player_api.php` and maps
 * them into [ChannelDto]s that reuse the managed-channel pipeline. Live stream URLs
 * follow the standard `/live/{user}/{pass}/{id}.m3u8` form; EPG comes from `xmltv.php`.
 */
@Singleton
class XtreamDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()

    fun fetch(host: String, username: String, password: String): PlaylistFetch {
        if (host.isBlank() || username.isBlank()) return PlaylistFetch(emptyList(), null)
        val base = host.trimEnd('/')
        val u = URLEncoder.encode(username, "UTF-8")
        val p = URLEncoder.encode(password, "UTF-8")

        val categories = getList<XtreamCategory>(
            "$base/player_api.php?username=$u&password=$p&action=get_live_categories"
        )
        val categoryNames = categories.associate { it.categoryId to it.categoryName }

        val streams = getList<XtreamStream>(
            "$base/player_api.php?username=$u&password=$p&action=get_live_streams"
        )
        if (streams.isEmpty()) return PlaylistFetch(emptyList(), null)

        val channels = streams.map { s ->
            ChannelDto(
                id = "xtream-${s.streamId}",
                name = s.name ?: "Channel ${s.streamId}",
                url = "$base/live/$username/$password/${s.streamId}.m3u8",
                channelImg = null,
                tvgLogo = s.streamIcon,
                groupTitle = categoryNames[s.categoryId] ?: "Uncategorized",
                tvgId = s.epgChannelId?.takeIf { it.isNotBlank() },
                tvgName = s.name
            )
        }
        val epgUrl = "$base/xmltv.php?username=$u&password=$p"
        return PlaylistFetch(channels, epgUrl)
    }

    private inline fun <reified T> getList(url: String): List<T> {
        return try {
            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val json = response.body?.string() ?: return emptyList()
                val type = TypeToken.getParameterized(List::class.java, T::class.java).type
                gson.fromJson<List<T>>(json, type) ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private data class XtreamCategory(
        @SerializedName("category_id") val categoryId: String? = null,
        @SerializedName("category_name") val categoryName: String = "Uncategorized"
    )

    private data class XtreamStream(
        @SerializedName("stream_id") val streamId: Long = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("stream_icon") val streamIcon: String? = null,
        @SerializedName("epg_channel_id") val epgChannelId: String? = null,
        @SerializedName("category_id") val categoryId: String? = null
    )
}
