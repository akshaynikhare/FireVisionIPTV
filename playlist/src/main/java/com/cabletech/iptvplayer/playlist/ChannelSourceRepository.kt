package com.cabletech.iptvplayer.playlist

import com.cabletech.iptvplayer.core.model.Channel
import com.cabletech.iptvplayer.core.model.ChannelSource
import com.cabletech.iptvplayer.playlist.db.ChannelDao
import com.cabletech.iptvplayer.playlist.db.ChannelSourceDao
import com.cabletech.iptvplayer.playlist.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Single source of truth for channel sources and their parsed channels.
 *
 * Coordinates between [PlaylistFetcher], [M3uParser], and the Room DAOs.
 */
class ChannelSourceRepository(
    private val sourceDao: ChannelSourceDao,
    private val channelDao: ChannelDao,
    private val fetcher: PlaylistFetcher,
) {

    // ---- Observe ----

    fun observeSources(): Flow<List<ChannelSource>> =
        sourceDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeChannels(): Flow<List<Channel>> =
        channelDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeChannelsByGroup(group: String?): Flow<List<Channel>> =
        channelDao.observeByGroup(group).map { list -> list.map { it.toDomain() } }

    fun observeGroups(): Flow<List<String>> = channelDao.observeGroups()

    // ---- Mutate sources ----

    suspend fun addSource(name: String, url: String): ChannelSource {
        val source = ChannelSource(id = UUID.randomUUID().toString(), name = name, url = url)
        sourceDao.upsert(source.toEntity())
        return source
    }

    suspend fun updateSource(source: ChannelSource) {
        sourceDao.update(source.toEntity())
    }

    suspend fun deleteSource(source: ChannelSource) {
        sourceDao.delete(source.toEntity())
        // cascading delete via FK removes child channels
    }

    suspend fun toggleSource(sourceId: String, enabled: Boolean) {
        sourceDao.getById(sourceId)?.let { entity ->
            sourceDao.update(entity.copy(isEnabled = enabled))
        }
    }

    // ---- Refresh ----

    /**
     * Fetches and re-parses a single source, replacing its channels in the DB.
     *
     * @throws PlaylistFetchException if the network request fails.
     */
    suspend fun refreshSource(sourceId: String) {
        val entity = sourceDao.getById(sourceId)
            ?: throw IllegalArgumentException("Unknown source: $sourceId")
        val content = fetcher.fetch(entity.url)
        val channels = M3uParser.parse(content)
        val channelEntities = channels.mapIndexed { index, channel ->
            channel.toEntity(sourceId = sourceId, sortOrder = index)
        }
        channelDao.deleteBySource(sourceId)
        channelDao.upsertAll(channelEntities)
        sourceDao.markRefreshed(
            id = sourceId,
            timestamp = System.currentTimeMillis(),
            count = channels.size,
        )
    }

    /** Refreshes all enabled sources sequentially. */
    suspend fun refreshAllEnabled() {
        sourceDao.getEnabled().forEach { source ->
            runCatching { refreshSource(source.id) }
                .onFailure { /* log or surface per-source errors to UI */ }
        }
    }
}
