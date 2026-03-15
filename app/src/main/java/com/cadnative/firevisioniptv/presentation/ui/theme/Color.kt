package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.Locale

// ── Primary accent — warm amber/gold ────────────────────────────────
val Amber = Color(0xFFE8A849)
val AmberDark = Color(0xFFC4842D)
val AmberLight = Color(0xFFF2C97D)

// ── Secondary accent — cool steel blue ──────────────────────────────
val SteelBlue = Color(0xFF6B8EAE)
val SteelBlueDark = Color(0xFF4A6D8C)
val SteelBlueLight = Color(0xFF8EADC8)

// ── Backgrounds — warm-tinted near-blacks ───────────────────────────
val BackgroundDark = Color(0xFF0A0A0C)
val BackgroundMedium = Color(0xFF0E0E12)
val SurfaceDark = Color(0xFF141418)
val SurfaceVariant = Color(0xFF1C1C22)
val SurfaceElevated = Color(0xFF26262E)

// ── Text ────────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFF0EDE8)
val TextSecondary = Color(0xFF9A9590)
val TextDim = Color(0xFF7A756F)
val TextDisabled = Color(0xFF3D3A37)

// ── Warm highlight — muted cream for subtle emphasis ────────────────
val WarmHighlight = Color(0xFFF5E6D0)

// ── Status ──────────────────────────────────────────────────────────
val Success = Color(0xFF6DAF7B)
val Error = Color(0xFFD4654A)
val Warning = Color(0xFFDEB252)
val Info = Color(0xFF6B8EAE)

// ── Channel health indicator ────────────────────────────────────────
val HealthOnline = Color(0xFF4CAF50)
val HealthChecking = Color(0xFFFFA726)
val HealthOffline = Color(0xFFEF5350)
val HealthUnknown = Color(0xFF616161)

// ── Focus and selection (TV) ────────────────────────────────────────
val FocusGlow = Color(0x40E8A849)
val FocusBorder = Color(0xFFE8A849)
val SelectionOverlay = Color(0x1AFFFFFF)

// ── Borders ─────────────────────────────────────────────────────────
val SubtleBorder = Color(0x10FFFFFF)

// ── Category colors — muted tones for dark backgrounds ──────────────
val CategorySports = Color(0xFF4CAF7A)
val CategoryNews = Color(0xFF5B9BD5)
val CategoryMovies = Color(0xFFC96B6B)
val CategoryEntertainment = Color(0xFF9B7EC8)
val CategoryMusic = Color(0xFFD98A6E)
val CategoryKids = Color(0xFF4DB6AC)
val CategoryDocumentary = Color(0xFF7C9A82)
val CategoryGeneral = Amber

fun categoryColor(category: String): Color = when (category.lowercase(Locale.ROOT)) {
    "sports" -> CategorySports
    "news" -> CategoryNews
    "movies" -> CategoryMovies
    "entertainment" -> CategoryEntertainment
    "music" -> CategoryMusic
    "kids" -> CategoryKids
    "documentary" -> CategoryDocumentary
    "general" -> CategoryGeneral
    else -> Amber
}

// ── Legacy aliases (keep references working across codebase) ────────
val FireOrange = Amber
val FireOrangeDark = AmberDark
val FireOrangeLight = AmberLight
val FireBlue = SteelBlue
val FireBlueDark = SteelBlueDark
val FireBlueLight = SteelBlueLight
