# Player Key Contract & Navigation Flow

## Overview

The player follows one rule: **every D-pad press reveals visible UI — nothing hidden, nothing destructive.** Channel changes only ever happen from the visible channel list, the dedicated CH± keys, digit entry, or last-channel recall. BACK always means "back out one level", exactly once per press.

Input is handled in `presentation/ui/screens/player/PlayerKeyHandler.kt` (pure function, unit-tested in `PlayerKeyHandlerTest`) with BACK consumed at the root Box in `PlayerScreen.kt`. `BackHandler` is mobile-only — on TV the BACK key never reaches it while a node is focused.

---

## Key Contract

### Context 1 — Bare player (video only)

| Key | Action |
|-----|--------|
| OK (short press) | Open channel overlay |
| OK (hold 600ms) | `longOkAction` — default: toggle Favorite (visible twin: bar button) |
| ▲ ▼ | `keyUpDownAction` — default: open Controls bar |
| ◀ ▶ | `keyLeftRightAction` — default: open Controls bar |
| CH+ / CH− (MEDIA_NEXT/PREV) | Zap next / previous (one press = one channel) |
| ⏪ / LAST_CH | Last-channel recall (visible twin: "LAST" card in overlay) |
| 0–9 | Channel number entry (on-screen chip, commits after 2s, max 4 digits) |
| INFO | Info bar + key-hint strip |
| MENU | Open Controls bar |
| PLAY / PAUSE / PLAY_PAUSE | Transport (visible twin: bar button) |
| SETTINGS / SEARCH | Navigate to Settings / Search |
| BACK | Exit player |

▲▼ and ◀▶ are remappable in Settings → Player Controls (Zap / Last Ch / Favorite / Play-Pause / Menu). Defaults are "Menu" on both axes so no stray press ever switches a channel.

### Context 2 — Controls bar focused

| Key | Action |
|-----|--------|
| ◀ ▶ | Move between buttons (native focus traversal) |
| OK | Activate button |
| ▲ / ▼ / MENU / BACK | Close the bar, back to bare player |
| Everything else | Ignored |

Bar buttons: Play/Pause, Favorite, Sleep, Aspect, Audio/Subs, Channels, Guide.

### Context 3 — Channel overlay open

| Key | Action |
|-----|--------|
| ◀ ▶ | Browse channel cards / category chips |
| ▲ | Cards → chips (wall when no categories exist) |
| ▼ | Chips → cards |
| OK on a card | Switch channel (overlay closes, info bar + hints show) |
| OK on a chip | Filter category — focus stays on the chip |
| OK (hold) on a card | Toggle favorite |
| MENU / BACK | Close overlay |
| CH±, digits, INFO | Inactive — the overlay owns the remote |
| (idle 7s) | Auto-close; any key press resets the timer |

Focus is hard-trapped inside the overlay (`focusProperties { exit = Cancel }`) — D-pad can never strand focus on the hidden player behind it.

### Context 4 — Audio/Subs (tracks) panel

▲▼ move between rows, OK selects, BACK closes **and returns focus to the Controls bar** that launched it.

### Context 5 — "Still watching?" sleep prompt

Any key **release** dismisses the prompt and resumes playback (release, not press, so the dismissing key can't leak into other handlers). BACK still backs out.

---

## BACK Precedence Chain

One press pops exactly one level (`repeatCount == 0` guard — holding BACK does NOT multi-pop):

1. Screen locked → flash the lock chip
2. Channel overlay open → close it
3. Tracks panel open → close it, refocus Controls bar
4. Controls bar focused → close the bar
5. Mobile landscape → exit fullscreen
6. Otherwise → exit player

## Hold / Repeat Policy

- All discrete actions gate on `repeatCount == 0` — holding a key fires the action once. No hold-to-surf: one press = one channel.
- 250ms cross-key debounce on channel switches (`CHANNEL_SWITCH_DEBOUNCE_MS`).
- Long-press OK fires its action during the hold at 600ms; the release is then suppressed.
- Card long-press (in the overlay) and the sleep prompt fire on **release** so held keys can't leak into newly opened surfaces.
- ◀▶ traversal while the Controls bar is focused stays repeat-driven (native focus movement).

## Key-Hint Strip

A one-line pill (`PlayerKeyHintStrip.kt`) rides above the full info bar, TV only, and fades in/out with it (channel change / INFO / program boundary). Labels are remap-aware: with default mappings it reads `OK Channels · D-pad Controls`; if the axes are mapped differently it splits into `▲▼ <action> · ◀▶ <action>`. No setting — always on.

## Gesture Parity

Nothing is reachable *only* through a hidden gesture:

| Hidden input | Visible equivalent |
|--------------|--------------------|
| Long-press OK → favorite | Favorite button on the Controls bar |
| 0–9 number entry | Channel overlay list |
| ⏪ / LAST_CH recall | "LAST" pinned card in the overlay |
| CH± zap | Overlay channel cards |
| PLAY_PAUSE media key | Play/Pause button on the Controls bar |
| Mobile gestures | `MobileChromeActions` chrome buttons |

## Channel Overlay (Switch Channel Sheet)

Bottom sheet: focused-channel detail strip → category chips → horizontal channel row. "NOW" badge on the playing channel; recently watched channels pinned first ("LAST"/"RECENT" badges).

- **Auto-category selection:** opens filtered to the playing channel's category; "All" or another chip broadens it.
- **One-shot scroll/focus:** the row scrolls to the current channel and takes focus once per open. EPG refreshes, favorite toggles, and background sync never move focus or scroll mid-browse.
- Slides up with fade (instant when `reduceMotion`).

---

## Auto-Navigation (Dead Stream)

Separate from back-press — when a stream is confirmed dead after all recovery attempts:

```
Stream fails → ErrorRecoveryManager retries (proxy + alternates)
            → All attempts exhausted → onStreamDead
            → 10s countdown with DeadStreamOverlay
            → Auto-navigate back (or user dismisses countdown)
```

## Background Playback (TV vs Phone)

- **Android TV / Fire TV:** Player pauses on `ON_STOP` (Home pressed), resumes on `ON_START`
- **Android Phone:** Player continues in background (no lifecycle pause)
- Detection via `PackageManager.FEATURE_LEANBACK`
