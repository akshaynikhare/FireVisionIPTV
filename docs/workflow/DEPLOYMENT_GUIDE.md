# Deployment Guide

Build, test, and distribute the FireVision IPTV Android app for Fire TV.

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Android Studio | Arctic Fox+ |
| JDK | 8+ |
| Min SDK | 28 (Android 9) |
| Target SDK | 34 |
| Fire TV | Stick 4K, Cube, or newer |

## Configuration

### API Base URL

In `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://tv.cadnative.com/\"")
```

### Firebase

Download `google-services.json` from Firebase Console and place in `app/`.

### Version Bumping

Increment before each release:

```kotlin
defaultConfig {
    versionCode 2         // Must increase for updates to work
    versionName "1.1"
}
```

## Building

```bash
make debug       # Debug APK → app/build/outputs/apk/debug/app-debug.apk
make release     # Release APK → app/build/outputs/apk/release/app-release.apk
```

### Release Signing

Set environment variables for signing (or create `keystore.properties`):

```bash
export SIGNING_KEY_STORE=path/to/keystore.jks
export SIGNING_KEY_ALIAS=firevision
export SIGNING_STORE_PASSWORD=...
export SIGNING_KEY_PASSWORD=...
```

### Build Variants

| Variant | Suffix | Debuggable | Optimized |
|---------|--------|------------|-----------|
| debug | `.debug` | Yes | No |
| dev | `.dev` | Yes | No |
| release | — | No | Yes (R8) |

## Testing on Fire TV

```bash
# Enable ADB: Settings > My Fire TV > Developer Options > ADB Debugging
adb connect <FIRE_TV_IP>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.cadnative.firevisioniptv/.ComposeMainActivity
make logcat      # View filtered logs
```

### Test Checklist

- [ ] App launches, channels load from server
- [ ] D-pad navigation works on all screens
- [ ] Channel playback (HLS streams)
- [ ] PIN-based pairing flow
- [ ] Favorites: add, remove, sync
- [ ] Search with results
- [ ] Health scanner runs and updates status indicators
- [ ] Offline mode (disconnect network, channels still visible from cache)
- [ ] Update check + download + install
- [ ] Back button behavior (overlay dismiss → exit flow)

## Distribution

### GitHub Releases (primary)

```bash
git tag -a v1.1 -m "Release 1.1"
git push origin v1.1
```

Upload `app-release.apk` as a release asset. Server auto-serves it via `GET /api/v1/app/version`.

### Direct Upload

```bash
curl -X POST https://tv.cadnative.com/api/v1/admin/app/upload \
  -H "X-API-Key: <API_KEY>" \
  -F "apk=@app/build/outputs/apk/release/app-release.apk" \
  -F "versionName=1.1" \
  -F "versionCode=2" \
  -F "releaseNotes=Bug fixes" \
  -F "isMandatory=false"
```

Download URL: `https://tv.cadnative.com/api/v1/app/download`

### Update Types

| Flag | Behavior |
|------|----------|
| `isMandatory=false` | User can skip update |
| `isMandatory=true` | Must update to continue |
| `minCompatibleVersion=1` | Minimum version that can update |

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | `adb uninstall com.cadnative.firevisioniptv` then reinstall |
| Signature mismatch | Uninstall existing app (debug vs release signing differs) |
| Channels not loading | Verify API URL in build config, check server: `curl https://tv.cadnative.com/health` |
| Update not working | Verify `versionCode` is higher than installed version |
