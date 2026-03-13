package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.presentation.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the settings screen.
 * 
 * Manages user preferences including theme, layout, and accessibility options.
 * 
 * Requirements: US-011 (Customization Options)
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadPreferences()
    }
    
    /**
     * Load all user preferences.
     */
    private fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                userPreferencesRepository.getTheme(),
                userPreferencesRepository.getGridSize(),
                userPreferencesRepository.getFontSize(),
                userPreferencesRepository.getAnimationSpeed(),
                userPreferencesRepository.getLayoutDensity()
            ) { theme, gridSize, fontSize, animationSpeed, layoutDensity ->
                SettingsUiState(
                    theme = theme,
                    gridSize = gridSize,
                    fontSize = fontSize,
                    animationSpeed = animationSpeed,
                    layoutDensity = layoutDensity,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * Set the theme.
     * 
     * @param theme The theme name (e.g., "dark", "amoled", "custom")
     */
    fun setTheme(theme: String) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setTheme(theme)
            handleResult(result, "Failed to update theme")
        }
    }
    
    /**
     * Set the grid size.
     * 
     * @param size The number of columns (2, 3, or 4)
     */
    fun setGridSize(size: Int) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setGridSize(size)
            handleResult(result, "Failed to update grid size")
        }
    }
    
    /**
     * Set the font size.
     * 
     * @param scale The font size scale factor (e.g., 1.0, 1.2, 1.5)
     */
    fun setFontSize(scale: Float) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setFontSize(scale)
            handleResult(result, "Failed to update font size")
        }
    }
    
    /**
     * Set the animation speed.
     * 
     * @param speed The animation speed multiplier (e.g., 0.5, 1.0, 1.5)
     */
    fun setAnimationSpeed(speed: Float) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setAnimationSpeed(speed)
            handleResult(result, "Failed to update animation speed")
        }
    }
    
    /**
     * Set the layout density.
     * 
     * @param density The layout density (e.g., "comfortable", "compact", "spacious")
     */
    fun setLayoutDensity(density: String) {
        viewModelScope.launch {
            val result = userPreferencesRepository.setLayoutDensity(density)
            handleResult(result, "Failed to update layout density")
        }
    }
    
    /**
     * Clear all cached data.
     */
    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = userPreferencesRepository.clearCache()
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    // Show success message (could add a success field to state)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to clear cache"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Reset all settings to defaults.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            setTheme("dark")
            setGridSize(3)
            setFontSize(1.0f)
            setAnimationSpeed(1.0f)
            setLayoutDensity("comfortable")
        }
    }
    
    /**
     * Handle the result of a preference update.
     */
    private fun handleResult(result: Result<Unit>, errorMessage: String) {
        when (result) {
            is Result.Success -> {
                // Success - state will be updated via Flow
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(error = result.exception.message ?: errorMessage)
                }
            }
        }
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
