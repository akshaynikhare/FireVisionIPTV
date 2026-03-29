package com.cabletech.iptvplayer.playlist

import com.cabletech.iptvplayer.core.model.Channel
import com.cabletech.iptvplayer.core.model.ChannelSource
import com.cabletech.iptvplayer.playlist.db.ChannelDao
import com.cabletech.iptvplayer.playlist.db.ChannelEntity
import com.cabletech.iptvplayer.playlist.db.ChannelSourceDao
import com.cabletech.iptvplayer.playlist.db.ChannelSourceEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChannelSourceRepositoryTest {

    private val sourceDao = mockk<ChannelSourceDao>(relaxed = true)
    private val channelDao = mockk<ChannelDao>(relaxed = true)
    private val fetcher = mockk<PlaylistFetcher>()

    private val repo = ChannelSourceRepository(sourceDao, channelDao, fetcher)

    private val sampleM3u = """
        #EXTM3U
        #EXTINF:-1 tvg-id="bbc1" tvg-name="BBC One" group-title="UK",BBC One
        http://stream.example.com/bbc1
    """.trimIndent()

    @Test
    fun `refreshSource fetches parses and persists channels`() = runTest {
        val sourceId = "src-1"
        coEvery { sourceDao.getById(sourceId) } returns ChannelSourceEntity(
            id = sourceId,
            name = "Test Source",
            url = "http://example.com/playlist.m3u",
        )
        coEvery { fetcher.fetch(any()) } returns sampleM3u

        repo.refreshSource(sourceId)

        coVerify { channelDao.deleteBySource(sourceId) }
        coVerify { channelDao.upsertAll(any()) }
        coVerify { sourceDao.markRefreshed(sourceId, any(), 1) }
    }

    @Test
    fun `refreshSource throws when source not found`() = runTest {
        coEvery { sourceDao.getById(any()) } returns null
        runCatching { repo.refreshSource("missing") }.let { result ->
            assert(result.isFailure)
            assert(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun `refreshAllEnabled skips failed sources and continues`() = runTest {
        coEvery { sourceDao.getEnabled() } returns listOf(
            ChannelSourceEntity(id = "s1", name = "OK", url = "http://ok.example.com/ok.m3u"),
            ChannelSourceEntity(id = "s2", name = "Bad", url = "http://bad.example.com/bad.m3u"),
        )
        coEvery { sourceDao.getById("s1") } returns ChannelSourceEntity(id = "s1", name = "OK", url = "http://ok.example.com/ok.m3u")
        coEvery { sourceDao.getById("s2") } returns ChannelSourceEntity(id = "s2", name = "Bad", url = "http://bad.example.com/bad.m3u")
        coEvery { fetcher.fetch("http://ok.example.com/ok.m3u") } returns sampleM3u
        coEvery { fetcher.fetch("http://bad.example.com/bad.m3u") } throws PlaylistFetchException("Network error")

        // Should not throw
        repo.refreshAllEnabled()

        coVerify(exactly = 1) { channelDao.deleteBySource("s1") }
        // s2 failed before we reached deleteBySource
    }
}
