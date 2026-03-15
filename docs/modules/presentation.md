# Presentation Layer

The presentation layer handles UI rendering, user interaction, and screen-level state management. It uses Jetpack Compose for TV with MVVM architecture — ViewModels expose `StateFlow<UiState>` that Compose screens collect and render.

**Package:** `com.cadnative.firevisioniptv.presentation`

## Package Structure

```
presentation/
├── mapper/                  # Domain → UI model mappers
│   ├── CategoryUiMapper.kt
│   └── ChannelUiMapper.kt
├── model/                   # UI state and model classes
│   ├── CategoryUiModel.kt
│   ├── ChannelUiModel.kt
│   ├── ChannelsUiState.kt
│   ├── FavoritesUiState.kt
│   ├── PlayerUiState.kt
│   ├── SearchUiState.kt
│   └── SettingsUiState.kt
├── navigation/
│   ├── FireVisionNavGraph.kt   # Navigation graph setup
│   └── Screen.kt               # Route definitions (sealed class)
├── ui/
│   ├── components/             # Reusable Compose components
│   │   ├── ChannelCard.kt
│   │   ├── ChannelCardSkeleton.kt
│   │   ├── EmptyState.kt
│   │   ├── ErrorState.kt
│   │   ├── LoadingIndicator.kt
│   │   ├── PlayerControls.kt
│   │   └── SideNavRail.kt
│   ├── player/
│   │   ├── ErrorRecoveryManager.kt
│   │   └── PlaybackManager.kt
│   ├── screens/                # Screen composables
│   │   ├── CategoriesScreen.kt
│   │   ├── ChannelsScreen.kt
│   │   ├── FavoritesScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── LegacySettingsScreen.kt
│   │   ├── PairingScreen.kt
│   │   ├── PlayerScreen.kt
│   │   ├── SearchScreen.kt
│   │   └── SettingsScreen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── utils/
│       ├── AnimationUtils.kt
│       └── FocusUtils.kt
└── viewmodel/
    ├── ChannelsViewModel.kt
    ├── FavoritesViewModel.kt
    ├── PairingViewModel.kt
    ├── PlayerViewModel.kt
    ├── SearchViewModel.kt
    └── SettingsViewModel.kt
```

## Navigation

### Screen Routes

Defined in `Screen.kt` as a sealed class:

| Screen | Route | Arguments |
|--------|-------|-----------|
| `Pairing` | `"pairing"` | — |
| `Home` | `"home"` | — |
| `Channels` | `"channels"` | — |
| `Categories` | `"categories"` | — |
| `Search` | `"search"` | — |
| `Favorites` | `"favorites"` | — |
| `Settings` | `"settings"` | — |
| `Player` | `"player/{channelId}"` | `channelId: String` |
| `ChannelsByCategory` | `"channels/category/{categoryId}"` | `categoryId: String` (URL-encoded) |

**Sidebar routes:** Home, Channels, Categories, Search, Favorites, Settings — shown in the persistent `SideNavRail`.

### Navigation Graph

`FireVisionNavGraph.kt` configures `NavHost` with:

- **Start destination:** `Pairing` on first launch (when TV code not configured), otherwise `Home`
- **Sidebar navigation:** Uses `popUpTo(Home)`, `saveState = true`, `launchSingleTop = true`, `restoreState = true` to prevent duplicate destinations and preserve scroll state
- **Player navigation:** Direct navigation with `channelId` argument
- **Pairing success:** Auto-navigates to Home after 1.5s delay, clears Pairing from back stack with `inclusive = true`

## ViewModels

All ViewModels are `@HiltViewModel` with constructor injection.

### ChannelsViewModel

**State:** `ChannelsUiState`

| Field | Type | Default |
|-------|------|---------|
| `channels` | `List<ChannelUiModel>` | `[]` |
| `categories` | `List<String>` | `[]` |
| `isLoading` | `Boolean` | `false` |
| `error` | `String?` | `null` |
| `selectedCategory` | `String?` | `null` |

