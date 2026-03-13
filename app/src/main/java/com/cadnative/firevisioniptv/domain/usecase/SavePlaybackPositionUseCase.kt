package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.PlaybackRepository
import javax.inject.Inject

/**
 * Use case for saving playback position.
 * 
 * Saves the current playback position for a channel so users
 * can resume watching from where they left off.
 * 
 * Requirements: US-004.4 (Playback position saved and restored)
 */
class SavePlaybackPositionUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) : UseCase<SavePlaybackPositionUseCase.Params, Result<Unit>>() {
    
    data class Params(
        val channelId: String,
        val position: Long,
        val duration: Long
    )
    
    override suspend fun execute(params: Params): Result<Unit> {
        return playbackRepository.savePlaybackPosition(
            channelId = params.channelId,
            position = params.position,
            duration = params.duration
        )
    }
}
