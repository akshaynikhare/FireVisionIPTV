# FireVisionIPTV - Android App

FireVisionIPTV is a free IPTV application for Amazon Fire TV devices. It allows users to watch live TV channels from around the world with automatic updates and server-synced channel lists.

## Preview

<img src="/preview/preview.gif" alt="Preview GIF" width="800px">

<img src="/preview/preview1.jpg" alt="Preview 1" width="200" height="150"> <img src="/preview/preview2.jpg" alt="Preview 2" width="200" height="150"> <img src="/preview/preview3.jpg" alt="Preview 3" width="200" height="150"> <img src="/preview/preview4.jpg" alt="Preview 4" width="200" height="150">

## Features

### Core Features
- Access to hundreds of live TV channels from various countries
- Easy-to-use interface optimized for Fire TV
- Server-synced channel lists
- Automatic app updates
- Offline fallback support
- No paid subscription required

### Technical Features
- Dynamic channel loading from server
- Automatic update notifications
- One-click update installation
- Graceful offline fallback to local M3U
- Fire TV optimized Leanback UI
- Channel browsing by category
- Search functionality
- HLS/DASH streaming support

## Documentation

- **[Architecture](ARCHITECTURE.md)** - App architecture and component details
- **[Deployment Guide](DEPLOYMENT_GUIDE.md)** - Complete build and deployment instructions
- **[Main Project README](../README.md)** - Overall project documentation

## Requirements

### Development
- **Android Studio**: Arctic Fox or later
- **JDK**: Java 8 or higher
- **Gradle**: 7.0+ (included with Android Studio)

### Target Device
- **Fire TV Device**: Fire TV Stick 4K, Fire TV Cube, or newer
- **Android Version**: Android 9.0 (API 28) or higher

## Quick Start

### Installation (End Users)

**Method 1: Direct Download**
1. Download the latest APK from your server: `https://tv.cadnative.com/api/v1/app/download`
2. On Fire TV: Settings > My Fire TV > Developer options > Enable "Apps from Unknown Sources"
3. Use Downloader app to install the APK
4. Launch FireVisionIPTV

**Method 2: ADB Install**
```bash
# Enable ADB on Fire TV first
adb connect FIRE_TV_IP:5555
adb install FireVisionIPTV.apk
```

### Building from Source

**1. Clone the repository:**
```bash
git clone https://github.com/akshaynikhare/FireVisionIPTV.git
cd FireVisionIPTV
```

**2. Open in Android Studio:**
- Launch Android Studio
- Select "Open an Existing Project"
- Navigate to the FireVisionIPTV directory
- Wait for Gradle sync

**3. Configure Server URL:**

Edit [ApiClient.java](app/src/main/java/com/cadnative/firevisioniptv/api/ApiClient.java):
```java
// Line 16
private static final String BASE_URL = "https://tv.cadnative.com";
```

**4. Build:**

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

APK location:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
FireVisionIPTV/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cadnative/firevisioniptv/
│   │   │   │   ├── api/
│   │   │   │   │   └── ApiClient.java          # Server communication
│   │   │   │   ├── update/
│   │   │   │   │   └── UpdateManager.java      # Auto-update logic
│   │   │   │   ├── models/
│   │   │   │   │   ├── Channel.java            # Channel model
│   │   │   │   │   ├── Movie.java              # Display model
│   │   │   │   │   └── PairingRequest.java     # Pairing model
│   │   │   │   ├── MainActivity.java           # Entry point
│   │   │   │   ├── MainFragment.java           # Browse UI
│   │   │   │   ├── PlaybackActivity.java       # Video player
│   │   │   │   └── MovieList.java              # Channel manager
│   │   │   └── res/                            # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle                            # Build configuration
└── build.gradle                                # Project configuration
```

## Configuration

### Update Version

Before each release, update [build.gradle](app/build.gradle):

```gradle
android {
    defaultConfig {
        versionCode 2        // Increment for each release
        versionName "1.1"    // Semantic version
    }
}
```

**Important**: `versionCode` must increase for auto-updates to work!

### Configure Server URL

Update the server URL in [ApiClient.java](app/src/main/java/com/cadnative/firevisioniptv/api/ApiClient.java):

```java
private static final String BASE_URL = "https://tv.cadnative.com";
```

### Firebase (Optional)

If using Firebase features:
1. Download `google-services.json` from Firebase Console
2. Place in `app/` directory

## Testing

### Test on Fire TV Device

**1. Enable ADB on Fire TV:**
- Settings > My Fire TV > Developer Options
- Enable "ADB Debugging"
- Enable "Apps from Unknown Sources"

**2. Find Fire TV IP:**
- Settings > My Fire TV > About > Network

**3. Connect and Install:**
```bash
adb connect FIRE_TV_IP:5555
adb devices
adb install app/build/outputs/apk/debug/app-debug.apk
```

**4. View Logs:**
```bash
adb logcat | grep -i firevision
```

### Test Checklist
- [ ] App launches successfully
- [ ] Channels load from server
- [ ] Fallback to local M3U works (airplane mode test)
- [ ] Channel playback works
- [ ] D-Pad navigation works
- [ ] Search functionality works
- [ ] Update check triggers on launch
- [ ] Update download and install works

## Usage

1. Launch the FireVisionIPTV app on your Fire TV device
2. Browse available live TV channels by category
3. Use search to find specific channels
4. Select a channel to start playback
5. App automatically checks for updates on launch

## Release Process

### 1. Update Version

```gradle
// app/build.gradle
versionCode 2         // Increment
versionName "1.1"     // Update
```

### 2. Build Release APK

```bash
./gradlew clean
./gradlew assembleRelease
```

### 3. Upload to Server

```bash
curl -X POST https://tv.cadnative.com/api/v1/admin/app/upload \
  -H "X-API-Key: YOUR_API_KEY" \
  -F "apk=@app/build/outputs/apk/release/app-release.apk" \
  -F "versionName=1.1" \
  -F "versionCode=2" \
  -F "releaseNotes=Bug fixes and improvements"
