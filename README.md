# FireVisionIPTV

An IPTV application for Amazon Fire TV and Android TV devices. Watch live TV channels from around the world with server-synced channel lists, category browsing, favorites, and background health scanning.

## Preview

<img src="/preview/preview.gif" alt="Preview GIF" width="800px">

<img src="/preview/preview1.jpg" alt="Preview 1" width="200" height="150"> <img src="/preview/preview2.jpg" alt="Preview 2" width="200" height="150"> <img src="/preview/preview3.jpg" alt="Preview 3" width="200" height="150"> <img src="/preview/preview4.jpg" alt="Preview 4" width="200" height="150">

## Features

- Live TV streaming with HLS support via Media3 ExoPlayer
- Server-synced channel lists with offline-first caching
- Channel browsing by category and language
- Favorites with reordering and server sync
- Full-text channel search with filters and search history
- Background channel health scanning with online/offline status
- Automatic thumbnail extraction from live streams
- Playback position saving and resume
- PIN-based device pairing with QR code
- In-app updates with APK download and install
- Fire TV TIF (TV Input Framework) integration
- Periodic background channel sync via WorkManager
- D-pad optimized navigation with Jetpack Compose for TV

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose for TV (`tv-foundation`, `tv-material`) |
| Architecture | Clean Architecture (data / domain / presentation) + MVVM |
| DI | Hilt 2.51 |
| Database | Room 2.6 (6 entities, 6 DAOs) |
| Networking | Retrofit 2.11 + OkHttp 4.12 |
| Player | Media3 ExoPlayer 1.4 (HLS) |
| Image Loading | Coil 2.7 |
| Navigation | Jetpack Navigation Compose 2.7 |
| Background Work | WorkManager 2.9 |
| Security | AndroidX Security Crypto (EncryptedSharedPreferences) |
| Analytics | Firebase Analytics + Realtime Database |
| Build | Gradle 8.7 with Version Catalog |
| Min SDK | 28 (Android 9.0) |
| Target SDK | 34 (Android 14) |

## Architecture

The app follows **Clean Architecture** with three distinct layers. Dependencies flow inward — presentation depends on domain, domain is pure Kotlin, and data implements domain interfaces.

```text
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  Compose Screens ← ViewModels ← UI Mappers      │
│  (HomeScreen, PlayerScreen, SearchScreen, ...)   │
├─────────────────────────────────────────────────┤
│                Domain Layer                      │
│  Use Cases → Repository Interfaces → Models      │
│  (GetChannels, SearchChannels, ToggleFavorite)   │
├─────────────────────────────────────────────────┤
│                 Data Layer                        │
│  Repository Impls → Data Sources → DAOs / API    │
│  (Room DB, Retrofit API, SharedPreferences)      │
└─────────────────────────────────────────────────┘
```

Data flows reactively via Kotlin `Flow`. ViewModels collect use case flows and expose `StateFlow<UiState>` to Compose screens.

## Project Structure

```
app/src/main/java/com/cadnative/firevisioniptv/
├── api/                        # Legacy API client (HTTP-based)
├── data/
│   ├── mapper/                 # DTO ↔ Entity ↔ Domain mappers
│   ├── model/
│   │   ├── dto/                # API response DTOs (Gson)
│   │   └── Result.kt          # Result<T> sealed class wrapper
│   ├── repository/             # Repository implementations
│   └── source/
│       ├── local/
│       │   ├── dao/            # Room DAOs (6 DAOs)
│       │   ├── entity/         # Room entities (6 tables)
│       │   └── *DataSource.kt  # Local data source wrappers
│       └── remote/
│           ├── FireVisionApiService.kt  # Retrofit API interface
│           └── *DataSource.kt           # Remote data source wrappers
├── di/                         # Hilt DI modules
│   ├── AppModule.kt            # Context, dispatchers
│   ├── DatabaseModule.kt       # Room DB + DAOs
│   ├── NetworkModule.kt        # OkHttp, Retrofit, API service
│   ├── RepositoryModule.kt     # Repository bindings
│   └── ImageLoadingModule.kt   # Coil ImageLoader
├── domain/
│   ├── model/                  # Domain models (Channel, Category, etc.)
│   ├── repository/             # Repository interfaces (7 interfaces)
│   ├── service/                # ChannelHealthScanner, ThumbnailExtractor
│   └── usecase/                # Use cases (12 concrete + 2 base classes)
├── presentation/
│   ├── mapper/                 # Domain → UI model mappers
│   ├── model/                  # UI state classes (UiState, UiModel)
│   ├── navigation/             # Navigation graph + Screen routes
│   ├── ui/
│   │   ├── components/         # Reusable Compose components
│   │   ├── player/             # PlaybackManager, ErrorRecoveryManager
│   │   ├── screens/            # Screen composables (8 screens)
│   │   ├── theme/              # Color, Theme, Typography
│   │   └── utils/              # Animation & focus utilities
│   └── viewmodel/              # ViewModels (6 ViewModels)
├── security/                   # EncryptedSharedPreferences
├── update/                     # In-app update manager
├── worker/                     # WorkManager channel sync
├── ComposeMainActivity.kt      # Main entry point
├── FireVisionApplication.kt    # Hilt application class
├── FireVisionTvInputService.kt # TIF integration service
└── PairingActivity.kt         # Device pairing
```

## Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17**
- **Android SDK 34**
- **Fire TV device** or Android TV emulator (API 28+)

### Build & Run

```bash
# Clone
git clone https://github.com/akshaynikhare/FireVisionIPTV.git
cd FireVisionIPTV

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Configuration

The API base URL is configured in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://tv.cadnative.com/\"")
```

Firebase requires a `google-services.json` file in the `app/` directory.

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](docs/ARCHITECTURE.md) | Detailed architecture overview with data flow diagrams |
| [Data Layer](docs/modules/data.md) | Room database, DAOs, repositories, API service |
| [Domain Layer](docs/modules/domain.md) | Use cases, repository interfaces, domain models |
| [Presentation Layer](docs/modules/presentation.md) | ViewModels, screens, navigation, UI components |
| [API Reference](docs/API.md) | REST endpoints, database schema, repository contracts |
| [Setup Guide](docs/SETUP.md) | Developer environment setup and deployment |

## License

This project is licensed under the [MIT License](LICENSE).

## Disclaimer

This application provides access to free IPTV streams, which may or may not be legal in your country. It is your responsibility to ensure that you comply with all applicable laws and regulations.

## Credits

- Original app by [akshaynikhare](https://github.com/akshaynikhare)
- Built with Jetpack Compose for TV, Media3 ExoPlayer, Room, Hilt, and Retrofit
