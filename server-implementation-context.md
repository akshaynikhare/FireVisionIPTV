## Server-Side Implementation Context from Android TV App

The Android TV app (branch: `claude/sync-channels-stream-metrics-IuIQ2`) now sends the following requests that the server needs to handle:

### 1. `POST /api/v1/channels/:id/report-status`
```json
{
  "status": "dead" | "alive" | "unresponsive",
  "device_id": "android_id_string",
  "timestamp": 1710700000000,
  "error_message": "optional error details"
}
```
**When sent:**
- `"dead"` — after ExoPlayer exhausts 5 reconnection attempts
- `"unresponsive"` — when stream buffers for >30 seconds without producing frames
- `"alive"` — from health scanner when stream responds successfully

**Expected server behavior:** Increment `metrics.deadCount` / `metrics.aliveCount` / `metrics.unresponsiveCount` on the Channel document. Update `isWorking` accordingly. Rate limit: 1 report per channel per device per 5 minutes.

---

### 2. `POST /api/v1/channels/:id/report-play`
```json
{
  "device_id": "android_id_string",
  "timestamp": 1710700000000
}
```
**When sent:** After successful playback for 10+ seconds (player reaches `STATE_READY` and plays).

**Expected server behavior:** Increment `metrics.playCount` and `metrics.aliveCount`. Set `isWorking = true`. Rate limit: 1 per channel per device per minute.

---

### 3. `POST /api/v1/channels/health-sync`
```json
{
  "device_id": "android_id_string",
  "results": [
    { "channel_id": "ch123", "status": "alive", "response_time_ms": 450, "timestamp": 1710700000000 },
    { "channel_id": "ch456", "status": "dead", "response_time_ms": 6000, "timestamp": 1710700000000 }
  ]
}
```
**When sent:** After the app's `ChannelHealthScanner` completes a full scan cycle (scans all channels in batches of 4).

**Expected server behavior:** Bulk `bulkWrite` to increment the appropriate counter for each channel. Return `200 OK`.

---

### 4. `GET /api/v1/favorites`
```json
// Response:
{
  "channel_ids": ["ch123", "ch456", "ch789"],
  "timestamp": 1710700000000
}
```
**When fetched:** On app launch after pairing is verified. The app merges server favorites with local — any server-side favorites not in local DB are added.

---

### Channel Schema Changes Needed

Add to Channel model:

```javascript
metrics: {
  deadCount: { type: Number, default: 0 },
  aliveCount: { type: Number, default: 0 },
  unresponsiveCount: { type: Number, default: 0 },
  playCount: { type: Number, default: 0 },
  lastDeadAt: Date,
  lastAliveAt: Date,
  lastPlayedAt: Date,
  lastUnresponsiveAt: Date
}
```

The existing `isWorking` boolean stays as the current snapshot, but counters provide the history.

---

### Server `stream-prober` Update

When the existing stream-prober runs its liveness checks, it should also increment `metrics.aliveCount` or `metrics.deadCount` (matching the same counter system the clients use).

---

### Enhanced Admin Stats (`GET /admin/stats/stream-health`)

With the new counters, the admin dashboard can show:
- Streams ranked by `deadCount` (most unreliable)
- Streams ranked by `playCount` (most popular)
- Streams with high `deadCount` but zero `playCount` (candidates for removal)
- Streams with high `unresponsiveCount` (partial failures)
- Failure rate: `deadCount / (deadCount + aliveCount)`

---

### Rate Limiting Requirements

| Endpoint | Rate Limit |
|----------|-----------|
| `report-status` | 1 per channel per device per 5 minutes |
| `report-play` | 1 per channel per device per 1 minute |
| `health-sync` | 1 per device per 5 minutes |

---

### Android TV App Files (for reference)

Key files in the Android TV app that send these requests:

| File | What it does |
|------|-------------|
| `PlayerViewModel.kt` | Reports `dead` (after 5 retries), `unresponsive` (after 30s buffer), and `play` (after 10s playback) |
| `ErrorRecoveryManager.kt` | Detects unresponsive streams (buffering >30s without frames) |
| `ChannelHealthScanner.kt` | Bulk syncs all scan results via `health-sync` after each scan cycle |
| `FavoriteRepositoryImpl.kt` | Pulls favorites from `GET /api/v1/favorites` on app launch |
| `StreamMetricsRepositoryImpl.kt` | Handles all API calls, updates local `stream_metrics` Room table |