```

### 4. Tag Release

```bash
git tag -a v1.1 -m "Release version 1.1"
git push origin v1.1
```

Users will be notified of the update on next app launch!

## Troubleshooting

### Build Issues

**Gradle Sync Failed:**
```bash
./gradlew clean
# In Android Studio: File > Invalidate Caches > Invalidate and Restart
```

**Dependencies Error:**
```bash
./gradlew --refresh-dependencies
```

### Installation Issues

**INSTALL_FAILED_UPDATE_INCOMPATIBLE:**
```bash
adb uninstall com.cadnative.firevisioniptv
adb install app-release.apk
```

### Runtime Issues

**Channels Not Loading:**
1. Check server URL in ApiClient.java
2. Verify server is running: `curl https://tv.cadnative.com/api/v1/channels`
3. Check device internet connection
4. View logs: `adb logcat | grep ApiClient`

**Update Not Working:**
1. Verify APK uploaded to server
2. Check version codes (new > old)
3. View logs: `adb logcat | grep UpdateManager`

**Video Playback Failed:**
1. Check stream URL validity
2. Test stream in VLC player
3. View logs: `adb logcat | grep ExoPlayer`

## API Integration

The app communicates with the server via these endpoints:

| Endpoint | Purpose |
|----------|---------|
| `/api/v1/channels` | Fetch channel list |
| `/api/v1/app/version?currentVersion=X` | Check for updates |
| `/api/v1/app/download` | Download APK |

See [Server API Documentation](../FireVisionIPTVServer/README.md) for details.

## Contributing

We welcome contributions! To contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make your changes
4. Test thoroughly on Fire TV device
5. Commit: `git commit -am 'Add new feature'`
6. Push: `git push origin feature/your-feature`
7. Submit a Pull Request

### Code Style
- Follow Android/Java conventions
- Use meaningful variable names
- Add comments for complex logic
- Test on actual Fire TV device

## License

This project is licensed under the [MIT License](LICENSE).

## Disclaimer

This application provides access to free IPTV streams, which may or may not be legal in your country. It is your responsibility to ensure that you comply with all applicable laws and regulations. The developers of FireVisionIPTV are not responsible for any legal issues that may arise from the use of this application.

## Support

- **Documentation**: See [ARCHITECTURE.md](ARCHITECTURE.md) and [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- **Issues**: Check logs with `adb logcat`
- **Server Issues**: See [Server Documentation](../FireVisionIPTVServer/README.md)
- **GitHub**: [Open an issue](https://github.com/akshaynikhare/FireVisionIPTV/issues)

## Credits

- Original app by [akshaynikhare](https://github.com/akshaynikhare)
- Channel list from [IPTV-Org](https://github.com/iptv-org/)
- Built with AndroidX Leanback
- Video playback via ExoPlayer

---

**Version**: 1.0.0
**Target**: Fire TV (Android 9.0+)
**Server**: https://tv.cadnative.com
