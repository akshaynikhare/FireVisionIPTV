# Architecture

FireVisionIPTV follows **Clean Architecture** with three layers: **Presentation**, **Domain**, and **Data**. The architecture enforces unidirectional dependency flow — outer layers depend on inner layers, never the reverse.

## Overview

```text
┌───────────────────────────────────────────────────────────────┐
│                     Presentation Layer                         │
│                                                               │
│  ┌─────────┐    ┌────────────┐    ┌──────────┐    ┌────────┐ │
│  │ Screens  │◄───│ ViewModels │◄───│ UiMapper │◄───│UiState │ │
│  │(Compose) │    │  (Hilt)    │    │          │    │UiModel │ │
│  └─────────┘    └─────┬──────┘    └──────────┘    └────────┘ │
│                       │ invokes                               │
├───────────────────────┼───────────────────────────────────────┤
│                  Domain Layer                                  │
│                       │                                        │
│  ┌──────────────┐    ┌▼──────────┐    ┌────────────────────┐  │
│  │  Repository   │◄───│ Use Cases │    │   Domain Models    │  │
│  │  Interfaces   │    │           │    │ (Channel, Category │  │
│  └──────┬───────┘    └───────────┘    │  PlaybackState...) │  │
│         │                              └────────────────────┘  │
├─────────┼─────────────────────────────────────────────────────┤
│         │            Data Layer                                │
│         ▼                                                      │
│  ┌──────────────┐    ┌────────────────┐    ┌───────────────┐  │
│  │  Repository   │───►│ Local Sources   │───►│  Room DAOs    │  │
│  │  Impls        │    │                │    │  (SQLite)     │  │
│  │               │───►│ Remote Sources  │───►│  Retrofit API │  │
│  └──────────────┘    └────────────────┘    └───────────────┘  │
│                                                                │
│  ┌──────────┐    ┌──────────┐    ┌───────────────────────┐    │
│  │ Entities │    │   DTOs   │    │  Mappers (DTO↔Entity  │    │
│  │ (Room)   │    │  (Gson)  │    │   ↔ Domain)           │    │
│  └──────────┘    └──────────┘    └───────────────────────┘    │
└───────────────────────────────────────────────────────────────┘
```

## Layer Details

### Presentation Layer

**Responsibility:** Renders UI, handles user interaction, manages screen-level state.

**Key components:**

| Component | Role |
|-----------|------|
| `ComposeMainActivity` | Entry point. Initializes Firebase, checks pairing, starts health scanner. Uses **Box overlay pattern**: app shell composes behind an opaque splash screen so ViewModels init during splash, not after. Orientation-adaptive: landscape uses side rail, portrait uses bottom nav bar. |
| `FireVisionNavGraph` | Defines all navigation routes using Jetpack Navigation Compose. 9 destinations including Player with `channelId` argument. |
| `Screen` (sealed class) | Type-safe route definitions: Home, Channels, Categories, Search, Favorites, Settings, Player, Pairing, ChannelsByCategory. |
| `SideNavRail` | Persistent sidebar for TV navigation across main routes. |
| ViewModels (6) | `ChannelsViewModel`, `FavoritesViewModel`, `SearchViewModel`, `PlayerViewModel`, `SettingsViewModel`, `PairingViewModel` — all `@HiltViewModel`. |
| UI Mappers | `ChannelUiMapper`, `CategoryUiMapper` — convert domain models to UI models, enriching with health status and thumbnails. |
| UI State classes | Immutable data classes (`ChannelsUiState`, `PlayerUiState`, etc.) exposed as `StateFlow` from ViewModels. |

**Dependencies:** Domain layer (use cases, models).

### Domain Layer

**Responsibility:** Contains business logic in use cases, defines repository contracts, and holds domain models. This layer is pure Kotlin with no Android framework dependencies (except Hilt annotations).

**Key components:**

| Component | Role |
|-----------|------|
| Domain Models (6) | `Channel`, `Category`, `ChannelHealthStatus`, `PlaybackState`, `SearchFilter`, `EpgProgram` |
| Repository Interfaces (8) | `ChannelRepository`, `CategoryRepository`, `FavoriteRepository`, `PlaybackRepository`, `PlaylistRepository`, `SearchHistoryRepository`, `UserPreferencesRepository`, `EpgRepository` |
| Use Cases (16) | Single-responsibility classes for each operation. Two base classes: `UseCase<P, R>` (suspend, one-shot) and `FlowUseCase<P, R>` (reactive streams). The 4 additions over the original 12 are: `PullFavoritesUseCase`, `ReportStreamStatusUseCase`, `ReportStreamPlayUseCase`, `SyncHealthResultsUseCase`. |
| Services (2) | `ChannelHealthScanner` (batch HTTP health checks with server sync after each cycle), `ChannelThumbnailExtractor` (MediaMetadataRetriever frame extraction). |

