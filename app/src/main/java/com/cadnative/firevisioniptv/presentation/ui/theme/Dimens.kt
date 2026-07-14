package com.cadnative.firevisioniptv.presentation.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and size tokens. Baseline 960×540dp TV display.
 * Screens branch on `LocalConfiguration` orientation (portrait = mobile)
 * or `screenWidthDp < 600` (compact = mobile) — Tv/Mobile variants below
 * follow that convention.
 */
object Dimens {
    // ── Base spacing scale (4dp grid) — general-purpose ──────────────
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 24.dp
    val Space6 = 32.dp

    // ── Screen padding ───────────────────────────────────────────────
    val ScreenPaddingHorizontalTv = 40.dp
    val ScreenPaddingHorizontalMobile = 16.dp
    val ScreenPaddingVertical = 28.dp          // TV; branch on isCompact
    val ScreenPaddingVerticalMobile = 14.dp    // phone vertical screen padding

    // ── Row / section spacing ────────────────────────────────────────
    // *Mobile variants are ~40-55% smaller; screens branch on isCompact.
    val RowGap = 36.dp                 // vertical gap between home rows (TV)
    val RowGapMobile = 18.dp           // vertical gap between home rows (mobile)
    val RowTitleGap = 16.dp            // row title → cards (TV)
    val RowTitleGapMobile = 10.dp      // row title → cards (mobile)
    val CardGap = 18.dp                // between cards in a channel row (TV)
    val CardGapMobile = 10.dp          // between cards in a channel row (mobile)
    val HeroCardGap = 24.dp            // between hero/featured cards (TV)
    val HeroCardGapMobile = 14.dp      // between hero/featured cards (mobile)
    val CategoryCardGap = 14.dp        // between category cards
    val GridGap = 16.dp                // channel grid cell spacing

    // ── Channel cards ────────────────────────────────────────────────
    val ChannelCardWidthTv = 180.dp
    val ChannelCardHeightTv = 110.dp
    val ChannelCardWidthMobile = 140.dp
    val ChannelCardHeightMobile = 90.dp
    val GridCardHeight = 120.dp

    // ── Category cards ───────────────────────────────────────────────
    val CategoryCardWidthTv = 180.dp
    val CategoryCardHeightTv = 100.dp
    val CategoryCardWidthMobile = 140.dp
    val CategoryCardHeightMobile = 80.dp

    // ── Icons ────────────────────────────────────────────────────────
    val IconSmall = 16.dp              // badges, inline indicators
    val IconMedium = 24.dp             // buttons, list items
    val IconLarge = 32.dp              // nav rail, prominent actions
    val ChannelLogoSize = 48.dp

    // ── TV on-screen search keyboard (A–Z grid) ──────────────────────
    // 40dp keys keep the 7-row block (6 key rows + action row) inside the
    // 540dp baseline height with slack for overscan; 44dp filled it exactly.
    val KeyboardKeySize = 40.dp             // square letter/digit key
    val KeyboardKeyGap = 6.dp               // gap between keys and rows
    val KeyboardActionKeyMinWidth = 72.dp   // backspace / clear action keys
    val KeyboardColumnGap = 24.dp           // gap between keyboard and results
    val SearchBarHeightTv = 44.dp           // read-only query bar + voice button (TV)
    val SearchHeaderHeightTv = 56.dp        // header band: aligns the "Search" title (left) with the query bar (right) so keyboard + results tops line up

    // ── Side navigation rail ─────────────────────────────────────────
    val NavRailCollapsedTv = 72.dp
    val NavRailCollapsedMobile = 52.dp
    val NavRailExpandedTv = 220.dp
    val NavRailExpandedMobile = 180.dp
    val NavRailBrandIconTv = 56.dp
    val NavRailBrandIconMobile = 40.dp
    // Nav item pill padding + gaps (were inline 2/6/8/10/14dp)
    val NavItemGapTv = 6.dp                 // vertical gap between rail items (TV)
    val NavItemGapMobile = 2.dp             // vertical gap between rail items (mobile)
    val NavItemPaddingHTv = 14.dp           // pill horizontal padding (TV)
    val NavItemPaddingHMobile = 10.dp       // pill horizontal padding (mobile)
    val NavItemPaddingVTv = 10.dp           // pill vertical padding (TV)
    val NavItemPaddingVMobile = 8.dp        // pill vertical padding (mobile)
    val NavItemLabelGap = 14.dp             // icon → label spacing when expanded
    val NavBrandLabelGap = 8.dp             // brand icon → wordmark spacing
    val NavItemIndicatorWidth = 3.dp        // selected-state leading accent bar
    val NavItemIndicatorHeight = 20.dp

