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

class ReportStreamPlayUseCaseTest {

    private lateinit var repository: StreamMetricsRepository
    private lateinit var useCase: ReportStreamPlayUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ReportStreamPlayUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository with channel id`() = runTest {
        coEvery { repository.reportStreamPlay("ch1", false, null) } returns Result.Success(Unit)

        val result = useCase(ReportStreamPlayUseCase.Params("ch1"))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.reportStreamPlay("ch1", false, null) }
    }

    @Test
    fun `passes proxyPlay true when stream used proxy`() = runTest {
        coEvery { repository.reportStreamPlay("ch1", true, null) } returns Result.Success(Unit)

        val result = useCase(ReportStreamPlayUseCase.Params("ch1", proxyPlay = true))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.reportStreamPlay("ch1", true, null) }
    }

    @Test
    fun `passes streamUrl when an alternate stream played`() = runTest {
        val altUrl = "https://alt.example.com/live.m3u8"
        coEvery { repository.reportStreamPlay("ch1", false, altUrl) } returns Result.Success(Unit)

        val result = useCase(ReportStreamPlayUseCase.Params("ch1", streamUrl = altUrl))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.reportStreamPlay("ch1", false, altUrl) }
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        val exception = Exception("Server unavailable")
        coEvery { repository.reportStreamPlay(any(), any(), any()) } returns Result.Error(exception)

        val result = useCase(ReportStreamPlayUseCase.Params("ch1"))

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `proxyPlay defaults to false and streamUrl defaults to null`() = runTest {
        coEvery { repository.reportStreamPlay("ch1", false, null) } returns Result.Success(Unit)

        useCase(ReportStreamPlayUseCase.Params("ch1"))

        coVerify(exactly = 1) { repository.reportStreamPlay("ch1", false, null) }
    }
}
