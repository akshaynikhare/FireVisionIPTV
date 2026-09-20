package com.cadnative.firevisioniptv.data.source.remote.playlist

import com.cadnative.firevisioniptv.data.model.dto.ChannelDto

/**
 * Result of loading a bring-your-own playlist source.
 *
 * [channels] flow through the same mapper/storage pipeline as managed channels.
 * [epgUrl] is the guide URL discovered from the source (M3U `url-tvg` header or
 * Xtream `xmltv.php`), wired into the client-side EPG so BYO sources get a guide too.
 * [legacyIdAliases] maps an id this source used to emit to the id it emits now, so a
 * refresh can carry per-channel user state (favorites, health, metrics, resume points)
 * across an id-scheme change instead of orphaning it.
 */
data class PlaylistFetch(
    val channels: List<ChannelDto>,
    val epgUrl: String?,
    val legacyIdAliases: Map<String, String> = emptyMap()
)