**Key behavior:**
- `loadChannels(category?)` — Collects channel flow combined with health data from `ChannelHealthDao`
- `toggleFavorite(channelId)` — Optimistic UI update with rollback on error
- `refresh()` — Triggers `RefreshChannelsUseCase`

### FavoritesViewModel

**State:** `FavoritesUiState`

| Field | Type | Default |
|-------|------|---------|
| `favorites` | `List<ChannelUiModel>` | `[]` |
| `isLoading` | `Boolean` | `false` |
| `error` | `String?` | `null` |

**Key behavior:**
- `loadFavorites()` — Called in `init`, combines favorites with health data
- `removeFavorite(channelId)` — Removes with rollback on error
- `moveFavoriteUp(channelId)` / `moveFavoriteDown(channelId)` — Reorder helpers

### SearchViewModel

**State:** `SearchUiState`

| Field | Type | Default |
|-------|------|---------|
| `query` | `String` | `""` |
| `results` | `List<ChannelUiModel>` | `[]` |
| `recentSearches` | `List<String>` | `[]` |
| `activeFilters` | `List<SearchFilter>` | `[]` |
| `isLoading` | `Boolean` | `false` |
| `error` | `String?` | `null` |

**Key behavior:**
- `onQueryChange(query)` — Updates query with 300ms debounce before searching
- `performSearch(query)` — Executes search with active filters, saves to history
- `addFilter(filter)` / `removeFilter(filter)` / `clearFilters()` — Filter management with re-search
- `clearHistory()` — Clears search history

### PlayerViewModel

**State:** `PlayerUiState`

| Field | Type | Default |
|-------|------|---------|
| `channel` | `ChannelUiModel?` | `null` |
| `isPlaying` | `Boolean` | `false` |
| `isBuffering` | `Boolean` | `false` |
| `isLoading` | `Boolean` | `false` |
| `position` | `Long` | `0L` |
| `duration` | `Long` | `0L` |
| `showControls` | `Boolean` | `true` |
| `error` | `String?` | `null` |

**Key behavior:**
- `loadChannel(channelId)` — Loads channel data and restores saved playback position
- `startPeriodicPositionSaving()` — Saves position every 5 seconds during playback
- `onCleared()` — Final position save and cleanup

### SettingsViewModel

**State:** `SettingsUiState`

| Field | Type | Default |
|-------|------|---------|
| `theme` | `String` | `"dark"` |
| `gridSize` | `Int` | `3` |
| `fontSize` | `Float` | `1.0f` |
| `animationSpeed` | `Float` | `1.0f` |
| `layoutDensity` | `String` | `"comfortable"` |
| `serverUrl` | `String` | `""` |
| `tvCode` | `String` | `""` |
| `qrCodeBitmap` | `Bitmap?` | `null` |
| `isPaired` | `Boolean` | `false` |
| `appVersion` | `String` | `"1.0.0"` |

**Key behavior:**
- `saveServerSettings()` — Validates and saves server config, generates QR code via ZXing
- `triggerLivelinessCheck()` — Triggers manual channel health scan via `ChannelHealthScanner`
- `resetToDefaults()` — Resets all preferences to defaults
- `clearCache()` — Deletes app cache directory

### PairingViewModel

**State:** `PairingUiState`

| Field | Type | Default |
|-------|------|---------|
| `pin` | `String` | `""` (6-digit PIN) |
| `statusMessage` | `String` | `""` |
| `countdownText` | `String` | `""` |
| `isLoading` | `Boolean` | `false` |
| `qrCodeBitmap` | `Bitmap?` | `null` |
| `isPaired` | `Boolean` | `false` |

**Key behavior:**
- `requestNewPairing()` — POST to `/api/v1/tv/pairing/request` with device info, receives 6-digit PIN
- `startPolling(pin)` — Polls `/api/v1/tv/pairing/status/{pin}` every 3 seconds (max ~10 minutes)
- `onPairingSuccess(channelListCode, username)` — Saves TV code to SharedPreferences
- `useDefaultChannelList()` — Uses default code `"5T6FEP"` for quick setup

