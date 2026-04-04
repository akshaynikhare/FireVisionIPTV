# Favorites Workflow

## Data Model

```
favorites (Room table — NO foreign key to channels)
├── id: Long (auto-generated PK)
├── channelId: String (unique index)
├── addedAt: Long (timestamp)
└── displayOrder: Int (for reordering)
```

Favorites are **independent of the channels table**. No FK CASCADE — channel syncs never delete favorites. The JOIN query in `FavoriteDao.getFavoriteChannels()` naturally excludes favorites whose channel no longer exists.

## Data Flow

```mermaid
flowchart TD
    A[User Action] --> B{Entry Point}
    B -->|Long-press D-pad| C[ChannelCard]
    B -->|Toggle in player| D[PlayerViewModel]
    B -->|Overlay toggle| E[Player Overlay]
    B -->|Remove from list| F[FavoritesViewModel]

    C --> G[ViewModel.toggleFavorite]
    D --> G
    E --> G
    F --> G

    G --> H[ToggleFavoriteUseCase]
    H --> I[FavoriteRepositoryImpl.toggleFavorite]
    I --> J{isFavorite?}
    J -->|Yes| K[removeFavorite — Room]
    J -->|No| L[addFavorite — Room]

    K --> M{isPaired?}
    L --> M

    M -->|Yes| N[syncFavoritesInBackground\nPOST to server]
    M -->|No| O[Local only\nNo server sync]

    N --> P[Room Flow re-emits]
    O --> P
    P --> Q[UI updates reactively]
```

## Paired vs Unpaired Users

| Behavior | Paired | Unpaired (default playlist) |
|----------|--------|----------------------------|
| Local favorites | Yes | Yes |
| Server sync on add/remove | Yes | **No** |
| Pull from server | Yes | **No** |
| Persistence | Room DB | Room DB |

Detection: `AppPreferences.getTvCode() != DEFAULT_TV_CODE`

## Entry Points (Add/Remove Favorite)

1. **ChannelCard** (All Channels, Search, Home rows)
   - D-pad long-press (600ms) toggles favorite
   - Menu/Bookmark key instant toggle
   - Uses `onPreviewKeyEvent` to intercept before Card consumes events

2. **Player** (current channel)
   - `PlayerViewModel.toggleFavorite()` — optimistic UI update, revert on error

3. **Player Overlay** (channel list in overlay)
   - `PlayerViewModel.toggleOverlayFavorite(channelId)` — optimistic per-channel

4. **Favorites Screen**
   - `FavoritesViewModel.removeFavorite(channelId)` — optimistic remove from list

## Favorites Screen

- Grid of `ChannelCard` components showing only favorited channels
- Room Flow is reactive — updates when favorites change from any source
- Combined with `channelHealthDao.getAllHealth()` for health status dots
- Supports reordering via `moveFavoriteUp/Down` (updates `displayOrder`)

## Protection Against Data Loss

| Threat | Protection |
|--------|-----------|
| Channel refresh (sync) | `@Upsert` instead of `@Insert(REPLACE)` — no DELETE+INSERT |
| Channel deleted from server | No FK CASCADE — favorite row persists, JOIN excludes it |
| Health scan cleanup | `cleanupOrphaned()` runs after scan, not before |
| Server sync failure | Local DB is source of truth; sync is fire-and-forget |
| App crash during sync | Local write happens first, sync is separate coroutine |

## Server Sync Flow (Paired Users Only)

```mermaid
sequenceDiagram
    participant User
    participant VM as ViewModel
    participant Repo as FavoriteRepositoryImpl
    participant Room as Room DB
    participant Server as FireVision Server

    User->>VM: toggleFavorite(channelId)
    VM->>Repo: toggleFavorite(channelId)
    Repo->>Room: addFavorite() / removeFavorite()
    Room-->>Repo: Done (immediate)
    Room-->>VM: Flow re-emits
    VM-->>User: UI updated

    alt isPaired = true
        Repo->>Repo: syncFavoritesInBackground()
        Repo->>Room: getAllFavoriteChannelIds()
        Room-->>Repo: [channelId list]
        Repo->>Server: POST /favorites (channelIds + deviceId)
        Server-->>Repo: 200 OK / error
        Note over Repo: Success or failure does NOT affect local state
    else isPaired = false
        Note over Repo: Skip server sync entirely
    end
```

## Room Queries

- `getFavoriteChannels()`: `INNER JOIN channels ON id = channelId WHERE isActive = 1 ORDER BY displayOrder, addedAt DESC`
- `isFavorite(channelId)`: `EXISTS(SELECT 1 FROM favorites WHERE channelId = ?)`
- `addFavorite()`: `INSERT OR REPLACE`
- `removeFavorite()`: `DELETE WHERE channelId = ?`