**Dependencies:** None (pure Kotlin + coroutines).

### Data Layer

**Responsibility:** Implements repository interfaces, manages local persistence (Room), remote API calls (Retrofit), and data mapping between layers.

**Key components:**

| Component | Role |
|-----------|------|
| Room Database | `FireVisionDatabase` (v3) with 6 entities: `ChannelEntity`, `CategoryEntity`, `FavoriteEntity`, `SearchHistoryEntity`, `PlaybackPositionEntity`, `ChannelHealthEntity` |
| DAOs (6) | Type-safe SQL queries with `Flow` return types for reactive updates |
| Local Data Sources (5) | Thin wrappers around DAOs that run operations on `IoDispatcher` |
| Remote Data Sources (2) | `ChannelRemoteDataSource`, `CategoryRemoteDataSource` — wrap Retrofit calls with error mapping |
| Repository Impls (7) | Implement domain interfaces. Offline-first: local DB is source of truth, remote data refreshes the cache. `EpgRepositoryImpl` uses in-memory cache with lazy network load. |
| Mappers (2) | `ChannelMapper`, `CategoryMapper` — bidirectional DTO ↔ Entity ↔ Domain conversions |
| `FireVisionApiService` | Retrofit interface with 9 endpoints: channels, categories, favorites (GET + POST), report-status, report-play, health-sync, app version, device pairing |
| `Result<T>` | Sealed class (`Success<T>` / `Error`) for type-safe error handling |

**Dependencies:** Domain layer (repository interfaces, models).

## Dependency Injection

Hilt provides all DI with 5 modules installed in `SingletonComponent`:

```text
┌─────────────────────────────────────────────────────────┐
│                    Hilt Modules                          │
├─────────────────────────────────────────────────────────┤
│ AppModule          → Context, @IoDispatcher,            │
│                      @MainDispatcher                     │
│ NetworkModule      → OkHttpClient, Retrofit,            │
│                      FireVisionApiService                │
│ DatabaseModule     → FireVisionDatabase, all 6 DAOs     │
│ RepositoryModule   → Binds 6 repo interfaces to impls   │
│ ImageLoadingModule → Coil ImageLoader (25% mem cache,   │
│                      50MB disk cache, 300ms crossfade)   │
└─────────────────────────────────────────────────────────┘
```

Custom qualifiers `@IoDispatcher` and `@MainDispatcher` differentiate coroutine dispatchers.

## Data Flow

### Typical read flow: User opens Channels screen

```text
ChannelsScreen
    │ collects StateFlow
    ▼
ChannelsViewModel
    │ invokes
    ▼
GetChannelsUseCase (FlowUseCase)
    │ calls
    ▼
ChannelRepository.getChannels() : Flow<Result<List<Channel>>>
    │ implemented by
    ▼
ChannelRepositoryImpl
    │ combines two flows:
    ├──► ChannelLocalDataSource.getAllChannels() → ChannelDao (Room, returns Flow)
    └──► FavoriteLocalDataSource.getAllFavorites() → FavoriteDao (Room, returns Flow)
    │ maps entities to domain models via ChannelMapper
    ▼
Flow<Result<List<Channel>>>
    │ mapped by ChannelUiMapper (enriched with ChannelHealthDao data)
    ▼
StateFlow<ChannelsUiState> → Compose recomposes
```

### Typical write flow: User toggles favorite

```text
ChannelsScreen (user clicks favorite icon)
    │
    ▼
ChannelsViewModel.toggleFavorite(channelId)
    │ optimistic UI update
    │ invokes
    ▼
ToggleFavoriteUseCase(channelId)
    │ calls
    ▼
FavoriteRepository.toggleFavorite(channelId)
    │ implemented by
    ▼
FavoriteRepositoryImpl
    ├──► Checks isFavorite via FavoriteDao
    ├──► Adds/removes FavoriteEntity via FavoriteDao
    └──► Fire-and-forget: syncFavorites() → POST /api/v1/favorites
    │
    ▼
Room emits updated Flow → ChannelRepositoryImpl recombines → ViewModel updates UI
```

### Refresh flow: Background channel sync

```text
WorkManager (every 6 hours)
    │
    ▼
ChannelSyncWorker.doWork()
    │ calls
    ▼
ChannelRepository.refreshChannels()
    │ implemented by
    ▼
ChannelRepositoryImpl
    ├──► ChannelRemoteDataSource.fetchChannels() → GET /api/v1/channels
    ├──► Maps ChannelDto → ChannelEntity via ChannelMapper
    └──► ChannelLocalDataSource.replaceAllChannels() → @Transaction: delete all + insert
    │
    ▼
Room emits updated Flow → any active ViewModel/screen recomposes
```

## Navigation

Navigation uses Jetpack Navigation Compose with a `NavHostController`. The `ComposeMainActivity` sets up a shell layout:

