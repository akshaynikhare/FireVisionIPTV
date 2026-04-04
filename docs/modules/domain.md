# Domain Layer

Business logic, domain models, repository contracts, and services. Pure Kotlin — no Android framework dependencies.

**Package:** `com.cadnative.firevisioniptv.domain`

## Package Structure

```
domain/
├── model/          # Domain models (pure data classes and enums)
├── repository/     # Repository interfaces (contracts for data layer)
├── service/        # Long-running background services
└── usecase/        # Single-responsibility business operations
```

## Domain Models

### Channel

`domain/model/Channel.kt`

Core business entity representing a TV channel.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique channel identifier |
| `name` | `String` | Display name |
| `streamUrl` | `String` | HLS/DASH streaming URL |
| `logoUrl` | `String?` | Channel logo image URL |
| `category` | `String` | Category name (e.g., "Sports", "News") |
| `language` | `String?` | Language code |
| `country` | `String?` | Country code |
| `isFavorite` | `Boolean` | Whether the user has favorited this channel (default: `false`) |

### Category

`domain/model/Category.kt`

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Category identifier |
| `name` | `String` | Display name |
| `channelCount` | `Int` | Number of channels in this category |

### ChannelHealthStatus

`domain/model/ChannelHealthStatus.kt`

Enum representing channel availability:

| Value | Description |
|-------|-------------|
| `UNKNOWN` | Not yet checked |
| `CHECKING` | Currently being scanned |
| `ONLINE` | Stream is reachable |
| `OFFLINE` | Stream is unreachable |

### PlaybackState

`domain/model/PlaybackState.kt`

| Field | Type | Description |
|-------|------|-------------|
| `channelId` | `String` | Associated channel |
| `position` | `Long` | Current position in milliseconds |
| `duration` | `Long` | Total duration in milliseconds |
| `isPlaying` | `Boolean` | Whether playback is active |

### EpgProgram

`domain/model/EpgProgram.kt`

EPG (Electronic Program Guide) entry for a channel.

| Field | Type | Description |
|-------|------|-------------|
| `channelEpgId` | `String` | TVG ID matching the channel |
| `title` | `String` | Program title (e.g., "News at 9") |
| `description` | `String?` | Program description |
| `startTime` | `Instant` | Program start time |
| `endTime` | `Instant` | Program end time |
| `icon` | `String?` | Program artwork URL |

### SearchFilter

`domain/model/SearchFilter.kt`

Sealed class for composable search filters:

| Subclass | Field | Description |
|----------|-------|-------------|
| `ByCategory` | `category: String` | Filter by category name |
| `ByLanguage` | `language: String` | Filter by language code |
| `ByCountry` | `country: String` | Filter by country code |
| `Combined` | `filters: List<SearchFilter>` | AND-combine multiple filters |

## Repository Interfaces

All repository interfaces define the contract that the data layer must implement. They use `Flow<Result<T>>` for reactive reads and `suspend fun` returning `Result<T>` for writes.

### ChannelRepository

`domain/repository/ChannelRepository.kt`

```kotlin
fun getChannels(): Flow<Result<List<Channel>>>
fun getChannelById(id: String): Flow<Result<Channel>>
fun getChannelsByCategory(category: String): Flow<Result<List<Channel>>>
fun searchChannels(query: String): Flow<Result<List<Channel>>>
suspend fun refreshChannels(): Result<Unit>
suspend fun addToFavorites(channelId: String): Result<Unit>
suspend fun removeFromFavorites(channelId: String): Result<Unit>
fun getFavoriteChannels(): Flow<Result<List<Channel>>>
```

### CategoryRepository

`domain/repository/CategoryRepository.kt`

```kotlin
fun getCategories(): Flow<Result<List<Category>>>
fun getCategoryById(id: String): Flow<Result<Category>>
suspend fun refreshCategories(): Result<Unit>
```

### FavoriteRepository

`domain/repository/FavoriteRepository.kt`

```kotlin
fun getFavoriteChannels(): Flow<Result<List<Channel>>>
fun isFavorite(channelId: String): Flow<Result<Boolean>>
suspend fun addFavorite(channelId: String): Result<Unit>
suspend fun removeFavorite(channelId: String): Result<Unit>
suspend fun toggleFavorite(channelId: String): Result<Unit>
suspend fun updateFavoriteOrder(channelId: String, newOrder: Int): Result<Unit>
suspend fun syncFavorites(): Result<Unit>
```

### PlaybackRepository

`domain/repository/PlaybackRepository.kt`

```kotlin
fun getPlaybackPosition(channelId: String): Flow<Result<Long?>>
suspend fun savePlaybackPosition(channelId: String, position: Long, duration: Long): Result<Unit>
suspend fun deletePlaybackPosition(channelId: String): Result<Unit>
fun getAllPlaybackPositions(): Flow<Result<Map<String, Long>>>
suspend fun clearOldPositions(keepCount: Int = 100): Result<Unit>
```

### PlaylistRepository

`domain/repository/PlaylistRepository.kt`

```kotlin
suspend fun parsePlaylistFromUrl(url: String): Result<List<Channel>>
suspend fun parsePlaylistFromString(content: String): Result<List<Channel>>
suspend fun importChannels(channels: List<Channel>): Result<Unit>
```

### SearchHistoryRepository

`domain/repository/SearchHistoryRepository.kt`

```kotlin
fun getRecentSearches(limit: Int = 10): Flow<Result<List<String>>>
suspend fun saveSearch(query: String): Result<Unit>
suspend fun clearHistory(): Result<Unit>
suspend fun removeSearch(query: String): Result<Unit>
```

