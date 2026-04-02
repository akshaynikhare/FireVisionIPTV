package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.HealthSyncEntry
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncHealthResultsUseCaseTest {

    private lateinit var repository: StreamMetricsRepository
    private lateinit var useCase: SyncHealthResultsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = SyncHealthResultsUseCase(repository)
    }

    @Test
    fun `delegates results list to repository`() = runTest {
        val entries = listOf(
            HealthSyncEntry("ch1", "alive", 250L, 1000L),
            HealthSyncEntry("ch2", "dead", null, 1001L),
        )
        coEvery { repository.syncHealthResults(entries) } returns Result.Success(Unit)

        val result = useCase(entries)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.syncHealthResults(entries) }
    }

    @Test
    fun `empty list is passed through to repository`() = runTest {
        coEvery { repository.syncHealthResults(emptyList()) } returns Result.Success(Unit)

        val result = useCase(emptyList())

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.syncHealthResults(emptyList()) }
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        val exception = Exception("Sync failed")
        val entries = listOf(HealthSyncEntry("ch1", "alive", 100L, 999L))
        coEvery { repository.syncHealthResults(entries) } returns Result.Error(exception)

        val result = useCase(entries)

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `passes all entry fields without modification`() = runTest {
        val entry = HealthSyncEntry("BBC.uk", "unresponsive", 30500L, 1700000000L)
        val captured = mutableListOf<List<HealthSyncEntry>>()
        coEvery { repository.syncHealthResults(capture(captured)) } returns Result.Success(Unit)

        useCase(listOf(entry))

        assertEquals(1, captured.first().size)
        assertEquals("BBC.uk", captured.first()[0].channelId)
        assertEquals("unresponsive", captured.first()[0].status)
        assertEquals(30500L, captured.first()[0].responseTimeMs)
    }
}
