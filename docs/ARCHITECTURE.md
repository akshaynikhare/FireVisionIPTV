# FireVision IPTV Android App - Architecture

## Overview

This document describes the architecture of the FireVision IPTV Android application for Fire TV devices.

## Client Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                                 │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │             Android App (Fire TV Device)                      │  │
│  │                                                                │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐   │  │
│  │  │ MainActivity│  │ MainFragment │  │ PlaybackActivity  │   │  │
│  │  │             │  │              │  │                   │   │  │
│  │  │ - Init      │  │ - Browse UI  │  │ - Video Player    │   │  │
│  │  │ - Update    │  │ - Load       │  │ - Channel         │   │  │
│  │  │   Check     │  │   Channels   │  │   Navigation      │   │  │
│  │  └──────┬──────┘  └──────┬───────┘  └───────────────────┘   │  │
│  │         │                │                                    │  │
│  │         └────────┬───────┘                                    │  │
│  │                  │                                            │  │
│  │         ┌────────▼────────┐                                  │  │
│  │         │   MovieList     │                                  │  │
│  │         │                 │                                  │  │
│  │         │ - Channel Mgmt  │                                  │  │
│  │         │ - Server Sync   │                                  │  │
│  │         └────────┬────────┘                                  │  │
│  │                  │                                            │  │
│  │         ┌────────▼────────┐     ┌─────────────────────┐     │  │
│  │         │   ApiClient     │     │  UpdateManager      │     │  │
│  │         │                 │     │                     │     │  │
│  │         │ - HTTP Client   │     │ - Update Check      │     │  │
│  │         │ - Channel Fetch │     │ - APK Download      │     │  │
│  │         │ - Version Check │     │ - Install Prompt    │     │  │
│  │         └────────┬────────┘     └──────────┬──────────┘     │  │
│  │                  │                         │                 │  │
│  └──────────────────┼─────────────────────────┼────────────────┘  │
│                     │                         │                    │
└─────────────────────┼─────────────────────────┼────────────────────┘
                      │                         │
                      │    HTTPS/TLS            │
                      │                         │
                      ▼                         ▼
               Server API Endpoints
```

## Technology Stack

### Client (Android)
- **Language**: Java
- **Framework**: AndroidX, Leanback (TV UI)
- **Video Player**: ExoPlayer (via VideoSupportFragment)
- **Image Loading**: Glide
- **Database**: Realm (optional, unused currently)
- **Backend**: Firebase (Firestore, Realtime DB)

## App Structure

```
FireVisionIPTV/app/src/main/java/com/cadnative/firevisioniptv/
├── api/
│   └── ApiClient.java              # Server communication
├── update/
│   └── UpdateManager.java          # Auto-update logic
├── models/
│   ├── Channel.java                # Channel model
│   ├── Movie.java                  # Display model
│   └── PairingRequest.java         # Pairing model
├── MainActivity.java               # Entry point
├── MainFragment.java               # Browse UI
├── PlaybackActivity.java           # Video player
└── MovieList.java                  # Channel list manager
```

## Data Flow

### 1. Channel Loading Flow

```
┌──────────────┐
│  App Starts  │
└──────┬───────┘
       │
       ▼
┌─────────────────────┐
│ MainActivity.onCreate│
│ - Init Firebase      │
│ - Init UpdateManager │
└─────────┬────────────┘
          │
          ▼
┌──────────────────────┐
│ Check for Updates    │
│ (Silent)             │
└──────────────────────┘
          │
          ▼
┌──────────────────────┐
│ Load MainFragment    │
└─────────┬────────────┘
          │
          ▼
┌──────────────────────────┐
│ MovieList.loadFromServer │
└─────────┬────────────────┘
          │
          ▼
┌─────────────────────────┐
│ ApiClient.fetchChannels │
└─────────┬───────────────┘
          │
          ├─── Success ──┐
          │              │
          │              ▼
          │         ┌──────────────────┐
          │         │ Parse JSON       │
          │         │ Create Movie List│
          │         │ Display in UI    │
          │         └──────────────────┘
          │
          └─── Error ────┐
                         │
                         ▼
                    ┌──────────────────┐
                    │ Fallback to      │
                    │ local playlist.m3u│
                    │ Display in UI    │
                    └──────────────────┘
```

### 2. Auto-Update Flow

```
┌──────────────┐
│  App Launch  │
└──────┬───────┘
       │
       ▼