## UI Models

### ChannelUiModel

```kotlin
data class ChannelUiModel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val streamUrl: String?,
    val category: String,
    val isFavorite: Boolean,
    val healthStatus: ChannelHealthStatus = UNKNOWN,
    val thumbnailPath: String?
)
```

### CategoryUiModel

```kotlin
data class CategoryUiModel(
    val id: String,
    val name: String,
    val channelCount: Int
)
```

## UI Mappers

### ChannelUiMapper (`@Singleton`)

| Method | Description |
|--------|-------------|
| `toUiModel(channel, healthStatus?, thumbnailPath?)` | Single domain Channel → ChannelUiModel |
| `toUiModelsWithHealth(channels, healthEntities)` | Batch conversion. Matches health entities by `channelId`, defaults to `UNKNOWN`. |
| `fromUiModel(uiModel)` | Reverse: ChannelUiModel → Channel |

### CategoryUiMapper (`@Singleton`)

| Method | Description |
|--------|-------------|
| `toUiModel(category)` | Category → CategoryUiModel |
| `fromUiModel(uiModel)` | CategoryUiModel → Category |

## Screens

| Screen | ViewModel | Description |
|--------|-----------|-------------|
| `HomeScreen` | `ChannelsViewModel` | Featured channels grid, category quick-links, navigation to all sections |
| `ChannelsScreen` | `ChannelsViewModel` | Full channel list with category filter tabs |
| `CategoriesScreen` | `ChannelsViewModel` | Category grid with channel counts |
| `SearchScreen` | `SearchViewModel` | Search input with debounce, filter chips, recent searches, results grid |
| `FavoritesScreen` | `FavoritesViewModel` | Ordered favorites list with reorder and remove actions |
| `PlayerScreen` | `PlayerViewModel` | Full-screen ExoPlayer with overlay controls, position resume |
| `SettingsScreen` | `SettingsViewModel` | Preferences, server config, QR code, cache management |
| `PairingScreen` | `PairingViewModel` | PIN display, QR code, countdown timer, polling status |
| `LegacySettingsScreen` | — | Kept for backward compatibility |

## Reusable Components

| Component | Description |
|-----------|-------------|
| `ChannelCard` | TV-focused card with logo, name, health indicator, favorite icon, and optional thumbnail |
| `ChannelCardSkeleton` | Shimmer loading placeholder for channel cards |
| `SideNavRail` | Persistent vertical navigation rail for sidebar routes |
| `PlayerControls` | Overlay controls for the player (play/pause, seek, channel info) |
| `EmptyState` | Centered message for empty lists |
| `ErrorState` | Error message with retry action |
| `LoadingIndicator` | Centered loading spinner |

## Player Infrastructure

### PlaybackManager

`ui/player/PlaybackManager.kt`

Manages ExoPlayer lifecycle, media source creation, and playback state.

### ErrorRecoveryManager

`ui/player/ErrorRecoveryManager.kt`

Handles playback errors with retry logic and user-facing error messages.

## Theme

- `Color.kt` — Color definitions for dark TV theme
- `Theme.kt` — `FireVisionTheme` composable wrapping Material3 dark color scheme
- `Type.kt` — Typography definitions optimized for TV viewing distance

## How to Extend

### Adding a new screen

1. Add a route entry to the `Screen` sealed class in `navigation/Screen.kt`
2. Create the screen composable in `ui/screens/`
3. Create a ViewModel in `viewmodel/` if needed (annotate with `@HiltViewModel`)
4. Add the composable destination in `FireVisionNavGraph.kt`
5. If it's a sidebar route, add it to `Screen.sidebarRoutes` and `SideNavRail`

### Adding a new reusable component

1. Create the composable in `ui/components/`
2. Accept data via parameters, emit events via lambda callbacks
3. Use `Modifier` as the first optional parameter for customization
