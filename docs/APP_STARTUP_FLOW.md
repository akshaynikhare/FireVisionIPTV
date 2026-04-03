# App Startup & Channel Loading Flow

## Overview

On app start, the priority is **show channels fast**. Cached data appears instantly. Server refresh and health scanning are deferred to avoid blocking the UI.

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

    C --> SP[SplashScreen shown FIRST — always]
    SP --> SP1[onSplashFinished callback]
    SP1 --> RT{isTvCodeConfigured?}

    RT -- "No: first start / not paired" --> PA[PairingScreen]

    PA --> PD{User choice}
    PD -- "Enter PIN" --> PA1[User enters PIN on dashboard]
    PD -- "Scan QR" --> PA2[User scans QR with phone]
    PD -- "Skip — Use Default Channels" --> PA3[Store default TV code 5T6FEP]

    PA1 --> PA4[Custom TV code saved to prefs]
    PA2 --> PA4
    PA3 --> PA4
    PA4 --> PA5["isPaired = true → 1.5s delay → Navigate to HomeScreen"]

    RT -- "Yes: already paired" --> HS[HomeScreen]
    PA5 --> HS

    HS --> J[ChannelsViewModel.init]

    J --> K["1. loadChannels — observe Room DB"]
    K --> O{Room has cached channels?}
    O -- Yes --> P[Show cached channels immediately]
    O -- No --> Q[Show loading spinner]

    J --> M["2. refresh — check if server request needed"]
    M --> MG{refreshJob active?}
    MG -- Yes --> MS[Skip — already refreshing]
    MG -- No --> R[refreshChannelsUseCase — API call]

    R --> S{API Success?}
    S -- Yes --> T[Room updated → Flow emits → UI updates silently]
    S -- No, has cache --> U[Swallow error silently — cached data stays]
    S -- No, no cache --> V[Show error + Try Again button]

    J --> L["3. loadHomeData — recently watched, popular categories"]
    J --> N["4. observeFavoriteCategories"]
    J --> J1["5. Start health scanner after 1 min delay"]

    style SP fill:#e2e3f1,stroke:#4a4e69
    style PA fill:#fff3cd,stroke:#856404
    style PA3 fill:#d1ecf1,stroke:#0c5460
    style P fill:#d4edda,stroke:#155724
    style J1 fill:#d1ecf1,stroke:#0c5460
```

## Channel Loading — Detail

```mermaid
sequenceDiagram
    participant UI as HomeScreen
    participant VM as ChannelsViewModel
    participant UC as GetChannelsUseCase
    participant Room as Room DB
    participant API as RefreshChannelsUseCase
    participant Server as FireVision Server

    Note over VM: init {} runs all 4 in parallel

    VM->>UC: getChannelsUseCase(Unit)
    UC->>Room: observe channels Flow
    Room-->>VM: emit cached channels (if any)
    VM-->>UI: channels → show content (or loading if empty)

    VM->>API: refreshChannelsUseCase(Unit)
    API->>Server: GET /api/v1/channels
    Server-->>API: channel list JSON
    API->>Room: upsert channels
    Room-->>VM: Flow re-emits with fresh data
    VM-->>UI: UI updates silently (no loading flash)
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

## Resume / Background Detection (Planned)

```mermaid
flowchart TD
    A[App goes to background] --> B[ON_STOP]
    B --> C[App returns to foreground]
    C --> D[ON_RESUME]
    D --> E{hasInitialized?}
    E -- Yes --> F[viewModel.onResume → refresh]
    E -- No --> G[Skip — init already handles it]
    F --> H[Silent background refresh]
    H --> I[Room updated → UI reacts via Flow]
```

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Show cached channels instantly | Users shouldn't wait for network on every launch |
| Health scan delayed 1 min | Let channel list load and render first — health is secondary |
| Silent refresh when cache exists | No loading flash, no error toast — just update in background |
| OFFLINE channels stay visible | Health dot indicator communicates status; hiding causes flicker during scan |
| 500ms debounce on health flow | Prevents rapid UI recomposition during batch scanning |
| refreshJob guard | Prevents duplicate concurrent refresh calls |
