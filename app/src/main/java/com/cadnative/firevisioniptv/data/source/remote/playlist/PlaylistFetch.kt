package com.cadnative.firevisioniptv.data.source.remote.playlist

import com.cadnative.firevisioniptv.data.model.dto.ChannelDto

/**
 * Result of loading a bring-your-own playlist source.
 *
 * [channels] flow through the same mapper/storage pipeline as managed channels.
 * [epgUrl] is the guide URL discovered from the source (M3U `url-tvg` header or
 * Xtream `xmltv.php`), wired into the client-side EPG so BYO sources get a guide too.
 */
data class PlaylistFetch(
    val channels: List<ChannelDto>,
    val epgUrl: String?
)
