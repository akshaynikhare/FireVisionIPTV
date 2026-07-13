package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.presentation.ui.player.PlayerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the multiview grid: exposes the first few channels and builds IPTV-tuned
 * players for each pane. Capped at [MAX_PANES] to stay within device decoder limits.
 */
@HiltViewModel
class MultiviewViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val playerFactory: PlayerFactory
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    init {
        viewModelScope.launch {
            getChannelsUseCase(Unit).collect { result ->
                if (result is Result.Success) {
                    // Full list — the screen picks how many panes to show and which
                    // channels fill them (via the layout selector + channel picker).
                    _channels.value = result.data
                }
            }
        }
    }

    fun createPlayer(): ExoPlayer = playerFactory.create()

    companion object {
        const val MAX_PANES = 4
    }
}
