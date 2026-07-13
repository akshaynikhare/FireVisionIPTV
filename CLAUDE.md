# CLAUDE.md — FireVision IPTV Android TV App

> IPTV streaming app for Amazon Fire TV and Android TV. Kotlin, Jetpack Compose for TV, Clean Architecture.

## Architecture

Clean Architecture with three layers. Dependencies flow inward only.

| Layer | Purpose | Key Files |
|-------|---------|-----------|
| **Presentation** | Compose screens, ViewModels, UI state | `presentation/ui/screens/`, `presentation/viewmodel/` |
| **Domain** | Use cases, repository interfaces, models | `domain/usecase/`, `domain/repository/`, `domain/model/` |
| **Data** | Room DB, Retrofit API, repository impls | `data/repository/`, `data/source/local/`, `data/source/remote/` |

Data flows reactively via Kotlin `Flow`. ViewModels expose `StateFlow<UiState>` to Compose screens.

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Kotlin | 1.9 |
| UI | Jetpack Compose for TV (`tv-foundation`, `tv-material`) | - |
| DI | Hilt | 2.51 |
| Database | Room | 2.6 (6 entities, 6 DAOs) |
| Networking | Retrofit + OkHttp | 2.11 / 4.12 |
| Player | Media3 ExoPlayer (HLS) | 1.4 |
| Images | Coil | 2.7 |
| Navigation | Jetpack Navigation Compose | 2.7 |
| Background | WorkManager | 2.9 |
| Security | EncryptedSharedPreferences | - |
| Analytics | Firebase Analytics + Crashlytics + Perf | BoM 33.1.0 |
| Min SDK | 28 (Android 9) | Target SDK 34 |

## Quick Start

```bash
make debug                               # Build debug APK (preferred)
make release                             # Build release APK (requires signing env vars)
make install                             # Build debug + install on device
make run                                 # Build, install, and launch
make lint                                # Run Android lint checks
make test                                # Run unit tests
make logcat                              # Show app logs (filtered to FireVision)
```

API base URL is hardcoded in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://tv.cadnative.com/\"")
```

Firebase requires `app/google-services.json`.

## Key File Locations

```
app/src/main/java/com/cadnative/firevisioniptv/
├── ComposeMainActivity.kt              ← Entry point
├── FireVisionApplication.kt            ← Hilt application class
├── PairingActivity.kt                  ← Device pairing (PIN + QR)
├── FireVisionTvInputService.kt         ← TIF integration
│
├── di/                                 ← Hilt DI modules
│   ├── AppModule.kt                    ← Context, dispatchers
│   ├── DatabaseModule.kt               ← Room DB + DAOs
│   ├── NetworkModule.kt                ← OkHttp, Retrofit, API service
│   ├── RepositoryModule.kt             ← Repository bindings
│   └── ImageLoadingModule.kt           ← Coil ImageLoader
│
├── data/
│   ├── mapper/                         ← DTO ↔ Entity ↔ Domain mappers
│   ├── model/dto/                      ← API response DTOs (Gson)
│   ├── repository/                     ← Repository implementations (6 repos)
│   └── source/
│       ├── local/dao/                  ← Room DAOs (6 DAOs)
│       ├── local/entity/               ← Room entities (6 tables)
│       └── remote/FireVisionApiService.kt  ← Retrofit API interface
│
├── domain/
│   ├── model/                          ← Domain models (Channel, Category, etc.)
│   ├── repository/                     ← Repository interfaces (7 interfaces)
│   ├── service/                        ← ChannelHealthScanner, ThumbnailExtractor
│   └── usecase/                        ← Use cases (12 concrete + 2 base classes)
│
├── presentation/
│   ├── mapper/                         ← Domain → UI model mappers
│   ├── model/                          ← UI state classes
│   ├── navigation/                     ← Nav graph + Screen routes
│   ├── ui/
│   │   ├── components/                 ← Reusable Compose components
│   │   ├── player/                     ← PlaybackManager, ErrorRecoveryManager
│   │   ├── screens/                    ← Screen composables (8 screens)
│   │   ├── theme/                      ← Color, Theme, Typography
│   │   └── utils/                      ← Animation & focus utilities
│   └── viewmodel/                      ← ViewModels (6 ViewModels)
│
├── security/SecurePreferences.kt       ← EncryptedSharedPreferences
├── update/                             ← In-app update manager
└── worker/                             ← WorkManager channel sync
```

## Navigation (Screens)

```
SplashScreen → PairingScreen (if no device paired)
             → HomeScreen (main entry after pairing)

