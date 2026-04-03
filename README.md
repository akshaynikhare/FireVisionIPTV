# FireVision IPTV

[![Release](https://img.shields.io/github/v/release/akshaynikhare/FireVisionIPTV)](https://github.com/akshaynikhare/FireVisionIPTV/releases/latest)
[![Build](https://github.com/akshaynikhare/FireVisionIPTV/actions/workflows/release.yml/badge.svg)](https://github.com/akshaynikhare/FireVisionIPTV/actions/workflows/release.yml)
[![Platform](https://img.shields.io/badge/platform-Fire%20TV%20%7C%20Android%20TV-orange)](https://github.com/akshaynikhare/FireVisionIPTV)
[![License](https://img.shields.io/github/license/akshaynikhare/FireVisionIPTV)](LICENSE)

IPTV streaming app for Amazon Fire TV and Android TV. Watch live channels with server-synced lists, category browsing, favorites, and background health scanning.

## Preview

<img src="/preview/preview.gif" alt="Preview GIF" width="800px">

<img src="/preview/preview1.jpg" alt="Preview 1" width="200" height="150"> <img src="/preview/preview2.jpg" alt="Preview 2" width="200" height="150"> <img src="/preview/preview3.jpg" alt="Preview 3" width="200" height="150"> <img src="/preview/preview4.jpg" alt="Preview 4" width="200" height="150">

## Features

- **Live Streaming** — HLS playback via Media3 ExoPlayer with position save/resume
- **Server Sync** — Channel lists, favorites, and health status synced with the backend
- **Smart Browsing** — Browse by category/language, full-text search with filters
- **Health Scanning** — Background channel health checks with online/offline indicators
- **Device Pairing** — PIN-based pairing with QR code support
- **In-App Updates** — OTA updates with APK download and install
- **TV Optimized** — D-pad navigation built with Jetpack Compose for TV

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

## Documentation

Detailed docs available in [`docs/`](docs/):

- [Architecture](docs/ARCHITECTURE.md) — Layers, data flow, dependency graph
- [Data Layer](docs/modules/data.md) — Database, DAOs, repositories, API
- [Domain Layer](docs/modules/domain.md) — Use cases, models, interfaces
- [Presentation Layer](docs/modules/presentation.md) — Screens, ViewModels, navigation
- [API Reference](docs/API.md) — Endpoints, schemas, contracts
- [Setup Guide](docs/SETUP.md) — Dev environment and deployment

## License

[MIT](LICENSE)

## Disclaimer

This application provides access to free IPTV streams. Ensure you comply with all applicable laws and regulations in your jurisdiction.
