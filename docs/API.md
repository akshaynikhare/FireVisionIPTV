# API Reference

## REST API Endpoints

Base URL: Configured via `BuildConfig.API_BASE_URL` (default: `https://tv.cadnative.com/`)

All requests include these headers (configured in `NetworkModule`):
```
Accept: application/json
Content-Type: application/json
X-TV-Code: <tv_code_from_settings>
```

### Channels

#### GET /api/v1/channels

Fetch all channels.

**Response:** `ChannelsResponse`
```json
{
  "success": true,
  "data": [
    {
      "channelId": "ch_123",
      "channelName": "BBC World",
      "channelUrl": "https://stream.example.com/bbc.m3u8",
      "channelImg": "https://img.example.com/bbc.png",
      "tvgLogo": "https://img.example.com/bbc-hd.png",
      "channelGroup": "News",
      "channelDrmKey": null,
      "channelDrmType": null,
      "tvgLanguage": "en",
      "tvgCountry": "GB",
      "tvgId": "BBCWorld.uk",
      "tvgName": "BBC World News",
      "isActive": true,
      "metadata": { "language": "English" }
    }
  ]
}
```

#### GET /api/v1/channels/{id}

Fetch a single channel by ID.

**Path parameter:** `id` — Channel identifier

**Response:** `ChannelDto` (same structure as items in the channels array)

### Categories

#### GET /api/v1/categories

Fetch all channel categories.

**Response:** `CategoriesResponse`
```json
{
  "categories": [
    {
      "id": "news",
      "name": "News",
      "display_order": 1,
      "channel_count": 45
    }
  ],
  "total": 12
}
```

### Favorites

#### GET /api/v1/favorites

Pull server-stored favorites on app launch. Merged with local Room favorites (server timestamp wins on conflict).

**Response:** `FavoritesResponse`
```json
{
  "channel_ids": ["ch_123", "ch_456"],
  "timestamp": 1710500000000
}
```

#### POST /api/v1/favorites

Sync local favorites to server.

**Request body:** `FavoritesRequest`
```json
{
  "channel_ids": ["ch_123", "ch_456"],
  "device_id": "device_abc",
  "timestamp": 1710500000000
}
```

**Response:** `204 No Content` on success

### Stream Health Reporting

#### POST /api/v1/channels/{id}/report-status

Report a stream as dead, alive, or unresponsive. Called fire-and-forget from `PlayerViewModel` — never blocks playback.

**Path parameter:** `id` — Channel identifier

**Request body:** `StreamStatusReport`
```json
{
  "status": "dead",
  "deviceId": "device_abc",
  "timestamp": "2026-04-02T10:00:00Z",
  "errorMessage": "Connection refused"
}
```

- `status`: `"dead"` (5 retries exhausted), `"alive"` (stream confirmed working), `"unresponsive"` (buffering >30 s)
- Rate limited server-side: 1 report per channel per device per 5 minutes

**Response:** `Response<Unit>` (body ignored)

#### POST /api/v1/channels/{id}/report-play

Report a successful play event (stream produced frames for ≥10 s). Also used when an alternate stream played — the `streamUrl` field triggers server-side auto-promotion of that alternate to primary.

**Request body:** `StreamPlayReport`
```json
{
  "deviceId": "device_abc",
  "timestamp": "2026-04-02T10:05:00Z",
  "proxyPlay": false,
  "streamUrl": "https://alt-stream.example.com/live.m3u8"
}
```

- `proxyPlay`: `true` when ExoPlayer fell back to the TV proxy
- `streamUrl` (optional): actual stream URL played; if it matches an alternate, server auto-promotes it to primary
- Rate limited server-side: 1 report per channel per device per minute

**Response:** `Response<Unit>` (body ignored)

#### POST /api/v1/channels/health-sync

Bulk sync results from a `ChannelHealthScanner` batch. Called after each scan cycle.

**Request body:** `HealthSyncRequest`
```json
{
  "deviceId": "device_abc",
  "results": [
    {
      "channelId": "ch_123",
      "status": "alive",
      "responseTimeMs": 320,
      "timestamp": "2026-04-02T10:10:00Z"
    }
  ]
}
```

- Up to 100 results per call; server uses MongoDB `bulkWrite` for efficiency
- Rate limited: 1 sync per device per 5 minutes

**Response:** `Response<Unit>` (body ignored)

### Fallback Streams

#### GET /api/v1/user-playlist/me/channels-with-fallbacks

