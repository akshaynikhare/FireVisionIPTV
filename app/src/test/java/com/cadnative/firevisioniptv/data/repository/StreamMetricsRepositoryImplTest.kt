package com.cadnative.firevisioniptv.data.repository

import android.app.Application
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.StreamMetricsDao
import com.cadnative.firevisioniptv.data.source.local.entity.StreamMetricsEntity
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import com.cadnative.firevisioniptv.domain.repository.HealthSyncEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreamMetricsRepositoryImplTest {

    private val streamMetricsDao: StreamMetricsDao = mockk(relaxed = true)
    private val apiService: FireVisionApiService = mockk(relaxed = true)
    private val application: Application = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createRepo() = StreamMetricsRepositoryImpl(
        streamMetricsDao = streamMetricsDao,
        apiService = apiService,
        application = application,
        dispatcher = testDispatcher
    )

    @Before
    fun setup() {
        // By default, getByChannelId returns null — so upsert will be called
        coEvery { streamMetricsDao.getByChannelId(any()) } returns null
        coEvery { streamMetricsDao.upsert(any()) } returns Unit
        coEvery { streamMetricsDao.incrementDead(any(), any()) } returns Unit
        coEvery { streamMetricsDao.incrementAlive(any(), any()) } returns Unit
        coEvery { streamMetricsDao.incrementUnresponsive(any(), any()) } returns Unit
        coEvery { streamMetricsDao.incrementPlay(any(), any()) } returns Unit
    }

    @Test
    fun `reportStreamDead upserts row and increments dead count`() = runTest {
        val repo = createRepo()

        val result = repo.reportStreamDead("ch1", "Stream unavailable")

        assertTrue(result is Result.Success)
        coVerify { streamMetricsDao.upsert(match { it.channelId == "ch1" }) }
        coVerify { streamMetricsDao.incrementDead("ch1", any()) }
    }

    @Test
    fun `reportStreamDead skips upsert when row already exists`() = runTest {
        coEvery { streamMetricsDao.getByChannelId("ch1") } returns StreamMetricsEntity(channelId = "ch1")

        val repo = createRepo()
        repo.reportStreamDead("ch1", null)

        coVerify(exactly = 0) { streamMetricsDao.upsert(any()) }
        coVerify { streamMetricsDao.incrementDead("ch1", any()) }
    }

    @Test
    fun `reportStreamAlive upserts row and increments alive count`() = runTest {
        val repo = createRepo()

        val result = repo.reportStreamAlive("ch2")

        assertTrue(result is Result.Success)
        coVerify { streamMetricsDao.upsert(match { it.channelId == "ch2" }) }
        coVerify { streamMetricsDao.incrementAlive("ch2", any()) }
    }

    @Test
    fun `reportStreamUnresponsive increments unresponsive count`() = runTest {
        val repo = createRepo()

        val result = repo.reportStreamUnresponsive("ch3")

        assertTrue(result is Result.Success)
        coVerify { streamMetricsDao.incrementUnresponsive("ch3", any()) }
    }

    @Test
    fun `reportStreamPlay increments play count`() = runTest {
        val repo = createRepo()

        val result = repo.reportStreamPlay("ch4", proxyPlay = false, streamUrl = "http://stream.m3u8")

        assertTrue(result is Result.Success)
        coVerify { streamMetricsDao.incrementPlay("ch4", any()) }
    }

    @Test
    fun `reportStreamDead returns Success even when api call fails`() = runTest {
        coEvery { apiService.reportStreamStatus(any(), any()) } throws Exception("Network error")

        val repo = createRepo()
        val result = repo.reportStreamDead("ch1", "error")

        // Local DB succeeded; API failure is fire-and-forget
        assertTrue(result is Result.Success)
        coVerify { streamMetricsDao.incrementDead("ch1", any()) }
    }

    @Test
    fun `syncHealthResults increments correct counters per status`() = runTest {
        val repo = createRepo()

        val entries = listOf(
            HealthSyncEntry(channelId = "ch1", status = "alive", responseTimeMs = 100L, timestamp = 1000L),
            HealthSyncEntry(channelId = "ch2", status = "dead", responseTimeMs = 0L, timestamp = 1000L),
            HealthSyncEntry(channelId = "ch3", status = "unresponsive", responseTimeMs = 0L, timestamp = 1000L),
            HealthSyncEntry(channelId = "ch4", status = "online", responseTimeMs = 50L, timestamp = 1000L)
        )

        val result = repo.syncHealthResults(entries)

        assertTrue(result is Result.Success)
        coVerify { streamMetricsDao.incrementAlive("ch1", 1000L) }
        coVerify { streamMetricsDao.incrementDead("ch2", 1000L) }
        coVerify { streamMetricsDao.incrementUnresponsive("ch3", 1000L) }
        coVerify { streamMetricsDao.incrementAlive("ch4", 1000L) }
    }

    @Test
    fun `syncHealthResults returns Success even when api call fails`() = runTest {
        coEvery { apiService.syncHealthResults(any()) } throws Exception("server error")

        val repo = createRepo()
        val result = repo.syncHealthResults(
            listOf(HealthSyncEntry("ch1", "alive", 100L, 1000L))
        )

        assertTrue(result is Result.Success)
    }
}
