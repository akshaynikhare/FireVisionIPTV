package com.cabletech.iptvplayer.playlist

import com.cabletech.iptvplayer.core.model.Channel
import java.util.UUID

/**
 * Lightweight M3U/M3U8 parser.
 *
 * Handles the extended M3U format with `#EXTINF` directives and common tvg
 * attributes (`tvg-id`, `tvg-name`, `tvg-logo`, `group-title`).
 */
object M3uParser {

    private val EXTINF_REGEX = Regex("""#EXTINF:-?\d+(?:\s+(.*))?,(.*)""")
    private val ATTR_REGEX = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(content: String): List<Channel> {
        val lines = content.lines()
        val channels = mutableListOf<Channel>()
        var currentAttrs: Map<String, String> = emptyMap()
        var currentDisplayName: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF") -> {
                    val match = EXTINF_REGEX.find(trimmed)
                    val attrString = match?.groupValues?.get(1) ?: ""
                    currentDisplayName = match?.groupValues?.get(2)?.trim()
                    currentAttrs = parseAttributes(attrString)
                }
                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    val name = currentAttrs["tvg-name"]
                        ?.takeIf { it.isNotBlank() }
                        ?: currentDisplayName
                        ?: "Unknown"

                    channels += Channel(
                        id = currentAttrs["tvg-id"]?.takeIf { it.isNotBlank() }
                            ?: UUID.randomUUID().toString(),
                        name = name,
                        streamUrl = trimmed,
                        logoUrl = currentAttrs["tvg-logo"]?.takeIf { it.isNotBlank() },
                        groupTitle = currentAttrs["group-title"]?.takeIf { it.isNotBlank() },
                        tvgId = currentAttrs["tvg-id"]?.takeIf { it.isNotBlank() },
                    )
                    currentAttrs = emptyMap()
                    currentDisplayName = null
                }
            }
        }
        return channels
    }

    private fun parseAttributes(attrString: String): Map<String, String> =
        ATTR_REGEX.findAll(attrString).associate { it.groupValues[1] to it.groupValues[2] }
}
