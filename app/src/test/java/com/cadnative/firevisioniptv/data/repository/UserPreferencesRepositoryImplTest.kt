package com.cadnative.firevisioniptv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cadnative.firevisioniptv.data.model.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryImplTest {

    private val context: Context = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)
    // UnconfinedTestDispatcher executes eagerly — compatible with runTest without separate scheduling
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getString("theme", "system") } returns "dark"
        every { prefs.getInt("grid_size", 3) } returns 3
        every { prefs.getFloat("font_size", 1.0f) } returns 1.0f
        every { prefs.getFloat("animation_speed", 1.0f) } returns 1.0f
        every { prefs.getString("layout_density", "comfortable") } returns "comfortable"
        every { prefs.getBoolean("back_exit_protection", true) } returns true
        every { prefs.getString("player_key_up_down", any()) } returns "zap"
        every { prefs.getString("player_key_left_right", any()) } returns "zap"
        every { prefs.getString("player_long_ok", any()) } returns "favorite"
        every { prefs.getInt("sleep_timer_default_minutes", 0) } returns 0
        every { prefs.getBoolean("always_show_program_bar", false) } returns false
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.apply() } returns Unit
    }

    private fun createRepo() = UserPreferencesRepositoryImpl(context, testDispatcher)

    @Test
    fun `getTheme emits initial value from SharedPreferences`() = runTest {
        val repo = createRepo()
        assertEquals("dark", repo.getTheme().first())
    }

    @Test
    fun `getGridSize emits initial value from SharedPreferences`() = runTest {
        val repo = createRepo()
        assertEquals(3, repo.getGridSize().first())
    }

    @Test
    fun `getFontSize emits initial value from SharedPreferences`() = runTest {
        val repo = createRepo()
        assertEquals(1.0f, repo.getFontSize().first())
    }

    @Test
    fun `getAnimationSpeed emits initial value from SharedPreferences`() = runTest {
        val repo = createRepo()
        assertEquals(1.0f, repo.getAnimationSpeed().first())
    }

    @Test
    fun `getLayoutDensity emits initial value from SharedPreferences`() = runTest {
        val repo = createRepo()
        assertEquals("comfortable", repo.getLayoutDensity().first())
    }

    @Test
    fun `setTheme persists to prefs and updates flow`() = runTest {
        val repo = createRepo()

        val result = repo.setTheme("amoled")

        assertTrue(result is Result.Success)
        verify { editor.putString("theme", "amoled") }
        verify { editor.apply() }
        assertEquals("amoled", repo.getTheme().first())
    }

    @Test
    fun `setGridSize persists to prefs and updates flow`() = runTest {
        val repo = createRepo()

        val result = repo.setGridSize(4)

        assertTrue(result is Result.Success)
        verify { editor.putInt("grid_size", 4) }
        assertEquals(4, repo.getGridSize().first())
    }

    @Test
    fun `setFontSize persists to prefs and updates flow`() = runTest {
        val repo = createRepo()

        val result = repo.setFontSize(1.5f)

        assertTrue(result is Result.Success)
        verify { editor.putFloat("font_size", 1.5f) }
        assertEquals(1.5f, repo.getFontSize().first())
    }

    @Test
    fun `setAnimationSpeed persists to prefs and updates flow`() = runTest {
        val repo = createRepo()

        val result = repo.setAnimationSpeed(0.5f)

        assertTrue(result is Result.Success)
        verify { editor.putFloat("animation_speed", 0.5f) }
        assertEquals(0.5f, repo.getAnimationSpeed().first())
    }

    @Test
    fun `setLayoutDensity persists to prefs and updates flow`() = runTest {
        val repo = createRepo()

        val result = repo.setLayoutDensity("compact")

        assertTrue(result is Result.Success)
        verify { editor.putString("layout_density", "compact") }
        assertEquals("compact", repo.getLayoutDensity().first())
    }

    @Test
    fun `getSleepTimerDefaultMinutes emits initial value from SharedPreferences`() = runTest {
        val repo = createRepo()
        assertEquals(0, repo.getSleepTimerDefaultMinutes().first())
    }

    @Test
    fun `setSleepTimerDefaultMinutes persists to prefs and updates flow`() = runTest {
        val repo = createRepo()

        val result = repo.setSleepTimerDefaultMinutes(60)

        assertTrue(result is Result.Success)
        verify { editor.putInt("sleep_timer_default_minutes", 60) }
        assertEquals(60, repo.getSleepTimerDefaultMinutes().first())
    }

    @Test
    fun `clearCache returns Success when cacheDir has no files`() = runTest {
        val cacheDir = mockk<File>()
        every { cacheDir.listFiles() } returns null
        every { context.cacheDir } returns cacheDir

        val repo = createRepo()
        val result = repo.clearCache()

        assertTrue(result is Result.Success)
    }
}
