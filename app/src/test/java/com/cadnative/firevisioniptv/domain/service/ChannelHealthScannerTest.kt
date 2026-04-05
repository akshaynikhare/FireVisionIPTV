package com.cadnative.firevisioniptv.domain.service

import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Tests for ChannelHealthScanner crash fixes:
 * - FK constraint: health results are NOT inserted for deleted channels
 * - Destroy safety: does not throw on main thread
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelHealthScannerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var channelHealthDao: ChannelHealthDao
    private lateinit var channelDao: ChannelDao
    private lateinit var thumbnailExtractor: ChannelThumbnailExtractor
    private lateinit var streamMetricsRepository: StreamMetricsRepository
    private lateinit var scanner: ChannelHealthScanner

    private fun makeChannel(id: String) = ChannelEntity(
        id = id,
        name = "Channel $id",
        // Use invalid URL so checkStream returns immediately without OkHttp
        streamUrl = "invalid://not-a-url",
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
        Dispatchers.setMain(testDispatcher)
        channelHealthDao = mockk(relaxed = true)
        channelDao = mockk(relaxed = true)
        thumbnailExtractor = mockk(relaxed = true)
        streamMetricsRepository = mockk(relaxed = true)

        scanner = ChannelHealthScanner(
            channelHealthDao = channelHealthDao,
            channelDao = channelDao,
            thumbnailExtractor = thumbnailExtractor,
            streamMetricsRepository = streamMetricsRepository,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        scanner.destroy()
        Dispatchers.resetMain()
    }

    @Test
    fun `triggerManualScan skips deleted channels and does not insert health for them`() = runTest(testDispatcher) {
        // Given — health dao returns 2 channel IDs, but channel "2" was deleted
        coEvery { channelHealthDao.getAllChannelIdsByPriority() } returns listOf("1", "2")
        coEvery { channelDao.getChannelByIdSync("1") } returns makeChannel("1")
        coEvery { channelDao.getChannelByIdSync("2") } returns null // deleted

        // When — trigger scan, advance enough for first scan to complete
        scanner.triggerManualScan()
        advanceTimeBy(1000)

        // Cancel the infinite auto-cycle loop so runTest can finish
        scanner.destroy()

        // Then — channel "2" should never be upserted
        coVerify(exactly = 0) {
            channelHealthDao.upsertPreservingThumbnail(
                channelId = "2",
                status = any(),
                lastCheckedAt = any(),
                responseTimeMs = any(),
                errorMessage = any()
            )
        }
        // Channel "1" should have been upserted (CHECKING + result)
        coVerify(atLeast = 1) {
            channelHealthDao.upsertPreservingThumbnail(
                channelId = "1",
                status = any(),
                lastCheckedAt = any(),
                responseTimeMs = any(),
                errorMessage = any()
            )
        }
    }

    @Test
    fun `triggerManualScan re-checks channel existence before saving results`() = runTest(testDispatcher) {
        // Given — channel exists at scan start but gets deleted before result save
        coEvery { channelHealthDao.getAllChannelIdsByPriority() } returns listOf("1")
        // First call (batch URL lookup) returns channel, second call (result save guard) returns null
        coEvery { channelDao.getChannelByIdSync("1") } returnsMany listOf(makeChannel("1"), null)

        // When
        scanner.triggerManualScan()
        advanceTimeBy(1000)

        // Cancel the infinite auto-cycle loop so runTest can finish
        scanner.destroy()

        // Then — CHECKING upsert happens (channel existed at URL lookup time),
        // but final result upsert is skipped (channel deleted before save)
        coVerify(exactly = 1) {
            channelHealthDao.upsertPreservingThumbnail(
                channelId = "1",
                status = "CHECKING",
                lastCheckedAt = any(),
                responseTimeMs = null,
                errorMessage = null
            )
        }
    }

    @Test
    fun `triggerManualScan handles empty channel list gracefully`() = runTest(testDispatcher) {
        // Given — no channels
        coEvery { channelHealthDao.getAllChannelIdsByPriority() } returns emptyList()

        // When
        scanner.triggerManualScan()
        advanceTimeBy(1000)

        // Cancel the infinite auto-cycle loop so runTest can finish
        scanner.destroy()

        // Then — no upserts, no crashes
        coVerify(exactly = 0) {
            channelHealthDao.upsertPreservingThumbnail(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `destroy does not throw when idle`() {
        // When / Then — should not throw
        scanner.destroy()
        assertFalse(scanner.scanProgress.value.isScanning)
    }

    @Test
    fun `destroy cancels active auto scan before it runs`() = runTest(testDispatcher) {
        // Given — start auto scan (has 60s startup delay)
        scanner.startAutoScan()

        // When — destroy before startup delay completes
        scanner.destroy()
        advanceTimeBy(120_000)

        // Then — scan never ran, no health upserts
        coVerify(exactly = 0) {
            channelHealthDao.getAllChannelIdsByPriority()
        }
    }
}
