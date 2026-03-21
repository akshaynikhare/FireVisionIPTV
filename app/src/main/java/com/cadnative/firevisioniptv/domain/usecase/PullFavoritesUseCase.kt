package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import javax.inject.Inject

class PullFavoritesUseCase @Inject constructor(
    private val repository: FavoriteRepository
) : UseCase<Unit, Result<Unit>>() {

    override suspend fun execute(params: Unit): Result<Unit> {
        return repository.pullFavoritesFromServer()
    }
}