Returns the user's assigned channels, each including up to 3 alternate stream URLs. Dead and flagged alternates are filtered server-side before sending. Used by `ChannelRepositoryImpl` to populate the in-memory fallback slot queue in `ErrorRecoveryManager`.

**Response:** same shape as `/me/channels` with an extra `alternateStreams` array on each channel:
```json
{
  "streamUrl": "https://alt.example.com/live.m3u8",
  "quality": "720p"
}
```

### Playlist

#### GET /api/v1/playlist.m3u

Download the full channel playlist in M3U format.

**Response:** `ResponseBody` (plain text M3U content)

### App Updates (Legacy ApiClient)

#### GET /api/v1/app/version

Check for app updates.

**Query parameter:** `currentVersion` — Current app version code (integer)

**Response:**
```json
{
  "updateAvailable": true,
  "isMandatory": false,
  "versionName": "1.6",
  "versionCode": 6,
  "downloadUrl": "https://tv.cadnative.com/api/v1/app/download",
  "fileSize": 15728640,
  "releaseNotes": "Bug fixes and performance improvements"
}
```

#### GET /api/v1/app/download

Download the latest APK binary.

### Device Pairing (PairingViewModel)

#### POST /api/v1/tv/pairing/request

Request a new pairing PIN.

**Request body:**
```json
{
  "deviceId": "...",
  "deviceName": "...",
  "deviceModel": "..."
}
```

**Response:**
```json
{
  "pin": "123456",
  "expiresAt": "2026-03-15T12:00:00Z"
}
```

#### GET /api/v1/tv/pairing/status/{pin}

Poll pairing status.

**Path parameter:** `pin` — 6-digit PIN from pairing request

**Response (pending):**
```json
{
  "status": "pending"
}
```

**Response (paired):**
```json
{
  "status": "paired",
  "channelListCode": "ABC123",
  "username": "user@example.com"
}
```

---

## Repository Interfaces

### ChannelRepository

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

```kotlin
fun getCategories(): Flow<Result<List<Category>>>
fun getCategoryById(id: String): Flow<Result<Category>>
suspend fun refreshCategories(): Result<Unit>
```

### FavoriteRepository

```kotlin
fun getFavoriteChannels(): Flow<Result<List<Channel>>>
fun isFavorite(channelId: String): Flow<Result<Boolean>>
suspend fun addFavorite(channelId: String): Result<Unit>
suspend fun removeFavorite(channelId: String): Result<Unit>
suspend fun toggleFavorite(channelId: String): Result<Unit>
suspend fun updateFavoriteOrder(channelId: String, newOrder: Int): Result<Unit>
suspend fun syncFavorites(): Result<Unit>
suspend fun pullFavoritesFromServer(): Result<Unit>  // pulls & merges server favorites on app launch
```

### PlaybackRepository

```kotlin
fun getPlaybackPosition(channelId: String): Flow<Result<Long?>>
suspend fun savePlaybackPosition(channelId: String, position: Long, duration: Long): Result<Unit>
suspend fun deletePlaybackPosition(channelId: String): Result<Unit>
fun getAllPlaybackPositions(): Flow<Result<Map<String, Long>>>
suspend fun clearOldPositions(keepCount: Int = 100): Result<Unit>
```

### PlaylistRepository

```kotlin
suspend fun parsePlaylistFromUrl(url: String): Result<List<Channel>>
suspend fun parsePlaylistFromString(content: String): Result<List<Channel>>
suspend fun importChannels(channels: List<Channel>): Result<Unit>
```

### SearchHistoryRepository

```kotlin
fun getRecentSearches(limit: Int = 10): Flow<Result<List<String>>>
suspend fun saveSearch(query: String): Result<Unit>
suspend fun clearHistory(): Result<Unit>
suspend fun removeSearch(query: String): Result<Unit>
```

### UserPreferencesRepository

```kotlin
fun getTheme(): Flow<String>                              // default: "dark"
suspend fun setTheme(theme: String): Result<Unit>
fun getGridSize(): Flow<Int>                              // default: 3
suspend fun setGridSize(size: Int): Result<Unit>
fun getFontSize(): Flow<Float>                            // default: 1.0f
suspend fun setFontSize(scale: Float): Result<Unit>
fun getAnimationSpeed(): Flow<Float>                      // default: 1.0f
suspend fun setAnimationSpeed(speed: Float): Result<Unit>
fun getLayoutDensity(): Flow<String>                      // default: "comfortable"
suspend fun setLayoutDensity(density: String): Result<Unit>
suspend fun clearCache(): Result<Unit>
```

