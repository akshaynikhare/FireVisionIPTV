# Data Layer

The data layer implements repository interfaces defined in the domain layer. It manages local persistence via Room, remote API calls via Retrofit, and data mapping between DTOs, entities, and domain models.

**Package:** `com.cadnative.firevisioniptv.data`

## Package Structure

```
data/
├── mapper/                  # Bidirectional data mappers
│   ├── CategoryMapper.kt
│   └── ChannelMapper.kt
├── model/
│   ├── dto/                 # API response/request DTOs
│   │   ├── CategoriesResponse.kt
│   │   ├── CategoryDto.kt
│   │   ├── ChannelDto.kt
│   │   ├── ChannelsResponse.kt
│   │   └── FavoritesRequest.kt
│   └── Result.kt            # Sealed Result<T> wrapper
├── repository/              # Repository implementations
│   ├── CategoryRepositoryImpl.kt
│   ├── ChannelRepositoryImpl.kt
│   ├── FavoriteRepositoryImpl.kt
│   ├── PlaybackRepositoryImpl.kt
│   ├── SearchHistoryRepositoryImpl.kt
│   └── UserPreferencesRepositoryImpl.kt
└── source/
    ├── local/
    │   ├── dao/             # Room DAOs
    │   │   ├── CategoryDao.kt
    │   │   ├── ChannelDao.kt
    │   │   ├── ChannelHealthDao.kt
    │   │   ├── FavoriteDao.kt
    │   │   ├── PlaybackPositionDao.kt
    │   │   └── SearchHistoryDao.kt
    │   ├── entity/          # Room entities
    │   │   ├── CategoryEntity.kt
    │   │   ├── ChannelEntity.kt
    │   │   ├── ChannelHealthEntity.kt
    │   │   ├── FavoriteEntity.kt
    │   │   ├── PlaybackPositionEntity.kt
    │   │   └── SearchHistoryEntity.kt
    │   ├── FireVisionDatabase.kt
    │   ├── CategoryLocalDataSource.kt
    │   ├── ChannelLocalDataSource.kt
    │   ├── FavoriteLocalDataSource.kt
    │   ├── PlaybackLocalDataSource.kt
    │   └── SearchHistoryLocalDataSource.kt
    └── remote/
        ├── FireVisionApiService.kt
        ├── CategoryRemoteDataSource.kt
        └── ChannelRemoteDataSource.kt
```

## Room Database

### FireVisionDatabase

`data/source/local/FireVisionDatabase.kt`

- **Version:** 3
- **Schema export:** Enabled (schemas stored in `app/schemas/`)
- **Migration strategy:** `fallbackToDestructiveMigration()` (development mode)

Provides 6 DAOs:
```kotlin
abstract fun channelDao(): ChannelDao
abstract fun categoryDao(): CategoryDao
abstract fun favoriteDao(): FavoriteDao
abstract fun searchHistoryDao(): SearchHistoryDao
abstract fun playbackPositionDao(): PlaybackPositionDao
abstract fun channelHealthDao(): ChannelHealthDao
```

### Entities

#### channels

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | TEXT | PRIMARY KEY | Channel identifier |
| `name` | TEXT | NOT NULL, INDEXED | Display name |
| `streamUrl` | TEXT | NOT NULL | Streaming URL |
| `logoUrl` | TEXT | nullable | Logo image URL |
| `categoryId` | TEXT | NOT NULL, INDEXED | Category reference |
| `language` | TEXT | nullable | Language code |
| `country` | TEXT | nullable | Country code |
| `groupTitle` | TEXT | nullable | M3U group title |
| `tvgId` | TEXT | nullable | TVG identifier |
| `tvgName` | TEXT | nullable | TVG name |
| `isActive` | INTEGER | NOT NULL, INDEXED, default 1 | Active flag |
| `lastUpdated` | INTEGER | NOT NULL | Timestamp |

#### categories

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | TEXT | PRIMARY KEY |
| `name` | TEXT | NOT NULL |
| `displayOrder` | INTEGER | NOT NULL, default 0 |
| `channelCount` | INTEGER | NOT NULL, default 0 |

#### channel_health

| Column | Type | Constraints |
|--------|------|-------------|
| `channelId` | TEXT | PRIMARY KEY, FK → channels(id) ON DELETE CASCADE |
| `status` | TEXT | NOT NULL |
| `lastCheckedAt` | INTEGER | NOT NULL, INDEXED, default 0 |
| `responseTimeMs` | INTEGER | nullable |
| `errorMessage` | TEXT | nullable |
| `thumbnailPath` | TEXT | nullable |

Unique index on `channelId`. Additional index on `status`.

#### favorites

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `channelId` | TEXT | NOT NULL, INDEXED, FK → channels(id) ON DELETE CASCADE |
| `addedAt` | INTEGER | NOT NULL |
| `displayOrder` | INTEGER | NOT NULL, default 0 |

#### playback_positions

| Column | Type | Constraints |
|--------|------|-------------|
| `channelId` | TEXT | PRIMARY KEY |
| `position` | INTEGER | NOT NULL |
| `duration` | INTEGER | NOT NULL |
| `lastPlayed` | INTEGER | NOT NULL, INDEXED |

#### search_history

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `query` | TEXT | NOT NULL |
| `timestamp` | INTEGER | NOT NULL, INDEXED |

### Key DAO Queries

**ChannelDao:**
- `getAllChannels()` — Active channels ordered by name, returns `Flow`
- `searchChannels(query)` — Case-insensitive LIKE search on `name` and `groupTitle`
- `replaceAllChannels(channels)` — `@Transaction`: atomic delete-all + insert-all

