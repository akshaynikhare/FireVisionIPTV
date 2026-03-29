package com.cabletech.iptvplayer.core.model

/**
 * Domain model for a single IPTV channel parsed from an M3U playlist or EPG source.
 *
 * @param id          Unique identifier (tvg-id from M3U, or generated UUID).
 * @param name        Display name of the channel.
 * @param streamUrl   Direct stream URL (HLS/DASH/RTSP/UDP).
 * @param logoUrl     Optional channel logo URL.
 * @param groupTitle  Playlist group/category label.
 * @param tvgId       tvg-id used to match EPG programme data.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val tvgId: String? = null,
)