```text
┌──────────────────────────────────────────┐
│ ┌──────┐ ┌────────────────────────────┐  │
│ │ Side │ │                            │  │
│ │ Nav  │ │       NavHost              │  │
│ │ Rail │ │   (current screen)         │  │
│ │      │ │                            │  │
│ │ Home │ │                            │  │
│ │ Chan │ │                            │  │
│ │ Cat  │ │                            │  │
│ │ Srch │ │                            │  │
│ │ Fav  │ │                            │  │
│ │ Set  │ │                            │  │
│ └──────┘ └────────────────────────────┘  │
└──────────────────────────────────────────┘
```

- **Sidebar routes** (Home, Channels, Categories, Search, Favorites, Settings): Navigate with `popUpTo(Home)`, `saveState`, `launchSingleTop`, `restoreState` — prevents duplicate destinations and preserves scroll state.
- **Player**: Full-screen overlay, navigated to with `channelId` argument.
- **Pairing**: Shown on first launch if TV code not configured. On success, clears itself from the back stack.
- **Start destination**: Pairing (first launch) or Home (returning user).

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Offline-first with Room as source of truth** | Fire TV devices may have intermittent connectivity. Local DB ensures the app is always usable. Remote data refreshes the cache. |
| **Flow-based reactivity end-to-end** | Room DAOs return `Flow`, which propagates through repositories and use cases to ViewModels. Any data change (favorite toggled, channels refreshed) automatically updates all observing screens. |
| **Optimistic UI updates** | Toggling favorites updates the UI immediately, then persists in the background. Rolls back on error for a responsive feel on TV remotes. |
| **UseCase / FlowUseCase base classes** | Standardizes one-shot vs. streaming operations. ViewModels invoke use cases as functions via `operator fun invoke()`. |
| **Separate ChannelRepository and FavoriteRepository** | Channels and favorites have different sync strategies. Channels do full-replace refresh; favorites do incremental sync with server. |
| **ChannelHealthScanner as a service, not a use case** | Health scanning is a long-running background process (batched HTTP checks, thumbnail extraction) that doesn't fit the single-responsibility use case pattern. |
| **Hilt over manual DI** | Compile-time DI validation, reduced boilerplate, native WorkManager and ViewModel integration via `@HiltWorker` and `@HiltViewModel`. |
| **Compose for TV over Leanback XML** | Modern declarative UI with better state management. Leanback dependency kept for backward compatibility. |
| **BuildConfig for API URL** | Allows different URLs per build variant (debug, dev, release) without code changes. |
| **Destructive migration in dev** | `fallbackToDestructiveMigration()` simplifies schema iteration during development. Production should use proper migrations. |
| **Fire-and-forget health reporting** | `ReportStreamStatusUseCase` and `ReportStreamPlayUseCase` are launched in `viewModelScope` with `SupervisorJob` — failures are silently swallowed so health reporting never blocks playback or affects user experience. |
| **Unresponsive stream detection in ErrorRecoveryManager** | `bufferWatchJob` fires after 30 s of continuous `STATE_BUFFERING` without producing frames, triggering `onStreamUnresponsive` — distinct from a full stream failure and reported separately to the server. |
| **In-memory alternate stream slots** | Alternates fetched from `/me/channels-with-fallbacks` are stored in a `StreamSlot` queue in `ChannelRepositoryImpl` (not in Room) to avoid database migrations. On cold start the queue is empty and the app degrades gracefully to primary-only retry. |
| **Box overlay pattern for pre-warming** | `ComposeMainActivity` composes the full app shell (NavHost, HomeScreen, ViewModel) behind an opaque splash overlay. ViewModel `init{}` fires at T=0, Room returns cached channels by ~T=50ms. When splash fades at ~T=1900ms, HomeScreen is already populated. Previous `Crossfade` approach delayed ViewModel creation until after splash. |
| **Non-blocking EPG enrichment** | `EpgRepository.getNowNextIfCached()` is a synchronous (non-suspend) function that returns `null` if EPG data hasn't loaded yet. Channels render immediately without EPG; "Now Playing" titles appear asynchronously once `ensureLoaded()` completes. This prevents a network call from blocking the entire channel list. |
| **Health flow seeding with `onStart`** | `channelHealthDao.getAllHealth().debounce(500ms).onStart { emit(emptyList()) }` — the `onStart` AFTER `debounce` seeds `combine` with an empty list so channels render instantly. Real health data updates silently ~500ms later. Placing `onStart` before `debounce` would cause the seed to be swallowed. |
| **Lifecycle-aware resume refresh** | `HomeScreen` uses `DisposableEffect` + `LifecycleEventObserver` to call `ChannelsViewModel.onResume()` on `ON_RESUME`. First resume is skipped (init handles it). Subsequent resumes trigger a silent `refresh()` to detect server-side changes. |
