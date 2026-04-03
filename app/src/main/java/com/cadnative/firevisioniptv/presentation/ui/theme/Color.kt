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

// ── Flame — brand primary ────────────────────────────────────────────
// Dark mode primary: Flame300. Light mode primary: Flame500.
val Flame300 = Color(0xFFF7A93A)   // bright amber — dark mode primary, focus rings
val Flame400 = Color(0xFFE07818)   // rich amber — button fills, active nav
val Flame500 = Color(0xFFB85E10)   // deep amber — light mode primary, pressed
val Flame50  = Color(0xFFFEECC8)   // tint — hover backgrounds, badge tints
val Flame100 = Color(0xFFFFD080)   // glow overlays, focus halos
val Flame700 = Color(0xFF7A3A06)   // text on flame-colored surfaces

// ── Backward-compatible aliases ──────────────────────────────────────
val Amber      = Flame300
val AmberLight = Flame100
val AmberDark  = Flame500

// ── Void — dark mode surfaces (blue-coal) ────────────────────────────
val Void950 = Color(0xFF0A0A12)    // app background
val Void900 = Color(0xFF11111A)    // sidebar / navigation drawer
val Void800 = Color(0xFF191921)    // card background
val Void700 = Color(0xFF21212C)    // elevated / focused card
val Void600 = Color(0xFF2A2B37)    // overlay, modal surface
val Void500 = Color(0xFF343542)    // tooltip, highest elevation

// ── Backward-compatible aliases ──────────────────────────────────────
val BackgroundDark   = Void950
val BackgroundMedium = Void900
val SurfaceDark      = Void800
val SurfaceVariant   = Void700
val SurfaceElevated  = Void600

// ── Parchment — light mode surfaces (warm cream) ─────────────────────
val Parchment50  = Color(0xFFFAF8F4)   // app background
val Parchment100 = Color(0xFFF4F0E8)   // content areas
val Parchment200 = Color(0xFFEDE8DE)   // card background
val Parchment300 = Color(0xFFE2DCD0)   // borders, dividers
val Parchment500 = Color(0xFFC8C0B0)   // strong borders
val Parchment700 = Color(0xFFA09080)   // icons, inactive elements

// ── Text — dark mode ─────────────────────────────────────────────────
val TextPrimaryDark   = Color(0xFFF2EDE3)
val TextSecondaryDark = Color(0xFFA8A0B2)
val TextDimDark       = Color(0xFF706880)
val TextDisabledDark  = Color(0xFF3A3648)

// ── Text — light mode ────────────────────────────────────────────────
val TextPrimaryLight   = Color(0xFF1C1A24)
val TextSecondaryLight = Color(0xFF5A5470)
val TextDimLight       = Color(0xFF8C8898)
val TextDisabledLight  = Color(0xFFC4C0D0)

// ── Backward-compatible aliases (resolves to dark defaults) ──────────
val TextPrimary   = TextPrimaryDark
val TextSecondary = TextSecondaryDark
val TextDim       = TextDimDark
val TextDisabled  = TextDisabledDark

// ── Warm highlight ───────────────────────────────────────────────────
val WarmHighlight = Parchment100

// ── Semantic — dark mode ─────────────────────────────────────────────
val SuccessDark = Color(0xFF28B560)
val ErrorDark   = Color(0xFFE83838)
val WarningDark = Color(0xFFF5A624)
val InfoDark    = Color(0xFF3D88F5)

// ── Semantic — light mode ────────────────────────────────────────────
val SuccessLight = Color(0xFF1A8A40)
val ErrorLight   = Color(0xFFC02020)
val WarningLight = Color(0xFFD07808)
val InfoLight    = Color(0xFF1A60D0)

// ── Backward-compatible aliases ──────────────────────────────────────
val Success  = SuccessDark
val Error    = ErrorDark
val Warning  = WarningDark
val Info     = InfoDark
val SteelBlue      = InfoDark
val SteelBlueDark  = InfoLight
val SteelBlueLight = Color(0xFF7AAEF8)

// ── Channel health ───────────────────────────────────────────────────
val HealthOnline   = SuccessDark    // #28B560
val HealthChecking = WarningDark    // #F5A624
val HealthOffline  = ErrorDark      // #E83838
val HealthUnknown  = TextDimDark    // #706880

// ── Focus and selection (TV) ─────────────────────────────────────────
val FocusGlow        = Color(0x40F7A93A)   // 25% Flame300
val FocusBorder      = Flame300
val SelectionOverlay = Color(0x1AFFFFFF)

// ── Borders ──────────────────────────────────────────────────────────
val SubtleBorderDark  = Color(0x10FFFFFF)   // ~6% white on dark surfaces
val SubtleBorderLight = Color(0x20000000)   // ~12% black on light surfaces
val SubtleBorder = SubtleBorderDark         // Backward compat — prefer subtleBorder() composable

// ── Standardized emphasis opacities ──────────────────────────────────
const val EmphasisHigh     = 0.87f
const val EmphasisMedium   = 0.6f
const val EmphasisDisabled = 0.38f

// ── Background gradient glows ────────────────────────────────────────
val AmberGlow    = Color(0x0AF7A93A)   // ~4% Flame300
val SteelBlueGlow = Color(0x0A3D88F5)  // ~4% Info blue

// ── Category colors — muted tones for dark backgrounds ───────────────
// Based on iptv-org/database categories (29 categories)
val CategorySports        = Color(0xFF4CAF7A)
val CategoryNews          = Color(0xFF5B9BD5)
val CategoryMovies        = Color(0xFFC96B6B)
val CategoryEntertainment = Color(0xFF9B7EC8)
val CategoryMusic         = Color(0xFFD98A6E)
val CategoryKids          = Color(0xFF4DB6AC)
val CategoryDocumentary   = Color(0xFF7C9A82)
val CategoryGeneral       = Flame300
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
    else -> Flame300
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
