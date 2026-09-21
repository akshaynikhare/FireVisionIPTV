package com.cadnative.firevisioniptv.data.source.local.dao

import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for ChannelDao.replaceAllChannels batched delete logic.
 *
 * Verifies that channel replacement correctly batches delete operations
 * to stay under SQLite's 999 variable limit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelDaoReplaceAllTest {

    private lateinit var dao: ChannelDao
    private val deletedBatches = mutableListOf<List<String>>()

    private fun makeChannel(id: String, streamUrl: String = "http://example.com/$id") = ChannelEntity(
        id = id,
        name = "Channel $id",
        streamUrl = streamUrl,
        logoUrl = null,
        categoryId = "cat1",
        language = "en",
        country = "US",
        groupTitle = "Group",
        tvgId = null,
        tvgName = null,
        isActive = true
    )

    @Before
    fun setup() {
        deletedBatches.clear()
        dao = mockk()
        coEvery { dao.insertChannels(any()) } just Runs
        coEvery { dao.deleteChannelsByIds(any()) } coAnswers {
            deletedBatches.add(firstArg())
        }
        coEvery { dao.replaceAllChannels(any()) } answers { callOriginal() }
        coEvery { dao.remapRenamedChannels(any(), any()) } answers { callOriginal() }
        coEvery { dao.remapChannelReferences(any(), any()) } answers { callOriginal() }
        coEvery { dao.remapFavoriteChannelId(any(), any()) } just Runs
        coEvery { dao.remapChannelHealthChannelId(any(), any()) } just Runs
        coEvery { dao.remapStreamMetricsChannelId(any(), any()) } just Runs
        coEvery { dao.remapPlaybackPositionChannelId(any(), any()) } just Runs
    }

    /** Stub the cached rows the DAO reads before it decides what to delete. */
    private fun givenCached(vararg channels: ChannelEntity) {
        coEvery { dao.getAllChannelIdentities() } returns channels.map {
            ChannelIdentity(id = it.id, streamUrl = it.streamUrl, tvgId = it.tvgId)
        }
    }

    @Test
    fun `replaceAllChannels deletes stale channels not in new list`() = runTest {
        // Given — DB has channels 1-5, new list has 1-3
        givenCached(*(1..5).map { makeChannel(it.toString()) }.toTypedArray())
        val newChannels = (1..3).map { makeChannel(it.toString()) }

        // When
        dao.replaceAllChannels(newChannels)

        // Then — should delete "4" and "5"
        val allDeleted = deletedBatches.flatten().toSet()
        assertTrue("Should delete stale IDs", allDeleted == setOf("4", "5"))
        coVerify { dao.insertChannels(newChannels) }
    }

    @Test
    fun `replaceAllChannels does not delete when all channels match`() = runTest {
        // Given — DB and new list have the same IDs
        givenCached(*(1..3).map { makeChannel(it.toString()) }.toTypedArray())
        val newChannels = (1..3).map { makeChannel(it.toString()) }

        // When
        dao.replaceAllChannels(newChannels)

        // Then — no deletes, only upsert
        coVerify(exactly = 0) { dao.deleteChannelsByIds(any()) }
        coVerify { dao.insertChannels(newChannels) }
    }

    @Test
    fun `replaceAllChannels handles empty database`() = runTest {
        // Given — DB is empty
        givenCached()
        val newChannels = (1..5).map { makeChannel(it.toString()) }

        // When
        dao.replaceAllChannels(newChannels)

        // Then — no deletes, only insert
        coVerify(exactly = 0) { dao.deleteChannelsByIds(any()) }
        coVerify { dao.insertChannels(newChannels) }
    }

    @Test
    fun `replaceAllChannels batches deletes under 900 to avoid SQLite variable limit`() = runTest {
        // Given — DB has 2500 channels, new list has 100 → 2400 stale IDs to delete
        val newChannels = (1..100).map { makeChannel(it.toString()) }
        givenCached(*(1..2500).map { makeChannel(it.toString()) }.toTypedArray())

        // When
        dao.replaceAllChannels(newChannels)

        // Then — 2400 deletes batched into chunks of 900
        val allDeleted = deletedBatches.flatten().toSet()
        val expectedDeleted = (101..2500).map { it.toString() }.toSet()
        assertTrue("Should delete all 2400 stale IDs", allDeleted == expectedDeleted)
        assertTrue(
            "Each batch must be ≤ 900 (SQLite limit safety)",
            deletedBatches.all { it.size <= 900 }
        )
        assertTrue(
            "Should need 3 batches for 2400 IDs (900+900+600)",
            deletedBatches.size == 3
        )
        coVerify { dao.insertChannels(newChannels) }
    }

    @Test
    fun `replaceAllChannels moves user state onto a renamed channel`() = runTest {
        // Given — the source reissued "old-1" as "new-1"; same stream, new id
        givenCached(makeChannel("old-1", "http://example.com/a"), makeChannel("2"))
        val newChannels = listOf(makeChannel("new-1", "http://example.com/a"), makeChannel("2"))

        // When
        dao.replaceAllChannels(newChannels)

        // Then — favorites, health, metrics and resume points follow the channel
        coVerify { dao.remapFavoriteChannelId("old-1", "new-1") }
        coVerify { dao.remapChannelHealthChannelId("old-1", "new-1") }
        coVerify { dao.remapStreamMetricsChannelId("old-1", "new-1") }
        coVerify { dao.remapPlaybackPositionChannelId("old-1", "new-1") }
        assertTrue("Retired id is still deleted", deletedBatches.flatten() == listOf("old-1"))
    }

    @Test
    fun `replaceAllChannels matches a renamed channel regardless of playlist order`() = runTest {
        // Given — the playlist was reordered and re-idded between refreshes
        givenCached(
            makeChannel("m3u-0-a", "http://example.com/a"),
            makeChannel("m3u-1-b", "http://example.com/b")
        )
        val newChannels = listOf(
            makeChannel("hash-b", "http://example.com/b"),
            makeChannel("hash-a", "http://example.com/a")
        )

        // When
        dao.replaceAllChannels(newChannels)

        // Then — position is irrelevant; the stream URL decides
        coVerify { dao.remapChannelReferences("m3u-0-a", "hash-a") }
        coVerify { dao.remapChannelReferences("m3u-1-b", "hash-b") }
    }

    @Test
    fun `replaceAllChannels falls back to the EPG id when a URL is rotated`() = runTest {
        // Given — provider reissued the stream URL but kept the tvg-id
        coEvery { dao.getAllChannelIdentities() } returns listOf(
            ChannelIdentity(id = "old", streamUrl = "http://old/token", tvgId = "CNN.us")
        )
        val newChannels = listOf(
            makeChannel("new", "http://new/token").copy(tvgId = "CNN.us")
        )

        // When
        dao.replaceAllChannels(newChannels)

        // Then
        coVerify { dao.remapChannelReferences("old", "new") }
    }

    @Test
    fun `replaceAllChannels refuses to guess when a key is ambiguous`() = runTest {
        // Given — two cached channels share a stream URL, so neither can be matched
        givenCached(
            makeChannel("old-a", "http://example.com/same"),
            makeChannel("old-b", "http://example.com/same")
        )
        val newChannels = listOf(makeChannel("new-a", "http://example.com/same"))

        // When
        dao.replaceAllChannels(newChannels)

        // Then — moving one channel's favorites onto another is worse than losing them
        coVerify(exactly = 0) { dao.remapChannelReferences(any(), any()) }
    }

    @Test
    fun `replaceAllChannels leaves unchanged channels alone`() = runTest {
        // Given — nothing was renamed
        givenCached(makeChannel("1"), makeChannel("2"))
        val newChannels = listOf(makeChannel("1"), makeChannel("2"))

        // When
        dao.replaceAllChannels(newChannels)

        // Then
        coVerify(exactly = 0) { dao.remapChannelReferences(any(), any()) }
    }

    @Test
    fun `replaceAllChannels handles replacing all channels with empty list`() = runTest {
        // Given — DB has channels, new list is empty (delete everything)
        givenCached(*(1..3).map { makeChannel(it.toString()) }.toTypedArray())
        val newChannels = emptyList<ChannelEntity>()

        // When
        dao.replaceAllChannels(newChannels)

        // Then — all existing channels should be deleted
        val allDeleted = deletedBatches.flatten().toSet()
        assertTrue("Should delete all existing IDs", allDeleted == setOf("1", "2", "3"))
        coVerify { dao.insertChannels(emptyList()) }
    }
}
