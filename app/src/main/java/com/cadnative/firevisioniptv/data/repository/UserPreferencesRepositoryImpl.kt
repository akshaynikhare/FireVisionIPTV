package com.cadnative.firevisioniptv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserPreferencesRepository using SharedPreferences.
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(prefs.getString(KEY_THEME, "dark") ?: "dark")
    private val _gridSize = MutableStateFlow(prefs.getInt(KEY_GRID_SIZE, 3))
    private val _fontSize = MutableStateFlow(prefs.getFloat(KEY_FONT_SIZE, 1.0f))
    private val _animationSpeed = MutableStateFlow(prefs.getFloat(KEY_ANIMATION_SPEED, 1.0f))
    private val _layoutDensity = MutableStateFlow(prefs.getString(KEY_LAYOUT_DENSITY, "comfortable") ?: "comfortable")

    override fun getTheme(): Flow<String> = _theme.asStateFlow()

    override suspend fun setTheme(theme: String): Result<Unit> {
        return try {
            prefs.edit().putString(KEY_THEME, theme).apply()
            _theme.value = theme
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getGridSize(): Flow<Int> = _gridSize.asStateFlow()

    override suspend fun setGridSize(size: Int): Result<Unit> {
        return try {
            prefs.edit().putInt(KEY_GRID_SIZE, size).apply()
            _gridSize.value = size
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getFontSize(): Flow<Float> = _fontSize.asStateFlow()

    override suspend fun setFontSize(scale: Float): Result<Unit> {
        return try {
            prefs.edit().putFloat(KEY_FONT_SIZE, scale).apply()
            _fontSize.value = scale
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getAnimationSpeed(): Flow<Float> = _animationSpeed.asStateFlow()

    override suspend fun setAnimationSpeed(speed: Float): Result<Unit> {
        return try {
            prefs.edit().putFloat(KEY_ANIMATION_SPEED, speed).apply()
            _animationSpeed.value = speed
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getLayoutDensity(): Flow<String> = _layoutDensity.asStateFlow()

    override suspend fun setLayoutDensity(density: String): Result<Unit> {
        return try {
            prefs.edit().putString(KEY_LAYOUT_DENSITY, density).apply()
            _layoutDensity.value = density
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun clearCache(): Result<Unit> {
        return try {
            context.cacheDir.deleteRecursively()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "firevision_preferences"
        private const val KEY_THEME = "theme"
        private const val KEY_GRID_SIZE = "grid_size"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_LAYOUT_DENSITY = "layout_density"
    }
}
