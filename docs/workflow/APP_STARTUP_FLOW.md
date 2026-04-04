# App Startup & Channel Loading Flow

## Overview

On app start, the priority is **show channels fast**. The app shell and ViewModel are composed **behind the splash screen**, so data loading starts at T=0. Cached data is ready before splash finishes. Server refresh, EPG, and health scanning are deferred and non-blocking.

---

## App Startup Flow

```mermaid
flowchart TD
    A[App Launch] --> B[FireVisionApplication.onCreate]
    B --> B1[Hilt DI init]
    B --> B2[Firebase Crashlytics init]
    B --> B3[WorkManager: schedule 6hr channel sync]

    B --> C[ComposeMainActivity.onCreate]
    C --> C1[FirebaseApp.initializeApp]
    C --> C2[Check isTvCodeConfigured + isFirstLaunch]

    C --> SET["setContent — Box overlay pattern"]

    SET --> SP["SplashScreen overlay (1.5s + 400ms fade)"]
    SET --> SHELL["FireVisionAppShell composed BEHIND splash"]

    SHELL --> RT{isTvCodeConfigured?}

    RT -- "No: first start / not paired" --> PA[PairingScreen composed behind splash]

    PA --> PD{User choice}
    PD -- "Enter PIN" --> PA1[User enters PIN on dashboard]
    PD -- "Scan QR" --> PA2[User scans QR with phone]
    PD -- "Skip — Use Default Channels" --> PA3[Store default TV code 5T6FEP]

    PA1 --> PA4[Custom TV code saved to prefs]
    PA2 --> PA4
    PA3 --> PA4
    PA4 --> PA5["isPaired = true → 1.5s delay → Navigate to HomeScreen"]

    RT -- "Yes: already paired" --> HS["HomeScreen composed at T=0 (behind splash)"]
    PA5 --> HS

    HS --> J["ChannelsViewModel.init — fires DURING splash"]

    J --> K["1. loadChannels — observe Room DB"]
    K --> O{Room has cached channels?}
    O -- Yes --> P["Channels in state by ~T=50ms (splash still showing)"]
    O -- No --> Q[Show loading spinner when splash fades]

    J --> M["2. refresh — silent background API call"]
    M --> MG{refreshJob active?}
    MG -- Yes --> MS[Skip — already refreshing]
    MG -- No --> R[refreshChannelsUseCase — API call]

    R --> S{API Success?}
    S -- Yes --> T[Room updated → Flow emits → UI updates silently]
    S -- No, has cache --> U[Swallow error silently — cached data stays]
    S -- No, no cache --> V[Show error + Try Again button]

    J --> EPG["3. ensureLoaded — EPG loads in background"]
    EPG --> EPG1["Re-enrich channels with 'Now Playing' when ready"]
    J --> L["4. loadHomeData — recently watched, popular categories"]
    J --> N["5. observeFavoriteCategories"]

    SP --> SP1["Splash fades at ~T=1900ms"]
    SP1 --> REVEAL["Reveal already-populated HomeScreen"]

    style SP fill:#e2e3f1,stroke:#4a4e69
    style PA fill:#fff3cd,stroke:#856404
    style PA3 fill:#d1ecf1,stroke:#0c5460
    style P fill:#d4edda,stroke:#155724
    style REVEAL fill:#d4edda,stroke:#155724
    style HS fill:#d1ecf1,stroke:#0c5460
```

## Channel Loading — Detail

```mermaid
sequenceDiagram
    participant Splash as SplashScreen
    participant UI as HomeScreen
    participant VM as ChannelsViewModel
    participant UC as GetChannelsUseCase
    participant Room as Room DB
    participant EPG as EpgRepository
    participant API as RefreshChannelsUseCase
    participant Server as FireVision Server

    Note over Splash,UI: Both composed at T=0 (Box overlay)
    Note over VM: init {} fires during splash — all 5 tasks in parallel

    VM->>UC: 1. getChannelsUseCase(Unit)
    UC->>Room: observe channels Flow
    Note over Room: Health flow seeded with empty list (onStart)
    Room-->>VM: emit cached channels (~T=50ms)
    Note over VM: enrichWithEpgIfReady — skips if EPG not loaded yet
    VM-->>UI: channels in StateFlow (splash still covering UI)

    VM->>API: 2. refreshChannelsUseCase(Unit)
    VM->>EPG: 3. ensureLoaded() — loads EPG guide from server

    API->>Server: GET /api/v1/channels
    Server-->>API: channel list JSON
    API->>Room: upsert channels
    Room-->>VM: Flow re-emits with fresh data

    EPG-->>VM: EPG loaded → re-enrich all channels with "Now Playing"

    Note over Splash: Splash fades out at ~T=1900ms
    Splash-->>UI: Reveal already-populated HomeScreen

    Note over UI: Time to content after splash: 0ms (pre-loaded)
```

