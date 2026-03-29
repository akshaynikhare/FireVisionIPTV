package com.cabletech.iptvplayer.player.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.cabletech.iptvplayer.core.model.Channel
import com.cabletech.iptvplayer.player.AspectRatio
import com.cabletech.iptvplayer.player.IptvPlayer
import com.cabletech.iptvplayer.player.PlayerState
import com.cabletech.iptvplayer.player.R
import com.cabletech.iptvplayer.player.databinding.ActivityPlayerBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Full-screen playback activity for IPTV streams.
 *
 * Designed for Fire TV D-pad navigation:
 * - DPAD_CENTER / MEDIA_PLAY_PAUSE → toggle play/pause
 * - MEDIA_STOP / BACK → stop and finish
 * - MEDIA_FAST_FORWARD / MEDIA_REWIND → ignored for live streams
 */
class PlayerActivity : FragmentActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: IptvPlayer
    private var currentChannel: Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channel = intent.getParcelableExtra<Channel>(EXTRA_CHANNEL)
            ?: run { finish(); return }
        currentChannel = channel

        player = IptvPlayer(this).also { p ->
            p.exoPlayer.let { binding.playerView.player = it }
        }

        observePlayerState()
        player.play(channel)
    }

    private fun observePlayerState() {
        player.state.onEach { state ->
            when (state) {
                is PlayerState.Loading,
                is PlayerState.Buffering  -> showLoading(true)
                is PlayerState.Playing    -> showLoading(false)
                is PlayerState.Paused     -> showLoading(false)
                is PlayerState.Error      -> onError(state)
                is PlayerState.Idle       -> Unit
            }
        }.launchIn(lifecycleScope)
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun onError(state: PlayerState.Error) {
        showLoading(false)
        if (state.retryCount >= 3) {
            binding.errorText.apply {
                visibility = View.VISIBLE
                text = state.message
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlayPause(); true
            }
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                player.stop(); finish(); true
            }
            KeyEvent.KEYCODE_MENU -> {
                cycleAspectRatio(); true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun togglePlayPause() {
        when (player.state.value) {
            is PlayerState.Playing  -> player.pause()
            is PlayerState.Paused   -> player.resume()
            else                    -> currentChannel?.let { player.play(it) }
        }
    }

    private var aspectRatioIndex = 0
    private fun cycleAspectRatio() {
        val ratios = AspectRatio.entries
        aspectRatioIndex = (aspectRatioIndex + 1) % ratios.size
        player.setAspectRatio(ratios[aspectRatioIndex])
    }

    override fun onPause()  { super.onPause();  player.pause() }
    override fun onResume() { super.onResume(); player.resume() }
    override fun onDestroy() { super.onDestroy(); player.release() }

    companion object {
        private const val EXTRA_CHANNEL = "extra_channel"

        fun intent(context: Context, channel: Channel): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL, channel)
            }
    }
}
