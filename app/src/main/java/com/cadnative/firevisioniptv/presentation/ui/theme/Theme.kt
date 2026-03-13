package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark color scheme optimized for Android TV viewing.
 * Uses Fire TV inspired orange/red theme with high contrast for TV displays.
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = FireOrange,
    onPrimary = Color.Black,
    primaryContainer = FireOrangeDark,
    onPrimaryContainer = Color.White,
    
    // Secondary colors
    secondary = FireBlue,
    onSecondary = Color.White,
    secondaryContainer = FireBlueDark,
    onSecondaryContainer = Color.White,
    
    // Tertiary colors
    tertiary = FireOrangeLight,
    onTertiary = Color.Black,
    tertiaryContainer = FireOrangeDark,
    onTertiaryContainer = Color.White,
    
    // Background colors
    background = BackgroundDark,
    onBackground = TextPrimary,
    
    // Surface colors
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    // Error colors
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Outline colors
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    // Inverse colors
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = FireOrangeDark,
    
    // Scrim
    scrim = Color.Black,
    
    // Surface tint
    surfaceTint = FireOrange
)

/**
 * FireVision theme for Android TV.
 * 
 * Provides a dark theme optimized for TV viewing with:
 * - High contrast colors for TV displays
 * - Large typography (minimum 16sp) for readability from distance
 * - Fire TV inspired orange/red color scheme
 * - Material Design 3 components
 * 
 * @param content The composable content to apply the theme to
 */
@Composable
fun FireVisionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = FireVisionTypography,
        content = content
    )
}