┌────────────────────────┐
│ UpdateManager.check()  │
│ currentVersion = 1     │
└──────┬─────────────────┘
       │
       ▼
┌────────────────────────────┐
│ GET /api/v1/app/version    │
│ ?currentVersion=1          │
└──────┬─────────────────────┘
       │
       ▼
┌────────────────────────────┐
│ Server Response:           │
│ {                          │
│   updateAvailable: true,   │
│   latestVersion: {         │
│     versionCode: 2,        │
│     downloadUrl: "...",    │
│     isMandatory: false     │
│   }                        │
│ }                          │
└──────┬─────────────────────┘
       │
       ├─── Update Available ───┐
       │                        │
       │                        ▼
       │               ┌──────────────────┐
       │               │ Show Dialog      │
       │               │ "Update Available"│
       │               └──────┬───────────┘
       │                      │
       │                      ▼
       │               ┌──────────────────┐
       │               │ User Clicks      │
       │               │ "Update Now"     │
       │               └──────┬───────────┘
       │                      │
       │                      ▼
       │               ┌──────────────────┐
       │               │ DownloadManager  │
       │               │ Downloads APK    │
       │               └──────┬───────────┘
       │                      │
       │                      ▼
       │               ┌──────────────────┐
       │               │ Download Complete│
       │               │ Show Install     │
       │               │ Prompt           │
       │               └──────────────────┘
       │
       └─── No Update ──────┐
                            │
                            ▼
                   ┌──────────────────┐
                   │ Continue Normally│
                   │ (Silent)         │
                   └──────────────────┘
```

### 3. Video Playback Flow

```
┌──────────────────┐
│ User Selects     │
│ Channel          │
└────────┬─────────┘
         │
         ▼
┌──────────────────────┐
│ PlaybackActivity     │
│ launched with        │
│ channel URL          │
└────────┬─────────────┘
         │
         ▼
┌──────────────────────┐
│ ExoPlayer           │
│ - Load stream URL    │
│ - Handle DRM (if any)│
│ - Start playback     │
└──────────────────────┘
```

## Component Details

### MainActivity
- **Purpose**: Entry point, initialization
- **Responsibilities**:
  - Initialize Firebase
  - Start UpdateManager
  - Load MainFragment
  - Handle app lifecycle

### MainFragment
- **Purpose**: Browse UI using Leanback library
- **Responsibilities**:
  - Display channel categories
  - Handle navigation
  - Search functionality
  - Launch PlaybackActivity

### MovieList
- **Purpose**: Channel data management
- **Responsibilities**:
  - Fetch channels from server
  - Parse M3U fallback
  - Cache channel list
  - Provide data to UI

### ApiClient
- **Purpose**: HTTP communication
- **Responsibilities**:
  - Fetch channel list from server
  - Check for app updates
  - Handle network errors
  - Parse JSON responses

### UpdateManager
- **Purpose**: App update management
- **Responsibilities**:
  - Check server for updates
  - Download APK files
  - Show update dialogs
  - Trigger installation

### PlaybackActivity
- **Purpose**: Video playback
- **Responsibilities**:
  - Initialize ExoPlayer
  - Handle stream URLs
  - DRM support
  - Playback controls

## Storage

### Local Assets
```
app/src/main/assets/
└── playlist.m3u          # Fallback channel list
```

### Shared Preferences
- Last update check timestamp
- Current app version
- User preferences

### Cache
- Channel list cache
- Image cache (Glide)

## Network Communication

### Endpoints Used
| Endpoint | Purpose | Frequency |
|----------|---------|-----------|
| `/api/v1/channels` | Fetch channel list | On app start, refresh |
| `/api/v1/app/version` | Check for updates | On app start |
| `/api/v1/app/download` | Download APK | User initiated |

### Error Handling
1. **Network Unavailable**: Fall back to local M3U
2. **Server Error**: Show error, retry option
3. **Update Download Failure**: Show error, allow retry

## Fire TV Optimization

### UI Considerations
- D-Pad navigation support
- Focus management
- Leanback library for TV UI
- Large touch targets

### Performance
- Image loading optimization (Glide)
- Lazy loading of channels
- Efficient list rendering
- Memory management

### Remote Control
- Back button handling
- Home button handling
- Play/Pause controls
- Search button integration

---

**Last Updated**: 2025-01-01
**Version**: 1.0.0
