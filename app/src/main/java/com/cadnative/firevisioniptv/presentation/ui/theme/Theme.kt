package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Composition local to expose whether FireVisionTheme is in dark mode. */
val LocalIsDarkTheme = compositionLocalOf { true }

/** Theme-aware subtle border color: light white on dark, dark on light. */
val subtleBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) SubtleBorderDark else SubtleBorderLight

private val DarkColorScheme = darkColorScheme(
    // Primary — Flame300 (bright amber, high contrast on void surfaces)
    primary = Flame300,
    onPrimary = Flame700,
    primaryContainer = Flame400,
    onPrimaryContainer = Flame50,

    // Secondary — warm accent (Flame400, complementary to primary)
    secondary = Flame400,
    onSecondary = Flame50,
    secondaryContainer = Flame500,
    onSecondaryContainer = Flame50,

    // Tertiary — Flame100 for subtle accent
    tertiary = Flame100,
    onTertiary = Flame700,
    tertiaryContainer = Flame400,
    onTertiaryContainer = Flame50,

    // Background — Void950
    background = Void950,
    onBackground = TextPrimaryDark,

    // Surface — Void800
    surface = Void800,
    onSurface = TextPrimaryDark,
    surfaceVariant = Void700,
    onSurfaceVariant = TextSecondaryDark,

    // Error
    error = ErrorDark,
    onError = Color(0xFF1A0404),
    errorContainer = Color(0xFF5C0A0A),
    onErrorContainer = Color(0xFFFFC4C4),

    // Outline
    outline = Void600,
    outlineVariant = Void700,

    // Inverse
    inverseSurface = Parchment200,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = Flame500,

    // Scrim
    scrim = Color(0xFF050508),

    // Surface tint
    surfaceTint = Flame300
)

private val LightColorScheme = lightColorScheme(
    // Primary — Flame500 (deep amber, sufficient contrast on parchment)
    primary = Flame500,
    onPrimary = Parchment50,
    primaryContainer = Flame100,
    onPrimaryContainer = Flame700,

    // Secondary — warm accent (Flame700, complementary to primary)
    secondary = Flame700,
    onSecondary = Parchment50,
    secondaryContainer = Flame100,
    onSecondaryContainer = Flame700,

    // Tertiary — Flame400
    tertiary = Flame400,
    onTertiary = Parchment50,
    tertiaryContainer = Flame50,
    onTertiaryContainer = Flame700,

    // Background — Parchment50
    background = Parchment50,
    onBackground = TextPrimaryLight,

    // Surface — Parchment200
    surface = Parchment200,
    onSurface = TextPrimaryLight,
    surfaceVariant = Parchment300,
    onSurfaceVariant = TextSecondaryLight,

    // Error
    error = ErrorLight,
    onError = Parchment50,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5C0000),

    // Outline
    outline = Parchment500,
    outlineVariant = Parchment300,

    // Inverse
    inverseSurface = Void800,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = Flame300,

    // Scrim
    scrim = Color(0xFF0A0A12),

    // Surface tint
    surfaceTint = Flame500
)

@Composable
fun FireVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = FireVisionTypography,
            content = content
        )
    }
}
