# ADR-002: Pre-Warm ViewModel During Splash via Box Overlay Pattern

## Status

Accepted

## Date

2026-04-04

## Context

The app displayed a splash screen for ~1900ms using a `Crossfade` composable that toggled between `SplashScreen` and `FireVisionAppShell`. This meant the app shell (NavHost → HomeScreen → `hiltViewModel()`) was not composed until the splash finished. ViewModel `init{}` — which triggers Room queries and API refresh — didn't fire until T=1900ms. Users saw a loading spinner for an additional 400ms+ after splash, totaling ~2300ms before channels appeared.

The primary goal: show channels to users as fast as possible on a Fire TV device.

## Decision

Replace `Crossfade(showSplash)` in `ComposeMainActivity` with a **Box overlay pattern**:

1. **Always compose `FireVisionAppShell`** underneath — this creates the NavHost, HomeScreen, and triggers ViewModel `init{}` at T=0.
2. **Overlay `SplashScreen` on top** — it uses an opaque `Void950` background that covers the app shell. SplashScreen has its own built-in alpha fade-out animation.
3. When splash finishes (~1900ms), it sets `showSplash = false` and the already-populated HomeScreen is revealed.

Supporting changes made alongside this decision:
- **Health flow seeding**: `channelHealthDao.getAllHealth().debounce(500ms).onStart { emit(emptyList()) }` — `onStart` placed AFTER `debounce` to seed `combine` immediately. Placing it before `debounce` causes the seed to be swallowed when real data arrives within the debounce window.
- **Non-blocking EPG enrichment**: Added `EpgRepository.getNowNextIfCached()` (synchronous, returns null if not loaded) to replace the suspend `enrichWithEpg()` that could trigger network calls and block all channels from rendering.
- **`isLoading` defaults to `true`**: Prevents a brief flash of "empty" state between ViewModel creation and first data emission.
- **Resume refresh**: `DisposableEffect` + `LifecycleEventObserver` on HomeScreen calls `ChannelsViewModel.onResume()` to detect server-side changes after backgrounding.

### Resulting Timeline

```
T=0ms      setContent → Box overlay → FireVisionAppShell composed → ViewModel.init{} fires
T=~50ms    Room returns cached channels, combine fires (health seeded empty)
T=~100ms   Channels in UI state (no EPG, no health — just names/logos)
T=~500ms   Health data arrives (debounced), health dots update
T=~1000ms  EPG ensureLoaded() completes, "Now Playing" titles appear
T=~1900ms  Splash fades out → reveals already-populated HomeScreen
           TIME TO CONTENT AFTER SPLASH: 0ms (pre-loaded)
```

## Alternatives Considered

### Keep Crossfade, start ViewModel earlier via manual injection
Create ViewModel in Activity.onCreate() before setContent and pass it down. Rejected: breaks Hilt's `hiltViewModel()` scoping, adds lifecycle management complexity, and doesn't solve the Compose composition dependency.

### Reduce splash duration
Could cut splash from 1900ms to 500ms. Rejected: doesn't solve the fundamental problem (ViewModel still only starts when shell is composed). Also, brand guidelines prefer a visible splash for the logo animation.

### Use a shared ViewModel at Activity scope
Scope ChannelsViewModel to the Activity instead of the NavHost. Rejected: would require `viewModelStoreOwner` overrides, breaks the current per-screen ViewModel pattern, and adds coupling between Activity and screen-level concerns.

## Consequences

**Positive:**
- Channels pre-loaded during splash — zero perceived loading time after splash fades
- No changes to ViewModel lifecycle or scoping — `hiltViewModel()` works as before
- SplashScreen's existing fade-out animation serves as the transition
- EPG and health data populate progressively without blocking initial render

**Negative:**
- App shell and HomeScreen compose "invisibly" behind splash, consuming resources during splash animation (negligible on Fire TV hardware)
- `onStart` placement after `debounce` is a non-obvious pattern that requires a code comment to prevent future regressions
