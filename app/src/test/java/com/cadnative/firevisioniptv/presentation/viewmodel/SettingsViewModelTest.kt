package com.cadnative.firevisioniptv.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.cadnative.firevisioniptv.MainDispatcherRule
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.domain.service.ChannelHealthScanner
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.domain.usecase.RefreshChannelsUseCase
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userPreferencesRepository: UserPreferencesRepository = mockk()
    private val application: Application = mockk(relaxed = true)
    private val channelHealthScanner: ChannelHealthScanner = mockk(relaxed = true)
    private val refreshChannelsUseCase: RefreshChannelsUseCase = mockk()

    private val mockContext: Context = mockk(relaxed = true)
    private val mockPrefs: SharedPreferences = mockk(relaxed = true)
    private val mockEditor: SharedPreferences.Editor = mockk(relaxed = true)
    private val scanProgressFlow = MutableStateFlow(ScanProgress())

    @Before
    fun setup() {
        // Mock QRCodeWriter to avoid Bitmap crash
        mockkConstructor(QRCodeWriter::class)
        every { anyConstructed<QRCodeWriter>().encode(any(), any(), any<Int>(), any<Int>()) } throws WriterException("mocked")

        // Mock AppPreferences static methods
        mockkObject(AppPreferences)
        every { AppPreferences.getServerUrl(any()) } returns "https://tv.cadnative.com/"
        every { AppPreferences.getTvCode(any()) } returns "TESTCODE"
        every { AppPreferences.clearPairing(any()) } returns Unit
        every { AppPreferences.isDemoMode(any()) } returns false

        // Mock Application
        every { application.applicationContext } returns mockContext
        every { application.packageName } returns "com.cadnative.firevisioniptv"
        val packageInfo = PackageInfo().apply {
            versionName = "1.0.0"
            @Suppress("DEPRECATION")
            versionCode = 1
        }
        every { application.packageManager } returns mockk {
            every { getPackageInfo("com.cadnative.firevisioniptv", 0) } returns packageInfo
        }
        every { application.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit

        // Mock preferences repository
        every { userPreferencesRepository.getTheme() } returns flowOf("dark")
        every { userPreferencesRepository.getGridSize() } returns flowOf(3)
        every { userPreferencesRepository.getFontSize() } returns flowOf(1.0f)
        every { userPreferencesRepository.getAnimationSpeed() } returns flowOf(1.0f)
        every { userPreferencesRepository.getLayoutDensity() } returns flowOf("comfortable")
        every { userPreferencesRepository.getBackExitProtection() } returns flowOf(true)
        every { userPreferencesRepository.getPlayerKeyUpDownAction() } returns flowOf(PlayerKeyAction.ZAP)
        every { userPreferencesRepository.getPlayerKeyLeftRightAction() } returns flowOf(PlayerKeyAction.ZAP)
        every { userPreferencesRepository.getPlayerLongOkAction() } returns flowOf(PlayerKeyAction.FAVORITE)
        every { userPreferencesRepository.getSleepTimerDefaultMinutes() } returns flowOf(0)
        every { userPreferencesRepository.getAlwaysShowProgramBar() } returns flowOf(false)

        // Mock scanner
        every { channelHealthScanner.scanProgress } returns scanProgressFlow
    }

    @After
    fun tearDown() {
        unmockkConstructor(QRCodeWriter::class)
        unmockkObject(AppPreferences)
    }

    private fun createViewModel() = SettingsViewModel(
        userPreferencesRepository = userPreferencesRepository,
        application = application,
        channelHealthScanner = channelHealthScanner,
        refreshChannelsUseCase = refreshChannelsUseCase
    )

    @Test
    fun `init loads preferences`() = runTest {
        val vm = createViewModel()
        runCurrent()

        assertEquals("dark", vm.uiState.value.theme)
        assertEquals(3, vm.uiState.value.gridSize)
        assertEquals(1.0f, vm.uiState.value.fontSize)
    }

    @Test
    fun `init loads server config`() = runTest {
        val vm = createViewModel()
        runCurrent()

        assertEquals("https://tv.cadnative.com/", vm.uiState.value.serverUrl)
        assertEquals("TESTCODE", vm.uiState.value.tvCode)
        assertTrue(vm.uiState.value.isPaired)
        assertFalse(vm.uiState.value.isDefaultMode)
    }

    @Test
    fun `init with default tv code sets isDefaultMode`() = runTest {
        every { AppPreferences.getTvCode(any()) } returns "5T6FEP"
        every { AppPreferences.isDemoMode(any()) } returns true

        val vm = createViewModel()
        runCurrent()

        assertTrue(vm.uiState.value.isDefaultMode)
        assertFalse(vm.uiState.value.isPaired)
    }

    @Test
    fun `onServerUrlChange updates state`() = runTest {
        val vm = createViewModel()
        runCurrent()

        vm.onServerUrlChange("https://new.server.com/")

        assertEquals("https://new.server.com/", vm.uiState.value.serverUrl)
        assertFalse(vm.uiState.value.settingsSaved)
    }

    @Test
    fun `onTvCodeChange updates state`() = runTest {
        val vm = createViewModel()
        runCurrent()

        vm.onTvCodeChange("NEWCODE")

        assertEquals("NEWCODE", vm.uiState.value.tvCode)
        assertFalse(vm.uiState.value.settingsSaved)
    }

    @Test
    fun `setTheme calls repository`() = runTest {
        coEvery { userPreferencesRepository.setTheme(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.setTheme("amoled")
        runCurrent()

        coVerify { userPreferencesRepository.setTheme("amoled") }
    }

    @Test
    fun `setTheme error sets error state`() = runTest {
        coEvery { userPreferencesRepository.setTheme(any()) } returns Result.Error(Exception("theme fail"))

        val vm = createViewModel()
        runCurrent()

        vm.setTheme("amoled")
        runCurrent()

        assertEquals("theme fail", vm.uiState.value.error)
    }

    @Test
    fun `setGridSize calls repository`() = runTest {
        coEvery { userPreferencesRepository.setGridSize(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.setGridSize(4)
        runCurrent()

        coVerify { userPreferencesRepository.setGridSize(4) }
    }

    @Test
    fun `setSleepTimerDefaultMinutes calls repository`() = runTest {
        coEvery { userPreferencesRepository.setSleepTimerDefaultMinutes(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.setSleepTimerDefaultMinutes(60)
        runCurrent()

        coVerify { userPreferencesRepository.setSleepTimerDefaultMinutes(60) }
    }

    @Test
    fun `clearCache success refreshes channels`() = runTest {
        coEvery { userPreferencesRepository.clearCache() } returns Result.Success(Unit)
        coEvery { refreshChannelsUseCase(Unit) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.clearCache()
        runCurrent()

        assertFalse(vm.uiState.value.isClearingCache)
        assertTrue(vm.uiState.value.cacheCleared)
        coVerify { refreshChannelsUseCase(Unit) }
    }

    @Test
    fun `clearCache error sets error`() = runTest {
        coEvery { userPreferencesRepository.clearCache() } returns Result.Error(Exception("clear failed"))

        val vm = createViewModel()
        runCurrent()

        vm.clearCache()
        runCurrent()

        assertFalse(vm.uiState.value.isClearingCache)
        assertEquals("clear failed", vm.uiState.value.error)
    }

    @Test
    fun `clearCache resets cacheCleared after delay`() = runTest {
        coEvery { userPreferencesRepository.clearCache() } returns Result.Success(Unit)
        coEvery { refreshChannelsUseCase(Unit) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.clearCache()
        runCurrent()
        assertTrue(vm.uiState.value.cacheCleared)

        advanceTimeBy(3001)
        runCurrent()
        assertFalse(vm.uiState.value.cacheCleared)
    }

    @Test
    fun `triggerLivelinessCheck delegates to scanner`() = runTest {
        val vm = createViewModel()
        runCurrent()

        vm.triggerLivelinessCheck()

        verify { channelHealthScanner.triggerManualScan() }
    }

    @Test
    fun `resetPairing clears pairing`() = runTest {
        val vm = createViewModel()
        runCurrent()

        vm.resetPairing()

        verify { AppPreferences.clearPairing(any()) }
        assertEquals("", vm.uiState.value.tvCode)
        assertFalse(vm.uiState.value.isPaired)
    }

    @Test
    fun `clearError clears both error fields`() = runTest {
        coEvery { userPreferencesRepository.setTheme(any()) } returns Result.Error(Exception("err"))

        val vm = createViewModel()
        runCurrent()

        vm.setTheme("bad")
        runCurrent()
        assertEquals("err", vm.uiState.value.error)

        vm.clearError()

        assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.downloadError)
    }

    @Test
    fun `resetToDefaults sets all preferences to defaults`() = runTest {
        coEvery { userPreferencesRepository.setTheme(any()) } returns Result.Success(Unit)
        coEvery { userPreferencesRepository.setGridSize(any()) } returns Result.Success(Unit)
        coEvery { userPreferencesRepository.setFontSize(any()) } returns Result.Success(Unit)
        coEvery { userPreferencesRepository.setAnimationSpeed(any()) } returns Result.Success(Unit)
        coEvery { userPreferencesRepository.setLayoutDensity(any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        runCurrent()

        vm.resetToDefaults()
        runCurrent()

        coVerify { userPreferencesRepository.setTheme("dark") }
        coVerify { userPreferencesRepository.setGridSize(3) }
        coVerify { userPreferencesRepository.setFontSize(1.0f) }
        coVerify { userPreferencesRepository.setAnimationSpeed(1.0f) }
        coVerify { userPreferencesRepository.setLayoutDensity("comfortable") }
    }
}
