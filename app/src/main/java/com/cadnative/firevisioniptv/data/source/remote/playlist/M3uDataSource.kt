package com.cadnative.firevisioniptv.data.source.remote.playlist

import com.cadnative.firevisioniptv.data.model.dto.ChannelDto
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches and parses a standard M3U/M3U8 playlist on-device (the TiviMate model),
 * turning `#EXTINF` entries into [ChannelDto]s that reuse the managed-channel pipeline.
 * Also extracts the playlist's `url-tvg`/`x-tvg-url` header so BYO sources get an EPG.
 */
@Singleton
class M3uDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    fun fetch(url: String): PlaylistFetch {
        if (url.isBlank()) return PlaylistFetch(emptyList(), null)
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return PlaylistFetch(emptyList(), null)
            val body = response.body?.string() ?: return PlaylistFetch(emptyList(), null)
            return parse(body)
        }
    }

    /** Parse raw M3U text. Public for unit testing. */
    fun parse(text: String): PlaylistFetch {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return PlaylistFetch(emptyList(), null)

        val epgUrl = lines.firstOrNull { it.startsWith("#EXTM3U", ignoreCase = true) }
            ?.let { header ->
                attr(header, "url-tvg") ?: attr(header, "x-tvg-url")
            }?.takeIf { it.isNotBlank() }

        val channels = ArrayList<ChannelDto>()
        val legacyIdAliases = LinkedHashMap<String, String>()
        var pending: ExtInf? = null
        var index = 0
        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> pending = parseExtInf(line)
                line.startsWith("#") -> { /* skip other tags */ }
                else -> {
                    val info = pending
                    if (info != null) {
                        val tvgId = info.tvgId?.takeIf { it.isNotBlank() }
                        val id = stableChannelId(listOfNotNull(tvgId, line).joinToString("|"))
                        val legacyId = legacyChannelId(index, tvgId, info.name)
                        if (legacyId != id) legacyIdAliases[legacyId] = id
                        channels.add(
                            ChannelDto(
                                id = id,
                                name = info.name,
                                url = line,
                                channelImg = null,
                                tvgLogo = info.logo,
                                groupTitle = info.group,
                                tvgId = tvgId,
                                tvgName = info.name
                            )
                        )
                        index++
                        pending = null
                    }
                }
            }
        }
        return PlaylistFetch(channels, epgUrl, legacyIdAliases)
    }

    private fun stableChannelId(sourceId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sourceId.trim().toByteArray(Charsets.UTF_8))
            .take(16)
            .joinToString("") { "%02x".format(it) }
        return "m3u-$digest"
    }

    /**
     * The id this parser emitted before ids became content-derived: position in the
     * playlist plus tvg-id or display name. Emitted alongside the current id so a
     * refresh can re-point favorites, health, metrics and resume points that were
     * stored under it. Position-based, so it only resolves for channels the playlist
     * still lists in the same order — a reordered playlist migrates what it can.
     */
    private fun legacyChannelId(index: Int, tvgId: String?, name: String): String =
        "m3u-$index-${tvgId ?: name}".take(200)

    private data class ExtInf(
        val name: String,
        val logo: String?,
        val group: String?,
        val tvgId: String?
    )

    private fun parseExtInf(line: String): ExtInf {
        // #EXTINF:-1 tvg-id="..." tvg-logo="..." group-title="...",Display Name
        val name = line.substringAfter(',', "").trim().ifEmpty {
            attr(line, "tvg-name") ?: "Unknown"
        }
        return ExtInf(
            name = name,
            logo = attr(line, "tvg-logo"),
            group = attr(line, "group-title"),
            tvgId = attr(line, "tvg-id")
        )
    }

    /** Extract a quoted attribute value (e.g. tvg-id="CNN.us") from an EXTINF/header line. */
    private fun attr(line: String, key: String): String? {
        val regex = Regex("""$key\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }
}
