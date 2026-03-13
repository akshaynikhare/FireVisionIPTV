package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all favorite channels.
 * 
 * Returns a Flow that emits the list of favorite channels,
 * automatically updating when favorites are added or removed.
 * 
 * Requirements: US-008 (Enhanced Favorites)
 */
class GetFavoriteChannelsUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : FlowUseCase<Unit, Result<List<Channel>>>() {
    
    override fun execute(params: Unit): Flow<Result<List<Channel>>> {
        return favoriteRepository.getFavoriteChannels()
    }
}