## Health Scanner Lifecycle

```mermaid
flowchart TD
    A[App Start] --> B[startAutoScan called]
    B --> C[Wait 1 minute — let channels load first]
    C --> D[Phase 1: runFullScan]
    D --> E[Batch channels in groups of 4]
    E --> F[Check each stream URL — HLS manifest or HEAD request]
    F --> G[Save results to ChannelHealthDao]
    G --> H[Sync results to server]
    H --> I[Wait 5 minutes]
    I --> J[Phase 2: Extract thumbnails for ONLINE channels]
    J --> K[Wait 30 minutes]
    K --> D

    style C fill:#fff3cd,stroke:#856404
    style D fill:#d4edda,stroke:#155724
    style J fill:#d1ecf1,stroke:#0c5460
```

## State Machine — HomeScreen Content

```mermaid
stateDiagram-v2
    [*] --> Loading : channels empty + not initialized
    Loading --> Content : channels arrive from Room
    Loading --> Error : refresh fails + no cache
    Loading --> Empty : refresh done + Room empty

    Content --> Content : silent refresh updates
    Content --> Content : health status updates (debounced 500ms)

    Error --> Loading : user taps "Try Again"
    Empty --> Loading : user taps "Try Again"

    state Content {
        [*] --> ShowingChannels
        ShowingChannels --> ShowingChannels : background refresh
    }
```

```
ContentState logic (HomeScreen):
  isLoading && channels.isEmpty()           → "loading"
  error != null && channels.isEmpty()       → "error"
  channels.isEmpty() && !isInitialLoadComplete → "loading"
  channels.isEmpty()                        → "empty"
  else                                      → "content"
```

## Clear Cache Flow (Settings)

```mermaid
sequenceDiagram
    participant User
    participant Settings as SettingsScreen
    participant VM as SettingsViewModel
    participant Repo as UserPreferencesRepository
    participant Room as Room DB
    participant API as RefreshChannelsUseCase
    participant Server as FireVision Server

    User->>Settings: Tap "Clear Local Cache"
    Settings->>VM: clearCache()
    VM->>Repo: clearCache()
    Repo->>Room: delete channels, health, metrics
    Note over Repo: Preserves: favorites, playback positions, search history
    Repo-->>VM: Success
    VM-->>Settings: cacheCleared = true
    VM->>API: refreshChannelsUseCase(Unit)
    API->>Server: GET /api/v1/channels
    Server-->>API: fresh channel list
    API->>Room: upsert channels
    Note over Settings: "Cache cleared" message auto-dismisses after 3s
```

## Resume / Background Detection

```mermaid
flowchart TD
    A[App goes to background] --> B[ON_STOP]
    B --> C[App returns to foreground]
    C --> D[ON_RESUME]
    D --> E{hasResumedBefore?}
    E -- "No (first resume)" --> G["Skip — init{} already handles it"]
    E -- "Yes (returning)" --> F["viewModel.onResume() → refresh()"]
    F --> H[Silent background refresh]
    H --> I[Room updated → UI reacts via Flow]

    style G fill:#fff3cd,stroke:#856404
    style F fill:#d4edda,stroke:#155724
```

Implemented via `DisposableEffect` + `LifecycleEventObserver` in HomeScreen, with `hasResumedBefore` guard in ChannelsViewModel.

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| App shell composed behind splash | ViewModel init{} fires at T=0 — channels pre-loaded before splash fades |
| EPG enrichment non-blocking | `getNowNextIfCached()` skips if EPG not loaded — channels render instantly, "Now Playing" titles appear later |
| Health flow seeded with empty list | `onStart { emit(emptyList()) }` after debounce lets `combine` fire immediately — no 500ms wait |
| Show cached channels instantly | Users shouldn't wait for network on every launch |
| Health scan delayed 1 min | Let channel list load and render first — health is secondary |
| Silent refresh when cache exists | No loading flash, no error toast — just update in background |
| OFFLINE channels stay visible | Health dot indicator communicates status; hiding causes flicker during scan |
| 500ms debounce on health flow | Prevents rapid UI recomposition during batch scanning |
| refreshJob guard | Prevents duplicate concurrent refresh calls |
| Resume triggers silent refresh | Detects server-side changes when returning from background |
