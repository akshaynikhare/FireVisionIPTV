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

    private fun makeChannel(id: String) = ChannelEntity(
        id = id,
        name = "Channel $id",
        streamUrl = "http://example.com/$id",
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
    }

    @Test
    fun `replaceAllChannels deletes stale channels not in new list`() = runTest {
        // Given — DB has channels 1-5, new list has 1-3
        coEvery { dao.getAllChannelIds() } returns listOf("1", "2", "3", "4", "5")
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
        coEvery { dao.getAllChannelIds() } returns listOf("1", "2", "3")
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
        coEvery { dao.getAllChannelIds() } returns emptyList()
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
        val existingIds = (1..2500).map { it.toString() }
        val newChannels = (1..100).map { makeChannel(it.toString()) }
        coEvery { dao.getAllChannelIds() } returns existingIds

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
    fun `replaceAllChannels handles replacing all channels with empty list`() = runTest {
        // Given — DB has channels, new list is empty (delete everything)
        coEvery { dao.getAllChannelIds() } returns listOf("1", "2", "3")
        val newChannels = emptyList<ChannelEntity>()

        // When
        dao.replaceAllChannels(newChannels)

        // Then — all existing channels should be deleted
        val allDeleted = deletedBatches.flatten().toSet()
        assertTrue("Should delete all existing IDs", allDeleted == setOf("1", "2", "3"))
        coVerify { dao.insertChannels(emptyList()) }
    }
}
