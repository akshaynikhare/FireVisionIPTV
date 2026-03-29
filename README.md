# IPTV Player — Android TV & Fire TV

Open-source IPTV media player for Android TV and Amazon Fire TV devices.
Supports HLS, DASH, RTSP, and plain-HTTP streams from M3U/M3U8 playlists, with XMLTV EPG integration.

## Module Layout

| Module     | Purpose |
|------------|---------|
| `:app`     | Fire TV / Android TV launcher activity, navigation host |
| `:core`    | Shared domain models (`Channel`, `ChannelSource`) and OkHttp client factory |
| `:player`  | ExoPlayer / Media3 wrapper for IPTV stream playback |
| `:playlist`| M3U/M3U8 parser and channel source manager |
| `:epg`     | XMLTV EPG model and parser |

## Requirements

- **Android Studio** Hedgehog (2023.1) or newer
- **JDK 17**
- **Android SDK** with API level 22 (Android 5.1) through 34 build tools

## Build

```bash
# Clone the repository
git clone https://github.com/cabletech/iptv-player.git
cd iptv-player

# Assemble a debug APK
./gradlew :app:assembleDebug

# Run all unit tests
./gradlew test

# Run lint checks
./gradlew lint
```

The debug APK is output to `app/build/outputs/apk/debug/`.

## Minimum Supported Devices

| Platform       | Min OS Version |
|----------------|----------------|
| Amazon Fire TV | Fire OS 5 (Android 5.1, API 22) |
| Android TV     | Android 5.1 (API 22) |
| Android Phone  | Android 5.1 (API 22) — sideload supported |

## CI

GitHub Actions runs **lint → unit tests → assemble debug APK** on every push and pull request to `main` / `develop`.
See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## License

Apache 2.0 — see [LICENSE](LICENSE).