### EpgRepository

`domain/repository/EpgRepository.kt`

Provides EPG (Electronic Program Guide) data — "Now" and "Next" program info for channels.

```kotlin
suspend fun ensureLoaded()
suspend fun getNowNext(tvgId: String): Pair<EpgProgram?, EpgProgram?>
fun getNowNextIfCached(tvgId: String): Pair<EpgProgram?, EpgProgram?>?
```

- `ensureLoaded()` — Fetches EPG data from the server if not already cached. Called once during ViewModel init.
- `getNowNext(tvgId)` — Suspend. Triggers load if needed, then returns current and next program.
- `getNowNextIfCached(tvgId)` — **Non-suspend, non-blocking.** Returns `null` if EPG not yet loaded (caller skips enrichment). Returns `Pair(null, null)` if loaded but no programs found for this tvgId. Used by `ChannelsViewModel.enrichWithEpgIfReady()` to avoid blocking channel display while EPG loads.

### UserPreferencesRepository

`domain/repository/UserPreferencesRepository.kt`

```kotlin
fun getTheme(): Flow<String>
suspend fun setTheme(theme: String): Result<Unit>
fun getGridSize(): Flow<Int>
suspend fun setGridSize(size: Int): Result<Unit>
fun getFontSize(): Flow<Float>
suspend fun setFontSize(scale: Float): Result<Unit>
fun getAnimationSpeed(): Flow<Float>
suspend fun setAnimationSpeed(speed: Float): Result<Unit>
fun getLayoutDensity(): Flow<String>
suspend fun setLayoutDensity(density: String): Result<Unit>
suspend fun clearCache(): Result<Unit>
```

## Use Cases

All use cases follow one of two base class patterns:

### Base Classes

**`UseCase<P, R>`** — For one-shot suspend operations:
```kotlin
abstract class UseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
    protected abstract suspend fun execute(params: P): R
}
```

**`FlowUseCase<P, R>`** — For reactive streaming operations:
```kotlin
abstract class FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
    protected abstract fun execute(params: P): Flow<R>
}
```

### Concrete Use Cases

| Use Case | Base | Input | Output | Description |
|----------|------|-------|--------|-------------|
| `GetChannelsUseCase` | `FlowUseCase` | `Unit` | `Flow<Result<List<Channel>>>` | Get all channels reactively |
| `GetChannelByIdUseCase` | `FlowUseCase` | `String` | `Flow<Result<Channel>>` | Get single channel by ID |
| `GetChannelsByCategoryUseCase` | `FlowUseCase` | `String` | `Flow<Result<List<Channel>>>` | Get channels filtered by category |
| `RefreshChannelsUseCase` | `UseCase` | `Unit` | `Result<Unit>` | Trigger remote refresh |
| `SearchChannelsUseCase` | `FlowUseCase` | `Params(query, filters)` | `Flow<Result<List<Channel>>>` | Search with filters (AND logic) |
| `GetRecentSearchesUseCase` | `FlowUseCase` | `Int` | `Flow<Result<List<String>>>` | Get recent search queries |
| `SaveSearchQueryUseCase` | `UseCase` | `String` | `Result<Unit>` | Save query to history |
| `ClearSearchHistoryUseCase` | `UseCase` | `Unit` | `Result<Unit>` | Clear all search history |
| `GetFavoriteChannelsUseCase` | `FlowUseCase` | `Unit` | `Flow<Result<List<Channel>>>` | Get favorites reactively |
| `ToggleFavoriteUseCase` | `UseCase` | `String` | `Result<Unit>` | Toggle channel favorite status |
| `ReorderFavoritesUseCase` | `UseCase` | `Params(channelId, newOrder)` | `Result<Unit>` | Update favorite display order |
| `GetPlaybackPositionUseCase` | `FlowUseCase` | `String` | `Flow<Result<PlaybackState?>>` | Get saved playback position |
| `SavePlaybackPositionUseCase` | `UseCase` | `Params(channelId, position, duration)` | `Result<Unit>` | Save playback position |

## Services

### ChannelHealthScanner

`domain/service/ChannelHealthScanner.kt`

Long-running service that periodically checks whether channel streams are reachable.

**Scan cycle:**
1. Full health scan of all channels (batches of 4, 6s timeout per stream)
2. 5-minute delay
3. Thumbnail extraction for ONLINE channels
4. 30-minute cooldown
5. Repeat

**Public API:**
```kotlin
val scanProgress: StateFlow<ScanProgress>  // scanned, total, isScanning
fun startAutoScan()
fun triggerManualScan()
fun stopScan()
```

**Stream checking strategy:**
- HLS streams: Validates response headers for `#EXTM3U`, `#EXT-X-STREAM-INF`, or `#EXTINF`
- Generic streams: Tries HEAD request, falls back to GET with Range header

### ChannelThumbnailExtractor

`domain/service/ChannelThumbnailExtractor.kt`

Extracts preview frames from online channel streams and caches them as JPEG thumbnails.

**Configuration:** 320x180 resolution, 70% JPEG quality, batches of 3.

**Public API:**
```kotlin
suspend fun extractThumbnails(): Int   // Returns count of successful extractions
suspend fun clearThumbnails()          // Clears all cached thumbnails
```

Thumbnails are stored in `cacheDir/thumbnails/{channelId}.jpg` and referenced via `ChannelHealthEntity.thumbnailPath`.

