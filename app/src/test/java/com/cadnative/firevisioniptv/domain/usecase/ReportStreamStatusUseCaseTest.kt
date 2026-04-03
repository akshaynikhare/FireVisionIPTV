package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReportStreamStatusUseCaseTest {

    private lateinit var repository: StreamMetricsRepository
    private lateinit var useCase: ReportStreamStatusUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ReportStreamStatusUseCase(repository)
    }

    @Test
    fun `DEAD status delegates to reportStreamDead with error message`() = runTest {
        coEvery { repository.reportStreamDead("ch1", "timeout") } returns Result.Success(Unit)

        val result = useCase(ReportStreamStatusUseCase.Params("ch1", ReportStreamStatusUseCase.Status.DEAD, "timeout"))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.reportStreamDead("ch1", "timeout") }
    }

    @Test
    fun `DEAD status with null error message passes null`() = runTest {
        coEvery { repository.reportStreamDead("ch1", null) } returns Result.Success(Unit)

        val result = useCase(ReportStreamStatusUseCase.Params("ch1", ReportStreamStatusUseCase.Status.DEAD))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.reportStreamDead("ch1", null) }
    }

    @Test
    fun `ALIVE status delegates to reportStreamAlive`() = runTest {
        coEvery { repository.reportStreamAlive("ch2") } returns Result.Success(Unit)

        val result = useCase(ReportStreamStatusUseCase.Params("ch2", ReportStreamStatusUseCase.Status.ALIVE))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.reportStreamAlive("ch2") }
    }

    @Test
    fun `UNRESPONSIVE status delegates to reportStreamUnresponsive`() = runTest {
        coEvery { repository.reportStreamUnresponsive("ch3") } returns Result.Success(Unit)

        val result = useCase(ReportStreamStatusUseCase.Params("ch3", ReportStreamStatusUseCase.Status.UNRESPONSIVE))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.reportStreamUnresponsive("ch3") }
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        val exception = Exception("Network error")
        coEvery { repository.reportStreamDead(any(), any()) } returns Result.Error(exception)

        val result = useCase(ReportStreamStatusUseCase.Params("ch1", ReportStreamStatusUseCase.Status.DEAD, "error"))

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `does not call wrong repository method for each status`() = runTest {
        coEvery { repository.reportStreamAlive("ch1") } returns Result.Success(Unit)

        useCase(ReportStreamStatusUseCase.Params("ch1", ReportStreamStatusUseCase.Status.ALIVE))

        coVerify(exactly = 0) { repository.reportStreamDead(any(), any()) }
        coVerify(exactly = 0) { repository.reportStreamUnresponsive(any()) }
    }
}
