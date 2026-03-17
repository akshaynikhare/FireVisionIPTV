package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// ── Standardized emphasis opacities ──────────────────────────────────
const val EmphasisHigh = 0.87f
const val EmphasisMedium = 0.6f
const val EmphasisDisabled = 0.38f

// ── Background gradient glows ────────────────────────────────────
val AmberGlow = Color(0x08E8A849)       // ~3% opacity Amber
val SteelBlueGlow = Color(0x086B8EAE)   // ~3% opacity Steel Blue

// ── Category colors — muted tones for dark backgrounds ──────────────
// Based on iptv-org/database categories (29 categories)
val CategorySports = Color(0xFF4CAF7A)
val CategoryNews = Color(0xFF5B9BD5)
val CategoryMovies = Color(0xFFC96B6B)
val CategoryEntertainment = Color(0xFF9B7EC8)
val CategoryMusic = Color(0xFFD98A6E)
val CategoryKids = Color(0xFF4DB6AC)
val CategoryDocumentary = Color(0xFF7C9A82)
val CategoryGeneral = Amber
val CategoryAnimation = Color(0xFFE87ECB)
val CategoryBusiness = Color(0xFF6893B8)
val CategoryClassic = Color(0xFFA89078)
val CategoryComedy = Color(0xFFE8C84A)
val CategoryCooking = Color(0xFFE88A5A)
val CategoryCulture = Color(0xFF8B7EC8)
val CategoryEducation = Color(0xFF5AA8D5)
val CategoryFamily = Color(0xFF6AAFAC)
val CategoryInteractive = Color(0xFF7A8ED5)
val CategoryLegislative = Color(0xFF8A9AAE)
val CategoryLifestyle = Color(0xFFB87EC8)
val CategoryOutdoor = Color(0xFF6AAF6A)
val CategoryPublic = Color(0xFF7A96B8)
val CategoryRelax = Color(0xFF80C0A8)
val CategoryReligious = Color(0xFFC8A86A)
val CategorySeries = Color(0xFFAF6A8A)
val CategoryScience = Color(0xFF5AC8D5)
val CategoryShop = Color(0xFFD5A05A)
val CategoryTravel = Color(0xFF5AB8AF)
val CategoryWeather = Color(0xFF68B8E8)
val CategoryAuto = Color(0xFF8AAF6A)
val CategoryXxx = Color(0xFF8A6A6A)

fun categoryColor(category: String): Color = when (category.lowercase(Locale.ROOT)) {
    "sports" -> CategorySports
    "news" -> CategoryNews
    "movies" -> CategoryMovies
    "entertainment" -> CategoryEntertainment
    "music" -> CategoryMusic
    "kids" -> CategoryKids
    "documentary" -> CategoryDocumentary
    "general" -> CategoryGeneral
    "animation" -> CategoryAnimation
    "business" -> CategoryBusiness
    "classic" -> CategoryClassic
    "comedy" -> CategoryComedy
    "cooking" -> CategoryCooking
    "culture" -> CategoryCulture
    "education" -> CategoryEducation
    "family" -> CategoryFamily
    "interactive" -> CategoryInteractive
    "legislative" -> CategoryLegislative
    "lifestyle" -> CategoryLifestyle
    "outdoor" -> CategoryOutdoor
    "public" -> CategoryPublic
    "relax" -> CategoryRelax
    "religious" -> CategoryReligious
    "series" -> CategorySeries
    "science" -> CategoryScience
    "shop" -> CategoryShop
    "travel" -> CategoryTravel
    "weather" -> CategoryWeather
    "auto" -> CategoryAuto
    "xxx" -> CategoryXxx
    else -> Amber
}

fun categoryIcon(category: String): ImageVector = when (category.lowercase(Locale.ROOT)) {
    "sports" -> Icons.Filled.SportsSoccer
    "news" -> Icons.Filled.Newspaper
    "movies" -> Icons.Filled.Movie
    "entertainment" -> Icons.Filled.Tv
    "music" -> Icons.Filled.MusicNote
    "kids" -> Icons.Filled.ChildCare
    "documentary" -> Icons.Filled.Article
    "general" -> Icons.Filled.LiveTv
    "animation" -> Icons.Filled.Movie
    "business" -> Icons.Filled.AccountBalance
    "classic" -> Icons.Filled.Theaters
    "comedy" -> Icons.Filled.EmojiEmotions
    "cooking" -> Icons.Filled.Restaurant
    "culture" -> Icons.Filled.AccountBalance
    "education" -> Icons.Filled.School
    "family" -> Icons.Filled.FamilyRestroom
    "interactive" -> Icons.Filled.SportsEsports
    "legislative" -> Icons.Filled.Gavel
    "lifestyle" -> Icons.Filled.SelfImprovement
    "outdoor" -> Icons.Filled.Park
    "public" -> Icons.Filled.Public
    "relax" -> Icons.Filled.Spa
    "religious" -> Icons.Filled.MenuBook
    "series" -> Icons.Filled.VideoLibrary
    "science" -> Icons.Filled.Science
    "shop" -> Icons.Filled.ShoppingCart
    "travel" -> Icons.Filled.Flight
    "weather" -> Icons.Filled.WbSunny
    "auto" -> Icons.Filled.DirectionsCar
    "xxx" -> Icons.Filled.Block
    else -> Icons.Filled.LiveTv
}

// ── Legacy aliases (keep references working across codebase) ────────
val FireOrange = Amber
val FireOrangeDark = AmberDark
val FireOrangeLight = AmberLight
val FireBlue = SteelBlue
val FireBlueDark = SteelBlueDark
val FireBlueLight = SteelBlueLight
