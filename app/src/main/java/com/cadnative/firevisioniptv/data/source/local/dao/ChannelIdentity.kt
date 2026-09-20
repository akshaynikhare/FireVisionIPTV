package com.cadnative.firevisioniptv.data.source.local.dao

/**
 * The columns needed to recognise a cached channel across an id change: what it is
 * ([streamUrl], [tvgId]) as opposed to what it is currently called ([id]).
 */
data class ChannelIdentity(
    val id: String,
    val streamUrl: String,
    val tvgId: String?
)