    // ── Channel card internals ───────────────────────────────────────
    val CardContentPadding = 10.dp          // text block padding inside a card
    val CardBadgePadding = 8.dp             // top badges inset (health / favorite)
    val CardBadgeBaselineNudge = 3.dp       // lifts the health dot onto the name's baseline
    val CardLogoTopPadding = 6.dp           // logo overlay top inset
    val HealthDotSize = 8.dp                // stream-health status dot
    val LiveDotSize = 6.dp                  // pulsing LIVE indicator dot
    val BadgePaddingV = 2.dp                // tight vertical padding inside small pills (LIVE)

    // ── Player chrome (mobile + overlay upgrades) ────────────────────
    val EpgProgressHeight = 3.dp            // thin live-program progress line
    val OverlayDetailStripHeight = 84.dp    // fixed focused-channel strip in the channel overlay
    val PlayerControlButton = 52.dp         // circular scrim buttons in mobile chrome
    val PlayerTopBarHeight = 64.dp          // mobile landscape top bar
    val PlayerZapRowHeight = 64.dp          // portrait Channels-tab row
    val PlayerZapRowLogo = 44.dp            // logo inside a zap row
    val PlayerPortraitDetailLogo = 52.dp    // channel logo in the portrait detail section
    val GestureIndicatorWidth = 160.dp      // brightness/volume pill level bar width
    val GestureIndicatorBarHeight = 4.dp    // level bar thickness inside the pill
    val TracksPanelMaxWidth = 420.dp        // audio/subtitle panel cap (fills 92% on phones)

    // ── Empty / error / loading states ───────────────────────────────
    val StateMedallionSize = 72.dp          // tinted icon medallion for empty/error states
    val StateTopPadding = 80.dp             // top inset for centered prompt/no-results columns
    val SkeletonTitleWidth = 140.dp         // placeholder section-title bar width
    val SkeletonTitleHeight = 20.dp         // placeholder section-title bar height

    // ── EPG Guide grid (channels × time) ─────────────────────────────
    val GuideChannelColumnWidth = 200.dp    // sticky left channel column
    val GuideRowHeight = 70.dp              // one channel row height
    val GuideTimelineHeight = 44.dp         // top time-axis strip height
    val GuideMinuteWidth = 6.dp             // horizontal px per timeline minute
    val GuideCellGap = 4.dp                 // gap between adjacent program cells
    val GuideCellPadding = 12.dp            // text inset inside a program cell
    val GuideCellMinWidth = 56.dp           // floor width for very short programs
    val GuideNowLineWidth = 2.dp            // vertical "now" indicator thickness
    val GuideChannelLogoSize = 40.dp        // logo in the channel column
    val GuideDetailPanelHeight = 100.dp     // focused-program detail header (compact)
    val GuideTickLabelInset = 6.dp          // time label offset from tick
    val GuideFocusBorderWidth = 2.dp        // border on the focused program/channel cell
    val GuideRowAccentWidth = 3.dp          // category accent bar on the focused row's channel cell
    val GuideFilterChipGap = 10.dp          // gap between filter chips above the grid

    // Mobile (compact) variants — screens branch on isCompact. TV values above
    // are far too large/cramped on a phone; these keep the guide legible and
    // avoid horizontal cramping without regressing the TV layout.
    val GuideChannelColumnWidthMobile = 120.dp  // sticky left channel column (mobile)
    val GuideRowHeightMobile = 60.dp            // one channel row height (mobile)
    val GuideTimelineHeightMobile = 36.dp       // top time-axis strip height (mobile)
    val GuideMinuteWidthMobile = 5.dp           // horizontal px per timeline minute (mobile)
    val GuideCellPaddingMobile = 8.dp           // text inset inside a program cell (mobile)
    val GuideChannelLogoSizeMobile = 28.dp      // logo in the channel column (mobile)
    val GuideDetailPanelHeightMobile = 84.dp    // focused-program detail header (mobile, compact)
}
