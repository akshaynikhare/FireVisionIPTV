# ADR-001: Unified Color Palette — Flame / Void / Parchment

## Status

Accepted

## Date

2026-04-02

## Context

The FireVision IPTV Android app and the FireVision IPTV Server web app (Next.js) evolved their color systems independently:

- **Android app** used a warm amber primary (`#E8A849`) with warm-tinted near-black surfaces and **Material Blue (`#2196F3`)** leftover in `colors.xml` for selection/focus states — creating a two-brand conflict.
- **Server web app** used an HSL-based amber primary with cool blue-black surfaces — a different amber shade and different backgrounds than the Android app.
- No shared reference document existed. Changes to one app had no defined impact on the other.
- The app had only a dark mode color scheme. The server supported both light and dark but the light mode used different aesthetic foundations (flat white backgrounds vs warm cream).

This resulted in users experiencing visually inconsistent products across the web admin dashboard and the TV app. The amber accent looked different, surface colors were mismatched, and semantic status colors (signal green, signal red) were different hex values on each platform.

## Decision

Adopt a single unified palette — **Flame / Void / Parchment** — across both apps.

### Palette Summary

**Flame (brand)**
| Token       | Hex       | Role                                      |
|-------------|-----------|-------------------------------------------|
| `flame-300` | `#F7A93A` | Dark mode primary, focus rings, icons     |
| `flame-400` | `#E07818` | Button fills, active nav items            |
| `flame-500` | `#B85E10` | Light mode primary, pressed states        |
| `flame-50`  | `#FFECC8` | Tint backgrounds, hover overlays          |
| `flame-100` | `#FFD080` | Glow effects, focus halos                 |
| `flame-700` | `#7A3A06` | Text rendered on flame-colored surfaces   |

**Void (dark mode surfaces)**
| Token      | Hex       | Role                                 |
|------------|-----------|--------------------------------------|
| `void-950` | `#0A0A12` | App background                       |
| `void-900` | `#11111A` | Sidebar / navigation drawer          |
| `void-800` | `#191921` | Card background                      |
| `void-700` | `#21212C` | Elevated / focused card              |
| `void-600` | `#2A2B37` | Overlay surfaces, modals             |
| `void-500` | `#343542` | Tooltip, highest elevation           |

**Parchment (light mode surfaces)**
| Token           | Hex       | Role                        |
|-----------------|-----------|-----------------------------|
| `parchment-50`  | `#FAF8F4` | App background              |
| `parchment-100` | `#F4F0E8` | Content areas               |
| `parchment-200` | `#EDE8DE` | Card background             |
| `parchment-300` | `#E2DCD0` | Borders, dividers           |
| `parchment-500` | `#C8C0B0` | Strong borders              |
| `parchment-700` | `#A09080` | Icons, inactive elements    |

**Semantic / Signal**
| Role    | Dark Mode   | Light Mode  |
|---------|-------------|-------------|
| Success | `#28B560`   | `#1A8A40`   |
| Error   | `#E83838`   | `#C02020`   |
| Warning | `#F5A624`   | `#D07808`   |
| Info    | `#3D88F5`   | `#1A60D0`   |

### Dual Mode Support Added

The Android app now supports both dark and light modes via `isSystemInDarkTheme()`. The `FireVisionTheme` composable selects the appropriate `ColorScheme` at runtime. Android resource qualifiers (`values-night/`) provide the correct XML colors for legacy View-based components.

### Reference Documents

- `docs/COLOR_PALETTE.md` in this repo — Kotlin token reference
- `FireVisionIPTVServer/frontend/COLOR_PALETTE.md` — CSS/Tailwind token reference

## Alternatives Considered

### Keep app-specific palettes, only sync semantically
Both apps define amber as the primary but allow surface/background shades to differ. Rejected because the visual mismatch would persist and grow over time as each app evolves.

### Adopt the server's existing palette as-is on Android
The server's light mode used flat white backgrounds (unsuitable for premium streaming feel). The amber shade was slightly different. We chose to define a new canonical palette rather than promote either existing implementation.

### Add Material You / Dynamic Color support
Material 3 dynamic color allows users to theme the app based on their wallpaper. Rejected for now because it would break the deliberate Flame brand identity. Can be revisited.

## Consequences

**Positive:**
- Single palette document is the source of truth for both platforms
- Android light mode is now supported — previously unimplemented
- Material Blue completely removed from the Android codebase
- Any future designer or developer has a clear reference

**Negative:**
- Kotlin `Color.kt` and `res/values/colors.xml` required full rewrites
- Existing screenshots and previews show the old palette and need updating
