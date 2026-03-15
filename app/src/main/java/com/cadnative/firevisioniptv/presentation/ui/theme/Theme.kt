package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    // Primary — warm amber
    primary = Amber,
    onPrimary = Color(0xFF1A1207),
    primaryContainer = AmberDark,
    onPrimaryContainer = WarmHighlight,

    // Secondary — steel blue
    secondary = SteelBlue,
    onSecondary = Color(0xFF0D1A24),
    secondaryContainer = SteelBlueDark,
    onSecondaryContainer = SteelBlueLight,

    // Tertiary — light amber for accents
    tertiary = AmberLight,
    onTertiary = Color(0xFF1A1207),
    tertiaryContainer = AmberDark,
    onTertiaryContainer = WarmHighlight,

    // Background
    background = BackgroundDark,
    onBackground = TextPrimary,

    // Surface
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,

    // Error — warm terracotta
    error = Error,
    onError = Color(0xFF1A0A06),
    errorContainer = Color(0xFF5C2214),
    onErrorContainer = Color(0xFFF5C4B8),

    // Outline
    outline = Color(0xFF46434A),
    outlineVariant = Color(0xFF2A2830),

    // Inverse
    inverseSurface = Color(0xFFE8E2DC),
    inverseOnSurface = Color(0xFF1A1816),
    inversePrimary = AmberDark,

    // Scrim
    scrim = Color(0xFF05050A),

    // Surface tint
    surfaceTint = Amber
)

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
