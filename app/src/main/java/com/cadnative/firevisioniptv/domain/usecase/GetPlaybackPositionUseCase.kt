package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.PlaybackState
import com.cadnative.firevisioniptv.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving playback position.
 * 
 * Retrieves the saved playback position for a channel so users
 * can resume watching from where they left off.
 * 
 * Requirements: US-004.4 (Playback position saved and restored)
 */
class GetPlaybackPositionUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) : FlowUseCase<String, Result<PlaybackState?>>() {
    
    override fun execute(params: String): Flow<Result<PlaybackState?>> {
        return playbackRepository.getPlaybackPosition(params)
    }
}
