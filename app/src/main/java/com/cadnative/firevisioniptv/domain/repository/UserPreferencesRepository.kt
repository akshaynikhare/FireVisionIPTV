package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user preferences and settings.
 * 
 * This interface defines the contract for managing user preferences,
 * including theme, layout, and other customization options.
 */
interface UserPreferencesRepository {
    
    /**
     * Get the selected theme.
     * 
     * @return Flow emitting the current theme name
     */
    fun getTheme(): Flow<String>
    
    /**
     * Set the selected theme.
     * 
     * @param theme The theme name (e.g., "dark", "amoled", "custom")
     * @return Result indicating success or failure
     */
    suspend fun setTheme(theme: String): Result<Unit>
    
    /**
     * Get the grid size preference.
     * 
     * @return Flow emitting the current grid size (number of columns)
     */
    fun getGridSize(): Flow<Int>
    
    /**
     * Set the grid size preference.
     * 
     * @param size The number of columns (e.g., 2, 3, 4)
     * @return Result indicating success or failure
     */
    suspend fun setGridSize(size: Int): Result<Unit>
    
    /**
     * Get the font size preference.
     * 
     * @return Flow emitting the current font size scale factor
     */
    fun getFontSize(): Flow<Float>
    
    /**
     * Set the font size preference.
     * 
     * @param scale The font size scale factor (e.g., 1.0, 1.2, 1.5)
     * @return Result indicating success or failure
     */
    suspend fun setFontSize(scale: Float): Result<Unit>
    
    /**
     * Get the animation speed preference.
     * 
     * @return Flow emitting the current animation speed multiplier
     */
    fun getAnimationSpeed(): Flow<Float>
    
    /**
     * Set the animation speed preference.
     * 
     * @param speed The animation speed multiplier (e.g., 0.5, 1.0, 1.5)
     * @return Result indicating success or failure
     */
    suspend fun setAnimationSpeed(speed: Float): Result<Unit>
    
    /**
     * Get the layout density preference.
     * 
     * @return Flow emitting the current layout density setting
     */
    fun getLayoutDensity(): Flow<String>
    
    /**
     * Set the layout density preference.
     * 
     * @param density The layout density (e.g., "comfortable", "compact", "spacious")
     * @return Result indicating success or failure
     */
    suspend fun setLayoutDensity(density: String): Result<Unit>
    
    /**
     * Clear all cached data.
     *
     * @return Result indicating success or failure
     */
    suspend fun clearCache(): Result<Unit>

    /**
     * Whether pressing back in the player requires a second press to exit.
     */
    fun getBackExitProtection(): Flow<Boolean>

    suspend fun setBackExitProtection(enabled: Boolean): Result<Unit>

    /**
     * Player key actions. Values: [PlayerKeyAction] constants.
     * upDown = D-pad up/down, leftRight = D-pad left/right, longOk = long-press OK.
     */
    fun getPlayerKeyUpDownAction(): Flow<String>

    suspend fun setPlayerKeyUpDownAction(action: String): Result<Unit>

    fun getPlayerKeyLeftRightAction(): Flow<String>

    suspend fun setPlayerKeyLeftRightAction(action: String): Result<Unit>

    fun getPlayerLongOkAction(): Flow<String>

    suspend fun setPlayerLongOkAction(action: String): Result<Unit>

    /**
     * Default sleep timer applied when playback starts, in minutes (0 = off).
     */
    fun getSleepTimerDefaultMinutes(): Flow<Int>

    suspend fun setSleepTimerDefaultMinutes(minutes: Int): Result<Unit>

    /**
     * When true, the player's now/next program bar stays pinned instead of
     * auto-hiding after a zap.
     */
    fun getAlwaysShowProgramBar(): Flow<Boolean>

    suspend fun setAlwaysShowProgramBar(enabled: Boolean): Result<Unit>
}

/**
 * Valid values for the remappable player key actions.
 */
object PlayerKeyAction {
    const val ZAP = "zap"
    const val LAST_CHANNEL = "last_channel"
    const val FAVORITE = "favorite"
    const val PLAY_PAUSE = "play_pause"
}