HomeScreen (SideNavRail)
├── Channels (by category, grid layout)
├── Favorites (reorderable, server-synced)
├── Search (full-text with filters + history)
├── Settings (server URL, health scan, about)
└── Player (full-screen ExoPlayer with HLS)
```

D-pad optimized navigation. Focus management via Compose for TV focus APIs.

## API Integration

Consumes the FireVision IPTV Server backend:
- `GET /api/v1/channels` — Channel list with categories
- `GET /api/v1/categories` — Category list
- `POST /api/v1/favorites` — Sync favorites
- `GET /api/v1/device/pair` — PIN-based device pairing
- `GET /api/v1/app-versions` — Update check

Auth via device pairing code stored in `EncryptedSharedPreferences`.

## Room Database

6 entities:
- `ChannelEntity` — Cached channel data
- `CategoryEntity` — Cached categories
- `FavoriteEntity` — Local + synced favorites
- `ChannelHealthEntity` — Health scan results
- `PlaybackPositionEntity` — Resume positions
- `SearchHistoryEntity` — Search history

Schema exported to `app/schemas/` for migration testing.

## Conventions

### Code organization
- New use cases go in `domain/usecase/` — extend `UseCase` or `FlowUseCase` base classes
- New screens go in `presentation/ui/screens/` with a corresponding ViewModel in `presentation/viewmodel/`
- UI layering: `components/` (stateless, reusable atoms/molecules) → `screens/<name>/` (one package per complex screen; `XxxScreen.kt` is the thin stateful root, content/sections split into `internal` same-package files). No UI file over ~400 lines
- Theme tokens are mandatory: colors in `theme/Color.kt`, type styles in `theme/Type.kt`, shapes in `theme/Shape.kt`, spacing/sizes in `theme/Dimens.kt` — never inline `Color(0x…)`, `fontSize`, `RoundedCornerShape(N.dp)`, or magic dp in screens/components
- Gate animations on `LocalPerfProfile.current.reduceMotion` (low-end Android 9 boxes): skip shimmer/stagger/transitions when true
- New DTOs go in `data/model/dto/`, entities in `data/source/local/entity/`
- Mappers in `data/mapper/` (DTO↔Entity) and `presentation/mapper/` (Domain→UI)

### Dependency injection
- All dependencies provided via Hilt modules in `di/`
- ViewModels use `@HiltViewModel` + `@Inject constructor`
- Use `@IoDispatcher`, `@DefaultDispatcher` qualifiers for coroutine dispatchers

### Data flow
- Screen → ViewModel → UseCase → Repository (interface) → RepositoryImpl → DataSource
- All async work via Kotlin `Flow` and coroutines
- Use `Result<T>` sealed class from `data/model/Result.kt` for API responses

## Workflow Best Practices

### Search before change
Before modifying any code, search the existing codebase for patterns. This is a Clean Architecture project — every layer has established conventions. Find the most similar existing use case, repository, or screen and follow its structure.

### Layer discipline
Never break the dependency rule:
1. **Domain** layer has zero Android dependencies — pure Kotlin only
2. **Data** layer implements domain interfaces — never imported by presentation directly
3. **Presentation** depends on domain only — ViewModels call use cases, not repositories
4. New features always start with the domain model and use case, then data, then presentation

### Incremental development with validation
1. Write use case → write repository impl → test data flow
2. Write ViewModel → wire to screen → test on Fire TV device or emulator
3. Never write 300+ lines without building and running on device
4. Always test D-pad navigation — this is a TV app, no touch input

### D-pad and focus management
- Every interactive element must be focusable and reachable via D-pad
- Use Compose for TV focus APIs (`focusable()`, `onFocusChanged()`)
- Test navigation flow: can you reach every screen and action with D-pad only?
- Back button must always work predictably

### Offline-first data strategy
- Room is the single source of truth for channel data
- API syncs update Room → UI observes Room via Flow
- App must work offline with cached data
- WorkManager handles periodic background sync

### Player reliability
- ExoPlayer errors are handled by `ErrorRecoveryManager`
- HLS streams can fail — always have retry and fallback logic
- Save playback position for resume on next launch
- Test with various stream qualities and broken URLs

## Record Architectural Decisions
When a significant decision is made (new tech choice, pattern change, data model strategy, trade-off picked), ask the user if they want it recorded as an ADR in `docs/decisions/`. Format: Status, Context, Decision, Alternatives Considered, Consequences.

## Session Best Practices

1. Always read this file before making changes
2. Build with `./gradlew assembleDebug` after changes to catch compile errors
3. Test on Fire TV device or Android TV emulator — not a phone emulator
4. All API calls go through `FireVisionApiService` (Retrofit) — never use raw HTTP
5. Use `EncryptedSharedPreferences` for any sensitive data (pairing code, tokens)
6. Never hardcode colors — use theme from `presentation/ui/theme/`
7. Room schema changes require a migration — never change entities without one
8. Test D-pad navigation after any UI changes
