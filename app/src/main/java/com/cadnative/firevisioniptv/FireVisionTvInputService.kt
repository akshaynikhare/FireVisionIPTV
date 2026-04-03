package com.cadnative.firevisioniptv

import android.content.Context
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvInputService
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONObject

class FireVisionTvInputService : TvInputService() {

    override fun onCreateSession(inputId: String): Session {
        return FireVisionSession(this)
    }

    @OptIn(UnstableApi::class)
    private inner class FireVisionSession(
        private val ctx: Context
    ) : TvInputService.Session(ctx) {

        private var player: ExoPlayer? = null
        private var currentSurface: Surface? = null
        private var currentVolume: Float = 1.0f

        override fun onSetSurface(surface: Surface?): Boolean {
            currentSurface = surface
            player?.setVideoSurface(surface)
            return true
        }

        override fun onSetStreamVolume(volume: Float) {
            currentVolume = volume
            player?.volume = volume
        }

        override fun onTune(channelUri: Uri): Boolean {
            Log.d(TAG, "onTune: $channelUri")
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING)

            val streamUrl = getStreamUrlFromChannel(channelUri)
            if (streamUrl == null) {
                Log.e(TAG, "No stream URL found for channel: $channelUri")
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN)
                return false
            }

            // Release existing player
            player?.release()

            val exoPlayer = ExoPlayer.Builder(ctx).build().apply {
                setVideoSurface(currentSurface)
                volume = currentVolume
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && isPlaying) {
                            notifyVideoAvailable()
                            Log.d(TAG, "Video available for: $channelUri")
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            notifyVideoAvailable()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}", error)
                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN)
                    }
                })

                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
            }

            player = exoPlayer
            return true
        }

        override fun onSetCaptionEnabled(enabled: Boolean) {}

        override fun onRelease() {
            Log.d(TAG, "Session released")
            player?.release()
            player = null
            currentSurface = null
        }

        private fun getStreamUrlFromChannel(channelUri: Uri): String? {
            val projection = arrayOf(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)

            return try {
                ctx.contentResolver.query(channelUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val internalData = cursor.getString(0)
                        val json = JSONObject(internalData ?: "{}")
                        json.optString("channelUrl").takeIf { it.isNotEmpty() }
                    } else null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading channel data from TIF", e)
                null
            }
        }
    }

    companion object {
        private const val TAG = "FireVisionTvInput"
    }
}
