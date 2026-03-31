package com.cadnative.firevisioniptv.data.repository

import android.content.Context
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.model.dto.EpgProgramDto
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val apiService: FireVisionApiService,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : EpgRepository {

    private val cache = mutableMapOf<String, List<EpgProgram>>()
    @Volatile private var cacheLoaded = false
    private val loadMutex = Mutex()

    override suspend fun ensureLoaded() = withContext(dispatcher) {
        if (!cacheLoaded) {
            loadMutex.withLock {
                if (!cacheLoaded) {
                    loadGuide()
                }
            }
        }
    }

    override suspend fun getNowNext(tvgId: String): Pair<EpgProgram?, EpgProgram?> =
        withContext(dispatcher) {
            if (!cacheLoaded) {
                loadMutex.withLock {
                    if (!cacheLoaded) {
                        loadGuide()
                    }
                }
            }
            val programs = cache[tvgId] ?: return@withContext Pair(null, null)
            val now = Instant.now()
            val nowProgram = programs.firstOrNull { it.startTime <= now && it.endTime > now }
            val nextProgram = programs
                .filter { it.startTime > now }
                .minByOrNull { it.startTime }
            Pair(nowProgram, nextProgram)
        }

    private suspend fun loadGuide() {
        try {
            val channelListCode = AppPreferences.getTvCode(context)
            val response = apiService.getEpgGuide(channelListCode)
            if (response.isSuccessful) {
                val data = response.body()?.data ?: emptyMap()
                data.forEach { (tvgId, dtos) ->
                    cache[tvgId] = dtos.mapNotNull { it.toDomain() }
                }
                cacheLoaded = true
            }
        } catch (_: Exception) {
            // Network failure — leave cache empty; graceful degradation
        }
    }

    private fun EpgProgramDto.toDomain(): EpgProgram? {
        return try {
            EpgProgram(
                channelEpgId = channelEpgId,
                title = title,
                description = description,
                startTime = Instant.parse(startTime),
                endTime = Instant.parse(endTime),
                icon = icon
            )
        } catch (_: Exception) {
            null
        }
    }
}
