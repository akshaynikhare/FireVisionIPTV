package com.cadnative.firevisioniptv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserPreferencesRepository using SharedPreferences.
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserPreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    private val _gridSize = MutableStateFlow(prefs.getInt(KEY_GRID_SIZE, 3))
    private val _fontSize = MutableStateFlow(prefs.getFloat(KEY_FONT_SIZE, 1.0f))
    private val _animationSpeed = MutableStateFlow(prefs.getFloat(KEY_ANIMATION_SPEED, 1.0f))
    private val _layoutDensity = MutableStateFlow(prefs.getString(KEY_LAYOUT_DENSITY, "comfortable") ?: "comfortable")
    private val _backExitProtection = MutableStateFlow(prefs.getBoolean(KEY_BACK_EXIT_PROTECTION, true))
    private val _keyUpDownAction = MutableStateFlow(prefs.getString(KEY_PLAYER_KEY_UP_DOWN, PlayerKeyAction.ZAP) ?: PlayerKeyAction.ZAP)
    private val _keyLeftRightAction = MutableStateFlow(prefs.getString(KEY_PLAYER_KEY_LEFT_RIGHT, PlayerKeyAction.ZAP) ?: PlayerKeyAction.ZAP)
    private val _longOkAction = MutableStateFlow(prefs.getString(KEY_PLAYER_LONG_OK, PlayerKeyAction.FAVORITE) ?: PlayerKeyAction.FAVORITE)
    private val _sleepTimerDefaultMinutes = MutableStateFlow(prefs.getInt(KEY_SLEEP_TIMER_DEFAULT, 0))
    private val _alwaysShowProgramBar = MutableStateFlow(prefs.getBoolean(KEY_ALWAYS_SHOW_PROGRAM_BAR, false))

    override fun getTheme(): Flow<String> = _theme.asStateFlow()

    override suspend fun setTheme(theme: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putString(KEY_THEME, theme).apply()
            }
            _theme.value = theme
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getGridSize(): Flow<Int> = _gridSize.asStateFlow()

    override suspend fun setGridSize(size: Int): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putInt(KEY_GRID_SIZE, size).apply()
            }
            _gridSize.value = size
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getFontSize(): Flow<Float> = _fontSize.asStateFlow()

    override suspend fun setFontSize(scale: Float): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putFloat(KEY_FONT_SIZE, scale).apply()
            }
            _fontSize.value = scale
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getAnimationSpeed(): Flow<Float> = _animationSpeed.asStateFlow()

    override suspend fun setAnimationSpeed(speed: Float): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putFloat(KEY_ANIMATION_SPEED, speed).apply()
            }
            _animationSpeed.value = speed
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getLayoutDensity(): Flow<String> = _layoutDensity.asStateFlow()

    override suspend fun setLayoutDensity(density: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putString(KEY_LAYOUT_DENSITY, density).apply()
            }
            _layoutDensity.value = density
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun clearCache(): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                // Only clear contents inside cacheDir, not cacheDir itself.
                // This preserves the directory and avoids breaking other libraries
                // (Coil, OkHttp) that expect cacheDir to exist.
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getBackExitProtection(): Flow<Boolean> = _backExitProtection.asStateFlow()

    override suspend fun setBackExitProtection(enabled: Boolean): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putBoolean(KEY_BACK_EXIT_PROTECTION, enabled).apply()
            }
            _backExitProtection.value = enabled
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getPlayerKeyUpDownAction(): Flow<String> = _keyUpDownAction.asStateFlow()

    override suspend fun setPlayerKeyUpDownAction(action: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putString(KEY_PLAYER_KEY_UP_DOWN, action).apply()
            }
            _keyUpDownAction.value = action
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getPlayerKeyLeftRightAction(): Flow<String> = _keyLeftRightAction.asStateFlow()

    override suspend fun setPlayerKeyLeftRightAction(action: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putString(KEY_PLAYER_KEY_LEFT_RIGHT, action).apply()
            }
            _keyLeftRightAction.value = action
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getPlayerLongOkAction(): Flow<String> = _longOkAction.asStateFlow()

    override suspend fun setPlayerLongOkAction(action: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putString(KEY_PLAYER_LONG_OK, action).apply()
            }
            _longOkAction.value = action
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getSleepTimerDefaultMinutes(): Flow<Int> = _sleepTimerDefaultMinutes.asStateFlow()

    override suspend fun setSleepTimerDefaultMinutes(minutes: Int): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putInt(KEY_SLEEP_TIMER_DEFAULT, minutes).apply()
            }
            _sleepTimerDefaultMinutes.value = minutes
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getAlwaysShowProgramBar(): Flow<Boolean> = _alwaysShowProgramBar.asStateFlow()

    override suspend fun setAlwaysShowProgramBar(enabled: Boolean): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                prefs.edit().putBoolean(KEY_ALWAYS_SHOW_PROGRAM_BAR, enabled).apply()
            }
            _alwaysShowProgramBar.value = enabled
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "firevision_preferences"
        private const val KEY_SLEEP_TIMER_DEFAULT = "sleep_timer_default_minutes"
        private const val KEY_ALWAYS_SHOW_PROGRAM_BAR = "always_show_program_bar"
        private const val KEY_BACK_EXIT_PROTECTION = "back_exit_protection"
        private const val KEY_PLAYER_KEY_UP_DOWN = "player_key_up_down"
        private const val KEY_PLAYER_KEY_LEFT_RIGHT = "player_key_left_right"
        private const val KEY_PLAYER_LONG_OK = "player_long_ok"
        private const val KEY_THEME = "theme"
        private const val KEY_GRID_SIZE = "grid_size"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_LAYOUT_DENSITY = "layout_density"
    }
}
