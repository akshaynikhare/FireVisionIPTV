# Health Scanner Lifecycle

## Overview

The `ChannelHealthScanner` checks stream URLs to determine if channels are online/offline. It runs as a singleton background service, started from `ComposeMainActivity` after pairing.

## Lifecycle Phases

```mermaid
flowchart TD
    A[App Launch — paired] --> B[startAutoScan]
    B --> C[1 min startup delay]
    C --> D[Phase 1: Health Scan\nrunFullScan]

    D --> D1[Get channels by priority\nstalest first]
    D1 --> D2[Batch check — 4 concurrent\nMark CHECKING → probe → upsert result]
    D2 --> D3[cleanupOrphaned\nremove entries for deleted channels]
    D3 --> D4[Bulk sync results to server]

    D4 --> E[5 min delay]
    E --> F[Phase 2: Thumbnail Extraction\nONLINE channels only]
    F --> G[30 min cooldown]
    G --> D
```

## Overlapping Scan Strategy

Old health statuses are **never deleted before a new scan**. The scan updates results incrementally per-batch:

```mermaid
stateDiagram-v2
    state "Before Scan" as before {
        state "A: ONLINE (30m ago)" as a1
        state "B: OFFLINE (30m ago)" as b1
        state "C: ONLINE (30m ago)" as c1
    }

    state "During Scan — Batch 1 (A, B)" as during {
        state "A: CHECKING → ONLINE (fresh)" as a2
        state "B: CHECKING → OFFLINE (fresh)" as b2
        state "C: ONLINE (still old — not cleared)" as c2
    }

    state "After Full Scan" as after {
        state "A: ONLINE (fresh)" as a3
        state "B: OFFLINE (fresh)" as b3
        state "C: ONLINE (fresh)" as c3
    }

    state "Post-Scan Cleanup" as cleanup {
        state "cleanupOrphaned()\nRemove entries for deleted channels" as clean
    }

    before --> during : scan starts
    during --> after : all batches done
    after --> cleanup : scan complete
```

Key: `cleanupOrphaned()` runs AFTER the full scan, not before. Users always see the previous status until a new result replaces it.

## Data Independence

```
channel_health (Room table — NO foreign key to channels)
├── channelId: String (PK)
├── status: ONLINE | OFFLINE | CHECKING | UNRESPONSIVE | UNKNOWN
├── lastCheckedAt: Long
├── responseTimeMs: Long?
├── errorMessage: String?
└── thumbnailPath: String?
```

- No FK CASCADE — channel sync never deletes health data
- `@Upsert` on channels prevents DELETE+INSERT cascade
- `upsertPreservingThumbnail()` updates health without losing thumbnail path

## Scan Priority

Channels are scanned in order of `lastCheckedAt ASC` — stalest first:

```sql
SELECT c.id FROM channels c
LEFT JOIN channel_health h ON c.id = h.channelId
WHERE c.isActive = 1
ORDER BY COALESCE(h.lastCheckedAt, 0) ASC
```

New channels (no health entry) get priority (`COALESCE` returns 0).

## Batch Processing

- Batch size: 4 channels concurrent
- Per batch:
  1. Mark all as CHECKING (preserves thumbnail)
  2. Fetch stream URLs from channels table
  3. `async` check each stream (HLS validation or HEAD/GET)
  4. Upsert results (preserves thumbnail)

## Stream Check Logic

```mermaid
flowchart TD
    A[Stream URL] --> B{Valid URL?}
    B -->|No| C[OFFLINE\nInvalid URL]
    B -->|Yes| D{HLS .m3u8?}

    D -->|Yes| E[GET request]
    E --> F{HTTP success?}
    F -->|No| G[OFFLINE\nHTTP error code]
    F -->|Yes| H{Contains #EXTM3U\nor #EXT-X-STREAM-INF?}
    H -->|Yes| I[ONLINE]
    H -->|No| J[OFFLINE\nInvalid HLS manifest]

    D -->|No| K[HEAD request]
    K -->|Success| L[ONLINE]
    K -->|Fail| M[GET with Range: bytes=0-1023]
    M --> N{Success or 206?}
    N -->|Yes| O[ONLINE]
    N -->|No| P[OFFLINE]
```

Timeouts: 6s connect, 6s read.

## Manual Scan

`triggerManualScan()` cancels any running scan and starts fresh:
1. Runs `runFullScan()` immediately
2. Then resumes the auto cycle (thumbnail extraction → cooldown → repeat)

## Integration Points

- `PlayerViewModel.onStreamDead()` → writes OFFLINE directly via `channelHealthDao.upsertPreservingThumbnail()`
- `PlayerViewModel.onStreamUnresponsive()` → writes UNRESPONSIVE
- `SettingsScreen` → triggers `triggerManualScan()`, shows `scanProgress`
- `ChannelCard` → reads health via `ChannelUiModel.healthStatus` (HealthIndicatorDot)
- Server sync: bulk POST health results after scan completes
