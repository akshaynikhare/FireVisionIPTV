## Summary

Unify the channel health, favorites, and playback metrics systems between the Android TV app and the FireVisionIPTVServer so both benefit from shared data. Currently the app runs its own `ChannelHealthScanner` and the server runs its own `stream-prober`, but neither shares results with the other. Similarly, playback success/failure data stays local.

## Problem

1. **Health data is siloed** — The app's `ChannelHealthScanner` flags streams ONLINE/OFFLINE locally (`ChannelHealthEntity`), but never reports this to the server. The server's `stream-prober` does independent liveness checks. Neither system benefits from the other's findings.
2. **Favorites sync is one-way** — `FavoriteRepositoryImpl` posts `channelIds` to `POST /api/v1/favorites`, but doesn't pull the server's stored favorites on app launch or device re-pair.
3. **No stream play metrics** — When a user successfully plays a stream, that "working" signal is lost. There's no play counter to validate stream health from real user activity.
4. **Dead stream detection is incomplete** — `ErrorRecoveryManager` retries 5 times and calls `onStreamDead()`, which updates local `ChannelHealthEntity` to OFFLINE. The server never learns about this failure.
5. **No "unresponsive" stream state** — Streams that partially load or timeout aren't distinguished from fully dead streams.

## Proposed Changes

### 1. New API Endpoints (coordinate with server repo)

The app needs to call these new server endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/channels/{id}/report-status` | POST | Report stream dead/alive/unresponsive from client |
| `/api/v1/channels/{id}/report-play` | POST | Increment play counter on successful playback |
| `/api/v1/channels/health-sync` | POST | Bulk sync health scan results from app → server |
| `/api/v1/favorites` | GET | Pull favorites from server (already exists) |

### 2. Stream Status Reporting from Playback (`ErrorRecoveryManager` / `PlayerViewModel`)

- After `ErrorRecoveryManager` exhausts 5 retries and calls `onStreamDead()`:
  - POST to `/api/v1/channels/{id}/report-status` with `{ status: "dead", deviceId, timestamp, errorMessage }`
  - This increments the server-side `deadCounter` for this stream

- After successful playback (player reaches `STATE_READY` and plays for ≥10 seconds):
  - POST to `/api/v1/channels/{id}/report-play` with `{ deviceId, timestamp }`
  - This increments the server-side `playCounter` and `worksCounter`

- New "unresponsive" state: if the stream connects but buffers for >30 seconds without producing frames:
  - POST to `/api/v1/channels/{id}/report-status` with `{ status: "unresponsive", deviceId, timestamp }`

### 3. Health Scan Sync (`ChannelHealthScanner`)

- After a batch health scan completes, bulk-sync results to server via `POST /api/v1/channels/health-sync`
- Payload: array of `{ channelId, status: "alive"|"dead"|"unresponsive", responseTimeMs, timestamp }`
- Each report increments the corresponding server-side counter for that stream

### 4. Bidirectional Favorites Sync

- On app launch (after pairing verified), pull favorites from `GET /api/v1/favorites` and merge with local `FavoriteEntity` table
- Continue pushing local changes to `POST /api/v1/favorites` as currently done
- Handle conflict resolution: server timestamp vs local timestamp, most recent wins

### 5. New Domain Models & Entities

- Add `StreamMetrics` domain model: `channelId`, `playCount`, `worksCount`, `deadCount`, `unresponsiveCount`, `lastReportedAt`
- Extend `ChannelHealthEntity` or create new `StreamMetricsEntity` for local caching of counters
- Add `ChannelHealthStatus.UNRESPONSIVE` enum value

### 6. New Use Cases

- `ReportStreamStatusUseCase` — called from PlayerViewModel when stream dies or is unresponsive
- `ReportStreamPlayUseCase` — called from PlayerViewModel on successful playback
- `SyncHealthResultsUseCase` — called from ChannelHealthScanner after scan batch
- `PullFavoritesUseCase` — called on app startup to fetch server favorites

### 7. FireVisionApiService Additions

Add to `FireVisionApiService.kt`:

```kotlin
@POST("api/v1/channels/{channelId}/report-status")
suspend fun reportStreamStatus(
    @Path("channelId") channelId: String,
    @Body report: StreamStatusReport
): Response<Unit>

@POST("api/v1/channels/{channelId}/report-play")
suspend fun reportStreamPlay(
    @Path("channelId") channelId: String,
    @Body report: StreamPlayReport
): Response<Unit>

@POST("api/v1/channels/health-sync")
suspend fun syncHealthResults(
    @Body results: HealthSyncRequest
): Response<Unit>

@GET("api/v1/favorites")
suspend fun getFavorites(): Response<FavoritesResponse>
```

## Files to Modify

| File | Change |
|------|--------|
| `data/source/remote/FireVisionApiService.kt` | Add 4 new endpoints |
| `data/model/dto/` | New DTOs: `StreamStatusReport`, `StreamPlayReport`, `HealthSyncRequest`, `FavoritesResponse` |
| `domain/model/` | New `StreamMetrics` model, add `UNRESPONSIVE` to `ChannelHealthStatus` |
| `domain/usecase/` | 4 new use cases |
| `domain/repository/` | Extend `ChannelHealthRepository` or new `StreamMetricsRepository` |
| `data/repository/` | Implement new repository methods |
| `presentation/ui/player/ErrorRecoveryManager.kt` | Add unresponsive detection (buffering >30s) |
| `presentation/viewmodel/PlayerViewModel.kt` | Call report use cases on dead/play/unresponsive events |
| `domain/service/ChannelHealthScanner.kt` | Bulk sync results to server after scan |
| `data/repository/FavoriteRepositoryImpl.kt` | Add pull-from-server logic |
| `data/source/local/entity/` | Optional `StreamMetricsEntity` |
| `data/source/local/dao/` | Optional `StreamMetricsDao` |

## Acceptance Criteria

- [ ] When a stream fails after 5 retries, server `deadCounter` for that channel increments
- [ ] When a stream plays successfully for 10+ seconds, server `playCounter` increments
- [ ] When a stream buffers >30s without frames, it's reported as "unresponsive"
- [ ] App health scan results are synced to server after each scan cycle
- [ ] Favorites are pulled from server on app launch and merged with local data
- [ ] All new API calls are fire-and-forget (don't block UI or playback)
- [ ] App works offline — metrics queue locally and sync when connectivity returns

## Related

- Depends on corresponding server issue in `akshaynikhare/FireVisionIPTVServer`
