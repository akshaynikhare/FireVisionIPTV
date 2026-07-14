package com.cadnative.firevisioniptv.data.repository

import android.content.Context
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.model.dto.EpgProgramDto
import com.cadnative.firevisioniptv.data.source.local.dao.EpgDao
import com.cadnative.firevisioniptv.data.source.local.entity.EpgProgramEntity
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import com.cadnative.firevisioniptv.data.source.remote.epg.XmltvEpgDataSource
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first, multi-source EPG repository.
 *
 * Room ([EpgDao]) is the persistent store, so the guide survives app restarts and
 * network failures (last-good fallback) instead of blanking. An in-memory cache,
 * hydrated from Room on first access, backs the fast synchronous read path.
 *
 * Sources are merged: the paired server guide (`/tv/epg`) and — TiviMate/Kodi style —
 * a user-supplied XMLTV URL parsed on-device. If every source fails, the previously
 * stored programs remain visible (never wiped). Channel matching is case-insensitive
 * on the tvg-id.
 */
@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val apiService: FireVisionApiService,
    private val xmltvDataSource: XmltvEpgDataSource,
    private val epgDao: EpgDao,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : EpgRepository {

    /** Keyed by normalized (lowercased) tvg-id for case-insensitive matching. */
    private val cache = ConcurrentHashMap<String, List<EpgProgram>>()
    @Volatile private var hydrated = false
    @Volatile private var refreshedThisSession = false
    private val loadMutex = Mutex()

    override suspend fun ensureLoaded() = withContext(dispatcher) {
        ensureReady()
    }

    override suspend fun refreshNow() = withContext(dispatcher) {
        if (!hydrated) {
            loadMutex.withLock {
                if (!hydrated) {
                    hydrateFromRoom()
                    hydrated = true
                }
            }
        }
        loadMutex.withLock {
            if (refreshFromSources()) refreshedThisSession = true
        }
    }

    override suspend fun getNowNext(tvgId: String): Pair<EpgProgram?, EpgProgram?> =
        withContext(dispatcher) {
            ensureReady()
            nowNextFrom(cache[keyOf(tvgId)])
        }

    override fun getNowNextIfCached(tvgId: String): Pair<EpgProgram?, EpgProgram?>? {
        if (!hydrated) return null
        return nowNextFrom(cache[keyOf(tvgId)])
    }

    override suspend fun getProgramsWindow(
        tvgIds: List<String>,
        from: Instant,
        to: Instant
    ): Map<String, List<EpgProgram>> = withContext(dispatcher) {
        ensureReady()
        tvgIds.associateWith { tvgId ->
            (cache[keyOf(tvgId)] ?: emptyList())
                .filter { it.startTime < to && it.endTime > from }
                .sortedBy { it.startTime }
        }
    }

    private fun keyOf(tvgId: String): String = tvgId.trim().lowercase()

    private fun nowNextFrom(programs: List<EpgProgram>?): Pair<EpgProgram?, EpgProgram?> {
        if (programs == null) return Pair(null, null)
        val now = Instant.now()
        val nowProgram = programs.firstOrNull { it.startTime <= now && it.endTime > now }
        val nextProgram = programs
            .filter { it.startTime > now }
            .minByOrNull { it.startTime }
        return Pair(nowProgram, nextProgram)
    }

    /** Hydrate from Room once (survives restart), then refresh from sources once per session. */
    private suspend fun ensureReady() {
        if (!hydrated) {
            loadMutex.withLock {
                if (!hydrated) {
                    hydrateFromRoom()
                    hydrated = true
                }
            }
        }
        if (!refreshedThisSession) {
            loadMutex.withLock {
                if (!refreshedThisSession) {
                    // Latch the session gate only when a refresh actually persisted data, so a
                    // transient failure doesn't wedge an empty/stale guide until app restart.
                    if (refreshFromSources()) refreshedThisSession = true
                }
            }
        }
    }

    private suspend fun hydrateFromRoom() {
        try {
            rebuildCache(epgDao.getAll().map { it.toDomain() })
        } catch (_: Exception) {
            // Cache stays empty; a source refresh may still populate it.
        }
    }

    /**
     * Best-effort refresh across all sources. Merges results; on total failure keeps the
     * last-good cache and Room rows. Only prunes well-past programs after a successful merge.
     */
    private suspend fun refreshFromSources(): Boolean {
        val merged = ArrayList<EpgProgram>()
        merged.addAll(fetchServerGuide())
        val xmltvUrl = AppPreferences.getEffectiveEpgUrl(context)
        if (xmltvUrl.isNotBlank()) {
            merged.addAll(runCatching { xmltvDataSource.fetch(xmltvUrl) }.getOrDefault(emptyList()))
        }

        if (merged.isEmpty()) return false // keep last-good and allow a retry this session

        return try {
            val nowMs = Instant.now().toEpochMilli()
            // Replace the schedule for the refreshed channels (so programs the provider removed
            // or rescheduled disappear), prune long-past rows, then rebuild the read cache.
            // The DAO call is a single Room transaction, so Room can't be left half-updated.
            epgDao.replaceForChannels(
                programs = merged.map { it.toEntity() },
                channelEpgIds = merged.map { it.channelEpgId }.distinct(),
                fromMs = nowMs,
                prunedBeforeMs = Instant.now().minus(Duration.ofHours(6)).toEpochMilli()
            )
            rebuildCache(epgDao.getAll().map { it.toDomain() })
            true
        } catch (_: Exception) {
            false // persistence failed — retain last-good cache, allow retry
        }
    }

    private suspend fun fetchServerGuide(): List<EpgProgram> {
        return try {
            val channelListCode = AppPreferences.getTvCode(context)
            if (channelListCode.isBlank()) return emptyList()
            val response = apiService.getEpgGuide(channelListCode)
            if (!response.isSuccessful) return emptyList()
            val channels = response.body()?.channels ?: return emptyList()
            channels.flatMap { ch ->
                (ch.programs ?: emptyList()).mapNotNull { it.toDomain(ch.channelId) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Rebuild the in-memory cache keyed by normalized tvg-id, deduped by start time. */
    private fun rebuildCache(programs: List<EpgProgram>) {
        val grouped = programs
            .groupBy { keyOf(it.channelEpgId) }
            .mapValues { (_, list) ->
                list.distinctBy { it.startTime }.sortedBy { it.startTime }
            }
        cache.clear()
        cache.putAll(grouped)
    }

    private fun EpgProgramDto.toDomain(channelEpgId: String): EpgProgram? {
        return try {
            EpgProgram(
                channelEpgId = channelEpgId,
                title = title,
                description = description,
                startTime = Instant.parse(start),
                endTime = Instant.parse(end),
                icon = icon
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun EpgProgramEntity.toDomain(): EpgProgram = EpgProgram(
        channelEpgId = channelEpgId,
        title = title,
        description = description,
        startTime = Instant.ofEpochMilli(startTimeMs),
        endTime = Instant.ofEpochMilli(endTimeMs),
        icon = icon
    )

    private fun EpgProgram.toEntity(): EpgProgramEntity = EpgProgramEntity(
        channelEpgId = channelEpgId,
        startTimeMs = startTime.toEpochMilli(),
        endTimeMs = endTime.toEpochMilli(),
        title = title,
        description = description,
        icon = icon
    )
}
