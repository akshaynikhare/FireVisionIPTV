# FireVision IPTV

[![Release](https://img.shields.io/github/v/release/akshaynikhare/FireVisionIPTV)](https://github.com/akshaynikhare/FireVisionIPTV/releases/latest)
[![Build](https://github.com/akshaynikhare/FireVisionIPTV/actions/workflows/release.yml/badge.svg)](https://github.com/akshaynikhare/FireVisionIPTV/actions/workflows/release.yml)
[![Platform](https://img.shields.io/badge/platform-Fire%20TV%20%7C%20Android%20TV-orange)](https://github.com/akshaynikhare/FireVisionIPTV)
[![License](https://img.shields.io/github/license/akshaynikhare/FireVisionIPTV)](LICENSE)
[![Min SDK](https://img.shields.io/badge/min%20SDK-28%20(Android%209)-green)](https://github.com/akshaynikhare/FireVisionIPTV)

**Open-source IPTV player for Amazon Fire TV and Android TV.** Stream live channels with server-synced lists, category browsing, favorites, and background health scanning.

Works standalone with any M3U source, or pairs with [FireVision IPTV Server](https://github.com/akshaynikhare/FireVisionIPTVServer) for full channel management, EPG, and OTA updates.

---

## Preview

<img src="/preview/preview.gif" alt="FireVision IPTV Demo" width="800px">

<img src="/preview/preview1.jpg" alt="Home" width="200" height="150"> <img src="/preview/preview2.jpg" alt="Channel Browser" width="200" height="150"> <img src="/preview/preview3.jpg" alt="Player" width="200" height="150"> <img src="/preview/preview4.jpg" alt="Settings" width="200" height="150">

---

## Features

- **Live Streaming** — HLS playback via Media3 ExoPlayer with position save and resume
- **Server Sync** — Channel lists, favorites, and health status synced from [FireVision IPTV Server](https://github.com/akshaynikhare/FireVisionIPTVServer)
- **Smart Browsing** — Browse by category and language, full-text search with history
- **Health Scanning** — Background channel health checks with online/offline indicators
- **Device Pairing** — PIN-based pairing with QR code — links to your self-hosted server
- **In-App Updates** — OTA updates with APK download and install prompt
- **TV Optimized** — D-pad navigation built with Jetpack Compose for TV, no touch required

---

## Download

Get the latest APK from [GitHub Releases](https://github.com/akshaynikhare/FireVisionIPTV/releases/latest).

**Install on Fire TV:**
1. Enable _Apps from Unknown Sources_ in Fire TV Settings → My Fire TV → Developer Options
2. Install [Downloader by AFTVnews](https://www.amazon.com/dp/B01N0BP507) from the Amazon Appstore
3. Enter the APK URL from the latest release and install

---

## Getting Started

### Standalone (M3U only)
1. Install the APK on your Fire TV or Android TV device
2. Open Settings → Add Source → enter your M3U playlist URL
3. Browse and stream channels

### With FireVision IPTV Server
1. Install the APK
2. Open Settings → Server Pairing
3. Scan the QR code from the admin panel, or enter the PIN manually
4. Channel list, favorites, and EPG sync automatically

See the [TV Pairing System docs](https://github.com/akshaynikhare/FireVisionIPTVServer/blob/main/docs/workflow/TV_PAIRING_SYSTEM.md) for the full pairing flow.

---

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose for TV |
| **Architecture** | Clean Architecture + MVVM |
| **DI** | Hilt |
| **Database** | Room |
| **Networking** | Retrofit + OkHttp |
| **Player** | Media3 ExoPlayer (HLS) |
| **Background** | WorkManager |
| **Min SDK** | 28 (Android 9) |

---

## Backend

This app is designed to pair with **[FireVision IPTV Server](https://github.com/akshaynikhare/FireVisionIPTVServer)** — a self-hosted Node.js/Docker server for managing channels, users, EPG, and devices from an admin panel.

---

## Documentation

| Doc | Description |
|---|---|
| [Architecture](docs/ARCHITECTURE.md) | Layers, data flow, dependency graph |
| [Data Layer](docs/modules/data.md) | Database, DAOs, repositories, API |
| [Domain Layer](docs/modules/domain.md) | Use cases, models, interfaces |
| [Presentation Layer](docs/modules/presentation.md) | Screens, ViewModels, navigation |
| [API Reference](docs/API.md) | Endpoints, schemas, contracts |
| [Setup Guide](docs/SETUP.md) | Dev environment and build instructions |

---

## Contributing

Issues and PRs are welcome. See the [Setup Guide](docs/SETUP.md) to get the dev environment running.

## License

[MIT](LICENSE)

## Disclaimer

This application streams IPTV content from sources you configure. Ensure you have the right to access any streams you use and comply with applicable laws in your jurisdiction.
