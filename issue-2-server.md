## Summary

Extend the server to track per-stream health metrics as counters (not just a boolean `isWorking`), accepting reports from both the server's own `stream-prober` and from Android TV client devices. This creates a unified view of stream reliability over time.

## Problem

1. **`isWorking` is binary and ephemeral** — The `Channel` model only stores a single boolean `isWorking` and `lastTested` timestamp. There's no history of how often a stream fails vs works.
2. **No client-reported metrics** — When a user's TV app fails to play a stream (after 5 retries) or successfully plays one, the server never learns. This real-world signal is more valuable than synthetic probes.
3. **No "unresponsive" classification** — Streams that connect but never deliver frames aren't tracked differently from fully dead streams.
4. **No play counting** — There's no way to know which streams are actually being watched, which would help prioritize health monitoring.
5. **Favorites sync is incomplete** — `GET /api/v1/favorites` exists but the sync flow between app and server needs to be bidirectional and robust.

## Proposed Changes

### 1. Extend Channel Schema with Metrics Counters

Add to `Channel.ts` model:

```typescript
// Stream health metrics (cumulative counters)
metrics: {
  deadCount: { type: Number, default: 0 },      // Times flagged dead (server probe + client reports)
  aliveCount: { type: Number, default: 0 },      // Times flagged alive (server probe + client reports)
  unresponsiveCount: { type: Number, default: 0 },// Times flagged unresponsive (connects but no frames)
  playCount: { type: Number, default: 0 },        // Times successfully played by a user
  lastDeadAt: { type: Date },
  lastAliveAt: { type: Date },
  lastPlayedAt: { type: Date },
  lastUnresponsiveAt: { type: Date },
}
```

The existing `isWorking` boolean stays as the current snapshot, but counters provide the history.

### 2. New API Endpoints

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `POST /api/v1/channels/:id/report-status` | POST | TV/Session | Client reports stream dead/alive/unresponsive |
| `POST /api/v1/channels/:id/report-play` | POST | TV/Session | Client reports successful playback |
| `POST /api/v1/channels/health-sync` | POST | TV/Session | Bulk health sync from client health scanner |
| `GET /admin/stats/stream-health` | GET | Admin | Enhanced: include counter-based analytics |

#### `POST /api/v1/channels/:id/report-status`

```json
{
  "status": "dead | alive | unresponsive",
  "deviceId": "string",
  "timestamp": "ISO8601",
  "errorMessage": "optional string"
}
```

- Increments `metrics.deadCount`, `metrics.aliveCount`, or `metrics.unresponsiveCount`
- Updates `lastDeadAt`, `lastAliveAt`, or `lastUnresponsiveAt`
- If status is "alive", sets `isWorking = true`; if "dead", sets `isWorking = false`
- Rate limit: 1 report per channel per device per 5 minutes

#### `POST /api/v1/channels/:id/report-play`

```json
{
  "deviceId": "string",
  "timestamp": "ISO8601"
}
```

- Increments `metrics.playCount` and `metrics.aliveCount` (playing = confirmed working)
- Updates `lastPlayedAt` and `lastAliveAt`
- Sets `isWorking = true`
- Rate limit: 1 report per channel per device per minute

#### `POST /api/v1/channels/health-sync`

```json
{
  "deviceId": "string",
  "results": [
    { "channelId": "string", "status": "alive|dead|unresponsive", "responseTimeMs": 123, "timestamp": "ISO8601" }
  ]
}
```

- Bulk updates counters for each channel
- Uses `bulkWrite` for efficiency

### 3. Update Server-Side `stream-prober` to Increment Counters

When `stream-prober.ts` checks a stream during scheduled liveness checks:
- If result is `alive`: increment `metrics.aliveCount`, update `lastAliveAt`, set `isWorking = true`
- If result is `dead`: increment `metrics.deadCount`, update `lastDeadAt`, set `isWorking = false`
- Keep existing `lastTested` and `responseTime` behavior

### 4. Enhanced Admin Stats

Update `GET /admin/stats/stream-health` to include:
- Streams ranked by `deadCount` (most unreliable)
- Streams ranked by `playCount` (most popular)
- Streams with high `deadCount` but zero `playCount` (candidates for removal)
- Streams with high `unresponsiveCount` (partial failures)
- Ratio analysis: `deadCount / (deadCount + aliveCount)` = failure rate

### 5. Favorites Sync Enhancement

- Ensure `GET /api/v1/favorites` returns full list with timestamps for conflict resolution
- `POST /api/v1/favorites` should accept `lastModified` timestamp for merge logic

## Files to Modify

| File | Change |
|------|--------|
| `backend/src/models/Channel.ts` | Add `metrics` subdocument to schema |
| `backend/src/routes/channels.js` | Add `report-status`, `report-play`, `health-sync` routes |
| `backend/src/services/stream-prober.ts` | Increment counters on probe results |
| `backend/src/services/task-registry.ts` | Update liveness task to use new counter logic |
| `backend/src/routes/admin.js` | Enhance `stream-health` stats with counter analytics |
| `backend/src/routes/favorites.js` | Add timestamp-based merge for bidirectional sync |

## Acceptance Criteria

- [ ] `Channel` schema has `metrics.deadCount`, `aliveCount`, `unresponsiveCount`, `playCount` fields
- [ ] `POST /api/v1/channels/:id/report-status` increments correct counter and updates `isWorking`
- [ ] `POST /api/v1/channels/:id/report-play` increments `playCount` and `aliveCount`
- [ ] `POST /api/v1/channels/health-sync` bulk-updates counters from client health scanner
- [ ] Server `stream-prober` increments counters during scheduled liveness checks
- [ ] Rate limiting prevents counter abuse from clients
- [ ] Admin stats dashboard shows counter-based analytics
- [ ] All endpoints validate input and return proper error codes

## Related

- Depends on corresponding client issue in `akshaynikhare/FireVisionIPTV`
