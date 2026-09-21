package com.cadnative.firevisioniptv.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cadnative.firevisioniptv.MainDispatcherRule
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.update.AppUpdater
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The snooze lives in the ViewModel rather than AppUpdater because Settings'
 * explicit "Check for Updates" shares the same AppUpdater.check() call and has to
 * keep surfacing a version the launch overlay is suppressing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, manifest = Config.NONE)
class AppUpdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appUpdater: AppUpdater = mockk(relaxed = true)
    private lateinit var context: Context

    private fun update(version: String, mandatory: Boolean = false) = UpdateInfo(
        versionName = version,
        releaseNotes = "",
        fileSize = "10 MB",
        downloadUrl = "https://example.invalid/app.apk",
        isMandatory = mandatory
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun viewModel() =
        AppUpdateViewModel(appUpdater, context, mainDispatcherRule.testDispatcher)

    @Test
    fun `update is offered when nothing has been snoozed`() = runTest {
        every { appUpdater.check() } returns update("1.6")

        val vm = viewModel()
        vm.checkForUpdate()
        advanceUntilIdle()

        assertEquals("1.6", vm.uiState.value.updateInfo?.versionName)
    }

    @Test
    fun `dismissed version stays suppressed on the next launch`() = runTest {
        every { appUpdater.check() } returns update("1.6")

        val first = viewModel()
        first.checkForUpdate()
        advanceUntilIdle()
        first.dismiss()

        // A fresh ViewModel stands in for the next cold start.
        val second = viewModel()
        second.checkForUpdate()
        advanceUntilIdle()

        assertNull(second.uiState.value.updateInfo)
    }

    @Test
    fun `a mandatory update ignores the snooze`() = runTest {
        every { appUpdater.check() } returns update("1.6")
        val first = viewModel()
        first.checkForUpdate()
        advanceUntilIdle()
        first.dismiss()

        every { appUpdater.check() } returns update("1.6", mandatory = true)
        val second = viewModel()
        second.checkForUpdate()
        advanceUntilIdle()

        assertEquals("1.6", second.uiState.value.updateInfo?.versionName)
    }

    @Test
    fun `a newer version is offered even though an older one was dismissed`() = runTest {
        every { appUpdater.check() } returns update("1.6")
        val first = viewModel()
        first.checkForUpdate()
        advanceUntilIdle()
        first.dismiss()

        every { appUpdater.check() } returns update("1.7")
        val second = viewModel()
        second.checkForUpdate()
        advanceUntilIdle()

        assertEquals("1.7", second.uiState.value.updateInfo?.versionName)
    }

    @Test
    fun `the snooze lapses so a re-cut build under the same version is offered again`() = runTest {
        every { appUpdater.check() } returns update("1.6")
        val first = viewModel()
        first.checkForUpdate()
        advanceUntilIdle()
        first.dismiss()

        // Backdate the snooze past its window.
        val eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
        context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong("update_snooze_at", eightDaysAgo).commit()

        val second = viewModel()
        second.checkForUpdate()
        advanceUntilIdle()

        assertEquals("1.6", second.uiState.value.updateInfo?.versionName)
    }
}