**ChannelHealthDao:**
- `upsertPreservingThumbnail(...)` — Updates health status while preserving existing `thumbnailPath`
- `getOnlineChannelIdsWithoutThumbnail()` — Returns ONLINE channels with NULL `thumbnailPath`
- `getAllChannelIdsByPriority()` — Ordered by `lastCheckedAt` ASC (oldest first)
- `cleanupOrphaned()` — Deletes health records with no matching channel

**FavoriteDao:**
- `getFavoriteChannels()` — JOIN query: `favorites INNER JOIN channels`, ordered by `displayOrder` ASC, `addedAt` DESC
- `isFavorite(channelId)` — EXISTS subquery returning `Flow<Boolean>`

## Remote API

### FireVisionApiService

`data/source/remote/FireVisionApiService.kt`

Retrofit interface:

| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| `GET` | `/api/v1/channels` | — | `Response<ChannelsResponse>` |
| `GET` | `/api/v1/channels/{id}` | `@Path("id")` | `Response<ChannelDto>` |
| `GET` | `/api/v1/categories` | — | `Response<CategoriesResponse>` |
| `POST` | `/api/v1/favorites` | `@Body FavoritesRequest` | `Response<Unit>` |
| `GET` | `/api/v1/playlist.m3u` | — | `Response<ResponseBody>` |

### DTOs

**ChannelsResponse:**
```kotlin
data class ChannelsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ChannelDto>
)
```

**ChannelDto:**
```kotlin
data class ChannelDto(
    @SerializedName("channelId") val id: String,
    @SerializedName("channelName") val name: String,
    @SerializedName("channelUrl") val url: String,
    @SerializedName("channelImg") val channelImg: String?,
    val tvgLogo: String?,
    @SerializedName("channelGroup") val groupTitle: String?,
    @SerializedName("channelDrmKey") val drmKey: String?,
    @SerializedName("channelDrmType") val drmType: String?,
    val tvgLanguage: String?,
    val tvgCountry: String?,
    val tvgId: String?,
    val tvgName: String?,
    val isActive: Boolean,
    val metadata: ChannelMetadataDto?
)
// Computed: logoUrl = tvgLogo ?: channelImg
```

**CategoriesResponse:**
```kotlin
data class CategoriesResponse(
    @SerializedName("categories") val categories: List<CategoryDto>,
    @SerializedName("total") val total: Int?
)
```

**FavoritesRequest:**
```kotlin
data class FavoritesRequest(
    @SerializedName("channel_ids") val channelIds: List<String>,
    @SerializedName("device_id") val deviceId: String?,
    @SerializedName("timestamp") val timestamp: Long
)
```

### Error Handling

Remote data sources map HTTP errors to typed exceptions:

| HTTP Code | Exception |
|-----------|-----------|
| Network failure | `NetworkException` |
| 400 | `BadRequestException` |
| 401 | `UnauthorizedException` |
| 403 | `ForbiddenException` |
| 404 | `NotFoundException` |
| 500 | `ServerException` |
| 503 | `ServiceUnavailableException` |
| Other | `UnknownException` |

## Data Mappers

### ChannelMapper

`data/mapper/ChannelMapper.kt` — `@Singleton`

| Method | From | To | Notes |
|--------|------|----|-------|
| `toDomain(entity, isFavorite)` | `ChannelEntity` | `Channel` | Accepts optional favorite flag |
| `toEntity(dto)` | `ChannelDto` | `ChannelEntity` | Resolves logo from `tvgLogo` or `channelImg`, defaults category to `"uncategorized"` |
| `fromDomain(channel)` | `Channel` | `ChannelEntity` | Reverse mapping |

### CategoryMapper

`data/mapper/CategoryMapper.kt` — `@Singleton`

| Method | From | To |
|--------|------|----|
| `toDomain(entity)` | `CategoryEntity` | `Category` |
| `toEntity(dto)` | `CategoryDto` | `CategoryEntity` |
| `fromDomain(category)` | `Category` | `CategoryEntity` |

## Result Wrapper

`data/model/Result.kt`

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    val isSuccess: Boolean
    val isError: Boolean
    fun getOrNull(): T?
    fun exceptionOrNull(): Exception?

    companion object {
        fun <T> success(data: T): Result<T>
        fun error(exception: Exception): Result<Nothing>
    }
}
```

## Repository Implementations

All repositories are `@Singleton` and follow the **offline-first** pattern: local database is the single source of truth, remote data refreshes the cache.

### ChannelRepositoryImpl

- `getChannels()` — Combines `ChannelDao.getAllChannels()` with `FavoriteDao.getAllFavorites()` using `flow.combine()` to enrich channels with favorite status
- `refreshChannels()` — Fetches from API, maps DTOs → entities, atomically replaces all local channels via `@Transaction`

### FavoriteRepositoryImpl

- `addFavorite()` / `removeFavorite()` — Updates local DB, then fires background sync to `POST /api/v1/favorites`
- `syncFavorites()` — Collects all local favorite channel IDs and sends to server

### UserPreferencesRepositoryImpl

- Uses `SharedPreferences` (not Room) with `MutableStateFlow` for reactive preference updates
- `clearCache()` — Recursively deletes `context.cacheDir`

## How to Extend

### Adding a new entity

1. Create entity class in `data/source/local/entity/` with `@Entity` annotation
2. Create DAO interface in `data/source/local/dao/` with `@Dao` annotation
3. Add entity to `@Database(entities = [...])` in `FireVisionDatabase`
4. Add abstract DAO getter in `FireVisionDatabase`
5. Provide the DAO via `DatabaseModule`
6. Increment database version (or use destructive migration in dev)

### Adding a new API endpoint

1. Add method to `FireVisionApiService` with Retrofit annotations
2. Add DTO classes in `data/model/dto/` with `@SerializedName` annotations
3. Add fetch method to the relevant remote data source
4. Call from the repository implementation
