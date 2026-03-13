package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Use case for reordering favorite channels.
 * 
 * Updates the display order of a favorite channel to allow
 * users to customize the order of their favorites.
 * 
 * Requirements: US-008.3 (Favorites reordering)
 */
class ReorderFavoritesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : UseCase<ReorderFavoritesUseCase.Params, Result<Unit>>() {
    
    data class Params(
        val channelId: String,
        val newOrder: Int
    )
    
    override suspend fun execute(params: Params): Result<Unit> {
        return favoriteRepository.updateFavoriteOrder(params.channelId, params.newOrder)
    }
}
