package com.cabletech.iptvplayer.player

import com.cabletech.iptvplayer.core.model.Channel

/** Sealed hierarchy representing all possible states of the IPTV player. */
sealed class PlayerState {
    data object Idle : PlayerState()
    data class Loading(val channel: Channel) : PlayerState()
    data class Playing(val channel: Channel) : PlayerState()
    data class Paused(val channel: Channel) : PlayerState()
    data class Buffering(val channel: Channel) : PlayerState()
    data class Error(val channel: Channel?, val message: String, val retryCount: Int = 0) : PlayerState()
}
