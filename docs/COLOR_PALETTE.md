# FireVision IPTV — Color Palette Reference

> Unified design system shared with the FireVisionIPTV Server web app.  
> Both apps use the same three-scale palette: **Flame** (brand), **Void** (dark surfaces), **Parchment** (light surfaces).

---

## Brand — Flame

Warm amber-orange. The single primary accent used for interactive elements, focus rings, and active states across both dark and light modes.

| Token       | Hex       | Usage                                       |
|-------------|-----------|---------------------------------------------|
| `flame-50`  | `#FFECC8` | Tint backgrounds, hover overlays            |
| `flame-100` | `#FFD080` | Glow effects, badge tints, highlight bg     |
| `flame-300` | `#F7A93A` | **Dark mode primary** — buttons, focus ring |
| `flame-400` | `#E07818` | Button fills, active nav items              |
| `flame-500` | `#B85E10` | **Light mode primary** — pressed states     |
| `flame-700` | `#7A3A06` | Text on flame-colored backgrounds           |

### Kotlin constants (`Color.kt`)

```kotlin
val Flame300 = Color(0xFFF7A93A)   // dark mode primary
val Flame400 = Color(0xFFE07818)   // button fills
val Flame500 = Color(0xFFB85E10)   // light mode primary
val Flame50  = Color(0xFFFFFCC8)   // tint
val Flame100 = Color(0xFFFFD080)   // glow
val Flame700 = Color(0xFF7A3A06)   // on-flame text
```

---

## Dark Mode — Void

Blue-coal near-blacks. The cool undertone makes Flame accents pop with maximum contrast.

| Token       | Hex       | Usage                                |
|-------------|-----------|--------------------------------------|
| `void-950`  | `#0A0A12` | App background                       |
| `void-900`  | `#11111A` | Sidebar / navigation drawer          |
| `void-800`  | `#191921` | Card background                      |
| `void-700`  | `#21212C` | Elevated card, focused card          |
| `void-600`  | `#2A2B37` | Overlay, modal surface               |
| `void-500`  | `#343542` | Tooltip, highest elevation           |

### Kotlin constants (`Color.kt`)

```kotlin
val Void950 = Color(0xFF0A0A12)
val Void900 = Color(0xFF11111A)
val Void800 = Color(0xFF191921)
val Void700 = Color(0xFF21212C)
val Void600 = Color(0xFF2A2B37)
val Void500 = Color(0xFF343542)
```

---

## Light Mode — Parchment

Warm cream surfaces. Feels premium and editorial; pairs naturally with the amber brand color.

| Token            | Hex       | Usage                       |
|------------------|-----------|-----------------------------|
| `parchment-50`   | `#FAF8F4` | App background              |
| `parchment-100`  | `#F4F0E8` | Content areas               |
| `parchment-200`  | `#EDE8DE` | Card background             |
| `parchment-300`  | `#E2DCD0` | Light border / divider      |
| `parchment-500`  | `#C8C0B0` | Strong border               |
| `parchment-700`  | `#A09080` | Icons, inactive dividers    |

### Kotlin constants (`Color.kt`)

```kotlin
val Parchment50  = Color(0xFFFAF8F4)
val Parchment100 = Color(0xFFF4F0E8)
val Parchment200 = Color(0xFFEDE8DE)
val Parchment300 = Color(0xFFE2DCD0)
val Parchment500 = Color(0xFFC8C0B0)
val Parchment700 = Color(0xFFA09080)
```

---

## Text

### Dark Mode

| Token          | Hex       | Usage                         |
|----------------|-----------|-------------------------------|
| `text-primary` | `#F2EDE3` | Headings, labels              |
| `text-secondary`| `#A8A0B2`| Supporting text, metadata     |
| `text-dim`     | `#706880` | Placeholders, timestamps      |
| `text-disabled`| `#3A3648` | Disabled state                |

### Light Mode

| Token          | Hex       | Usage                         |
|----------------|-----------|-------------------------------|
| `text-primary` | `#1C1A24` | Headings, labels              |
| `text-secondary`| `#5A5470`| Supporting text, metadata     |
| `text-dim`     | `#8C8898` | Placeholders, timestamps      |
| `text-disabled`| `#C4C0D0` | Disabled state                |

---

## Semantic Colors

Used for status badges, alerts, and feedback. Both modes have distinct values optimised for WCAG AA contrast.

| Role        | Dark Mode   | Light Mode  |
|-------------|-------------|-------------|
| **Success** | `#28B560`   | `#1A8A40`   |
| **Error**   | `#E83838`   | `#C02020`   |
| **Warning** | `#F5A624`   | `#D07808`   |
| **Info**    | `#3D88F5`   | `#1A60D0`   |

---

## Channel Health Indicators

Consistent across both dark and light modes (sufficient contrast on all surfaces).

| State     | Color     | Hex       |
|-----------|-----------|-----------|
| Online    | Green     | `#28B560` |
| Checking  | Amber     | `#F5A624` |
| Offline   | Red       | `#E83838` |
| Unknown   | Grey      | `#706880` |

---

## Focus & Interaction (TV)

| Token            | Hex          | Usage                              |
|------------------|--------------|------------------------------------|
| `focus-border`   | `#F7A93A`    | Focused card/item border ring      |
| `focus-glow`     | `#40F7A93A`  | 25% opacity amber glow around focused elements |
| `selection-overlay` | `#1AFFFFFF` | 10% white overlay on selected items |

---

## Theme Mapping

### Material 3 Color Scheme (`Theme.kt`)

| M3 Role              | Dark Value    | Light Value   |
|----------------------|---------------|---------------|
| `primary`            | `Flame300`    | `Flame500`    |
| `onPrimary`          | `#12080A`     | `#FAF8F4`     |
| `primaryContainer`   | `Flame400`    | `Flame100`    |
| `background`         | `Void950`     | `Parchment50` |
| `surface`            | `Void800`     | `Parchment200`|
| `surfaceVariant`     | `Void700`     | `Parchment300`|
| `onBackground`       | text-primary  | text-primary  |
| `onSurface`          | text-primary  | text-primary  |
| `onSurfaceVariant`   | text-secondary| text-secondary|
| `error`              | `#E83838`     | `#C02020`     |
| `outline`            | `Void600`     | `Parchment500`|

---

## Do's and Don'ts

**DO**
- Use `Flame300` (#F7A93A) for all interactive highlights in dark mode
- Use `Flame500` (#B85E10) for interactive highlights in light mode
- Use void scale for dark surfaces — never pure black `#000000`
- Use parchment scale for light surfaces — never pure white `#FFFFFF`
- Use semantic colors only for their intended status meaning

**DON'T**
- Use Material Blue (#2196F3) anywhere — it is not part of this palette
- Mix warm and cool text colors on the same surface
- Use `Flame300` on parchment light backgrounds — contrast is insufficient; use `Flame500`

---

## Cross-Platform Reference

The server web app (Next.js) uses the same palette mapped to Tailwind CSS HSL variables.  
See `FireVisionIPTVServer/frontend/COLOR_PALETTE.md` for the web-side token mapping.