---

## Use Cases

| Use Case | Type | Input | Output |
|----------|------|-------|--------|
| `GetChannelsUseCase` | Flow | `Unit` | `Flow<Result<List<Channel>>>` |
| `GetChannelByIdUseCase` | Flow | `String` (channelId) | `Flow<Result<Channel>>` |
| `GetChannelsByCategoryUseCase` | Flow | `String` (category) | `Flow<Result<List<Channel>>>` |
| `RefreshChannelsUseCase` | Suspend | `Unit` | `Result<Unit>` |
| `SearchChannelsUseCase` | Flow | `Params(query, filters)` | `Flow<Result<List<Channel>>>` |
| `GetRecentSearchesUseCase` | Flow | `Int` (limit) | `Flow<Result<List<String>>>` |
| `SaveSearchQueryUseCase` | Suspend | `String` (query) | `Result<Unit>` |
| `ClearSearchHistoryUseCase` | Suspend | `Unit` | `Result<Unit>` |
| `GetFavoriteChannelsUseCase` | Flow | `Unit` | `Flow<Result<List<Channel>>>` |
| `ToggleFavoriteUseCase` | Suspend | `String` (channelId) | `Result<Unit>` |
| `ReorderFavoritesUseCase` | Suspend | `Params(channelId, newOrder)` | `Result<Unit>` |
| `PullFavoritesUseCase` | Suspend | `Unit` | `Result<Unit>` |
| `GetPlaybackPositionUseCase` | Flow | `String` (channelId) | `Flow<Result<PlaybackState?>>` |
| `SavePlaybackPositionUseCase` | Suspend | `Params(channelId, position, duration)` | `Result<Unit>` |
| `ReportStreamStatusUseCase` | Suspend | `Params(channelId, status, deviceId, errorMessage?)` | `Result<Unit>` |
| `ReportStreamPlayUseCase` | Suspend | `Params(channelId, deviceId, proxyPlay, streamUrl?)` | `Result<Unit>` |
| `SyncHealthResultsUseCase` | Suspend | `Params(deviceId, results[])` | `Result<Unit>` |

---

## Database Schema

### Entity Relationship Diagram

```text
┌──────────────┐       ┌──────────────┐
│   channels   │◄──FK──│  favorites   │
│              │       │              │
│ id (PK)      │       │ id (PK, AI)  │
│ name         │       │ channelId    │
│ streamUrl    │       │ addedAt      │
│ logoUrl      │       │ displayOrder │
│ categoryId   │       └──────────────┘
│ language     │
│ country      │       ┌──────────────────┐
│ groupTitle   │◄──FK──│  channel_health   │
│ tvgId        │       │                  │
│ tvgName      │       │ channelId (PK)   │
│ isActive     │       │ status           │
│ lastUpdated  │       │ lastCheckedAt    │
└──────────────┘       │ responseTimeMs   │
                       │ errorMessage     │
┌──────────────┐       │ thumbnailPath    │
│  categories  │       └──────────────────┘
│              │
│ id (PK)      │       ┌────────────────────┐
│ name         │       │ playback_positions  │
│ displayOrder │       │                    │
│ channelCount │       │ channelId (PK)     │
└──────────────┘       │ position           │
                       │ duration           │
┌──────────────────┐   │ lastPlayed         │
│  search_history  │   └────────────────────┘
│                  │
│ id (PK, AI)      │
│ query            │
│ timestamp        │
└──────────────────┘
```

### Foreign Keys

| Child Table | Parent Table | Column | On Delete |
|-------------|-------------|--------|-----------|
| `favorites` | `channels` | `channelId` → `id` | CASCADE |
| `channel_health` | `channels` | `channelId` → `id` | CASCADE |

### Indices

| Table | Index | Columns |
|-------|-------|---------|
| `channels` | `index_channels_categoryId` | `categoryId` |
| `channels` | `index_channels_isActive` | `isActive` |
| `channels` | `index_channels_name` | `name` |
| `channel_health` | `index_channel_health_channelId` (unique) | `channelId` |
| `channel_health` | `index_channel_health_lastCheckedAt` | `lastCheckedAt` |
| `channel_health` | `index_channel_health_status` | `status` |
| `favorites` | `index_favorites_channelId` | `channelId` |
| `playback_positions` | `index_playback_positions_lastPlayed` | `lastPlayed` |
| `search_history` | `index_search_history_timestamp` | `timestamp` |
