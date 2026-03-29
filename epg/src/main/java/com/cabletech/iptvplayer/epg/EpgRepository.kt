package com.cabletech.iptvplayer.epg

import com.cabletech.iptvplayer.epg.db.EpgDao
import com.cabletech.iptvplayer.epg.db.EpgSourceEntity
import com.cabletech.iptvplayer.epg.db.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.UUID
import java.util.zip.GZIPInputStream

/**
 * Fetches, parses, and persists XMLTV EPG data.
 *
 * Handles both plain `.xml` and gzip-compressed `.xml.gz` endpoints.
 */
class EpgRepository(
    private val dao: EpgDao,
    private val client: OkHttpClient,
) {

    // ---- Observe ----

    fun observeSources() = dao.observeSources()

    fun observeSchedule(channelId: String, fromMs: Long, toMs: Long): Flow<List<EpgProgramme>> =
        dao.observeSchedule(channelId, fromMs, toMs).map { list -> list.map { it.toDomain() } }

    // ---- Now / Next ----

    suspend fun getNowPlaying(channelId: String): EpgProgramme? =
        dao.getNowPlaying(channelId, System.currentTimeMillis())?.toDomain()

    suspend fun getNextUp(channelId: String): EpgProgramme? =
        dao.getNextUp(channelId, System.currentTimeMillis())?.toDomain()

    // ---- Sources ----

    suspend fun addSource(name: String, url: String): EpgSourceEntity {
        val entity = EpgSourceEntity(id = UUID.randomUUID().toString(), name = name, url = url)
        dao.upsertSource(entity)
        return entity
    }

    // ---- Refresh ----

    suspend fun refreshSource(sourceId: String) {
        val source = dao.getEnabledSources().firstOrNull { it.id == sourceId }
            ?: throw IllegalArgumentException("Unknown EPG source: $sourceId")
        val programmes = fetchAndParse(source.url)
        // Replace all programmes for channels belonging to this source
        val entities = programmes.map { it.toEntity() }
        // Upsert is safe here — we use REPLACE conflict strategy
        dao.insertAll(entities)
        dao.markSourceRefreshed(sourceId, System.currentTimeMillis())
    }

    suspend fun refreshAll() {
        dao.getEnabledSources().forEach { source ->
            runCatching { refreshSource(source.id) }
        }
    }

    private suspend fun fetchAndParse(url: String): List<EpgProgramme> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw EpgFetchException("HTTP ${response.code} fetching EPG: $url")
                }
                val bodyBytes = response.body?.byteStream()
                    ?: throw EpgFetchException("Empty EPG response: $url")
                val stream: InputStream = if (url.endsWith(".gz", ignoreCase = true))
                    GZIPInputStream(bodyBytes)
                else
                    bodyBytes
                XmltvParser.parse(stream)
            }
        }
}

class EpgFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)
