package com.cadnative.firevisioniptv.domain.service

import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Tests for ChannelHealthScanner destroy() safety (Fix #2: NetworkOnMainThreadException).
 *
 * The scanner's scan methods use infinite coroutine loops (while(isActive) + delay)
 * which are not compatible with StandardTestDispatcher. Full scan behavior
 * (deleted-channel guards, batched deletes) is covered by ChannelDaoReplaceAllTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelHealthScannerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var scanner: ChannelHealthScanner

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scanner = ChannelHealthScanner(
            channelHealthDao = mockk(relaxed = true),
            channelDao = mockk(relaxed = true),
            thumbnailExtractor = mockk(relaxed = true),
            streamMetricsRepository = mockk(relaxed = true),
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `destroy does not throw when scanner is idle`() {
        scanner.destroy()
        assertFalse(scanner.scanProgress.value.isScanning)
    }

    @Test
    fun `destroy can be called multiple times without crashing`() {
        scanner.destroy()
        scanner.destroy()
        assertFalse(scanner.scanProgress.value.isScanning)
    }

    @Test
    fun `initial scan progress is not scanning`() {
        val progress = scanner.scanProgress.value
        assertFalse(progress.isScanning)
    }
}
