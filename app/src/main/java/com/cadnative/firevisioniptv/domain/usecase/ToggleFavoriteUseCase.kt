package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Use case for toggling a channel's favorite status.
 * 
 * If the channel is currently a favorite, it will be removed.
 * If it's not a favorite, it will be added.
 * 
 * Requirements: US-008 (Enhanced Favorites)
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : UseCase<String, Result<Unit>>() {
    
    override suspend fun execute(params: String): Result<Unit> {
        return favoriteRepository.toggleFavorite(params)
    }
}
