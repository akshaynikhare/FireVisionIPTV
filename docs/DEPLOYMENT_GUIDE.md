# FireVision IPTV Android App - Deployment Guide

This guide covers building, testing, and deploying the FireVision IPTV Android application for Fire TV devices.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Development Setup](#development-setup)
3. [Configuration](#configuration)
4. [Building](#building)
5. [Testing](#testing)
6. [Distribution](#distribution)
7. [Update Management](#update-management)

---

## Prerequisites

### Required Software
- **Android Studio**: Arctic Fox or later
- **JDK**: Java 8 or higher
- **Gradle**: 7.0+ (included with Android Studio)
- **Git**: For version control

### Fire TV Device Requirements
- **Fire TV Device**: Fire TV Stick 4K, Fire TV Cube, or newer
- **Android Version**: Android 9.0 (API 28) or higher
- **ADB Debugging**: Enabled for testing

---

## Development Setup

### Step 1: Clone the Repository

```bash
git clone <your-repository-url>
cd FireVision__IPTV/FireVisionIPTV
```

### Step 2: Open in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to `FireVision__IPTV/FireVisionIPTV`
4. Click "OK"
5. Wait for Gradle sync to complete

### Step 3: Verify Project Configuration

Check [build.gradle](app/build.gradle):

```gradle
android {
    compileSdk 34

    defaultConfig {
        applicationId "com.cadnative.firevisioniptv"
        minSdk 28        // Fire TV minimum
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
}
```

---

## Configuration

### Step 1: Update Server URL

Open [ApiClient.java](app/src/main/java/com/cadnative/firevisioniptv/api/ApiClient.java):

```java
// Line 16 - Update this:
private static final String BASE_URL = "https://tv.cadnative.com";
```

Replace `tv.cadnative.com` with your server domain.

### Step 2: Configure Firebase (Optional)

If using Firebase features:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create/select your project
3. Download `google-services.json`
4. Place in `app/` directory

### Step 3: Update App Version

Before each release, update version in [build.gradle](app/build.gradle):

```gradle
defaultConfig {
    versionCode 1        // Increment for each release
    versionName "1.0"    // Semantic version
}
```

**Important**:
- `versionCode` must increase for updates to work
- `versionName` is displayed to users

---

## Building

### Debug Build (Development)

**Using Android Studio:**
1. Build > Build APK(s)
2. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

**Using Command Line:**
```bash
cd FireVisionIPTV

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Production)

**Step 1: Generate Signing Key (First time only)**

```bash
keytool -genkey -v -keystore firevision-release-key.jks \
  -alias firevision -keyalg RSA -keysize 2048 -validity 10000

# You'll be prompted for:
# - Keystore password
# - Key password
# - Name, Organization, etc.
```

**Step 2: Configure Signing**

Create `keystore.properties` in project root:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=firevision
storeFile=../firevision-release-key.jks
```

**Important**: Never commit `keystore.properties` to git!

**Step 3: Build Release APK**

```bash
# Using Android Studio:
Build > Generate Signed Bundle/APK > APK > Next
# Select keystore and enter passwords

# Using Command Line:
./gradlew assembleRelease

# APK location:
# app/build/outputs/apk/release/app-release.apk
```

---

## Testing

### Test on Emulator

1. In Android Studio: Tools > Device Manager
2. Create New Device
3. Select TV device profile (e.g., "Android TV (1080p)")
4. Download system image (API 28+)
5. Run the app: Run > Run 'app'

### Test on Physical Fire TV Device

**Step 1: Enable ADB on Fire TV**

1. On Fire TV: Settings > My Fire TV > Developer Options
2. Enable "ADB Debugging"
3. Enable "Apps from Unknown Sources"

**Step 2: Find Fire TV IP Address**

1. Settings > My Fire TV > About > Network
2. Note the IP address (e.g., 192.168.1.100)

**Step 3: Connect via ADB**

```bash
# Connect to Fire TV
adb connect FIRE_TV_IP:5555

# Verify connection
adb devices
# Should show: FIRE_TV_IP:5555    device

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or for release:
adb install app/build/outputs/apk/release/app-release.apk
```

**Step 4: Launch and Test**

```bash
# Launch app
adb shell am start -n com.cadnative.firevisioniptv/.MainActivity

# View logs
adb logcat | grep -i firevision
```

### Test Checklist

- [ ] App launches successfully
- [ ] Channels load from server
- [ ] Fallback to local M3U works (disconnect network)
- [ ] Channel playback works
- [ ] D-Pad navigation works
- [ ] Search functionality works
- [ ] Update check triggers on launch
- [ ] Update download and install works

---

## Distribution

### Method 1: Direct APK Distribution

**Upload to Your Server:**

```bash
# Build release APK
./gradlew assembleRelease

# Upload to server
curl -X POST https://tv.cadnative.com/api/v1/admin/app/upload \
  -H "X-API-Key: YOUR_API_KEY" \
  -F "apk=@app/build/outputs/apk/release/app-release.apk" \
  -F "versionName=1.0" \
  -F "versionCode=1" \
  -F "releaseNotes=Initial release" \
  -F "isMandatory=false"
```

**Share Download Link:**
```
https://tv.cadnative.com/api/v1/app/download
```

Users can download and sideload using:
- Downloader app on Fire TV
- ADB from computer

### Method 2: GitHub Releases

**Create Release:**

```bash
# Tag the release
git tag -a v1.0 -m "Release version 1.0"
git push origin v1.0
```

On GitHub:
1. Go to Releases
2. Create new release
3. Select the tag (v1.0)
4. Upload `app-release.apk`
5. Add release notes
6. Publish release

### Method 3: Amazon Appstore (Optional)

For wider distribution:

1. Sign up for [Amazon Developer Account](https://developer.amazon.com/)
2. Create new app in Developer Console
3. Upload APK
4. Complete app listing
5. Submit for review

---

## Update Management

### Releasing Updates

**Step 1: Update Version**

Edit [build.gradle](app/build.gradle):

```gradle
defaultConfig {
    versionCode 2         // Increment from previous (was 1)
    versionName "1.1"     // Update version string
}
```

**Step 2: Build New APK**

```bash
./gradlew clean
./gradlew assembleRelease
```

**Step 3: Upload to Server**

```bash
curl -X POST https://tv.cadnative.com/api/v1/admin/app/upload \
  -H "X-API-Key: YOUR_API_KEY" \
  -F "apk=@app/build/outputs/apk/release/app-release.apk" \
  -F "versionName=1.1" \
  -F "versionCode=2" \
  -F "releaseNotes=What's new:\n- Bug fixes\n- Performance improvements" \
  -F "isMandatory=false"
```

**Step 4: Users Get Notified**

On next app launch, users will see:
- Update available dialog
- Release notes
- Option to download and install

### Update Types

**Optional Update:**
```bash
-F "isMandatory=false"
```
Users can choose to skip the update.

**Mandatory Update:**
```bash
-F "isMandatory=true"
```
Users must update to continue using the app.

### Version Compatibility

```bash
-F "minCompatibleVersion=1"
```
Specifies minimum version that can update to this version.

---

## Troubleshooting

### Build Errors

**Gradle Sync Failed:**
```bash
# Clean and rebuild
./gradlew clean
# In Android Studio: File > Invalidate Caches > Invalidate and Restart
```

**Missing Dependencies:**
```bash
# Check build.gradle for correct repositories
# Sync gradle files
./gradlew --refresh-dependencies
```

### Installation Errors

**INSTALL_FAILED_UPDATE_INCOMPATIBLE:**
```bash
# Uninstall old version first
adb uninstall com.cadnative.firevisioniptv

# Then install new version
adb install app-release.apk
```

**Signature Mismatch:**
- This happens when debug and release builds have different signatures
- Uninstall existing app first

### Runtime Errors

**Channels Not Loading:**
1. Check server URL in [ApiClient.java](app/src/main/java/com/cadnative/firevisioniptv/api/ApiClient.java):16
2. Verify server is accessible: `curl https://tv.cadnative.com/api/v1/channels`
3. Check device internet connection
4. View logs: `adb logcat | grep ApiClient`

**Update Not Working:**
1. Verify APK uploaded to server
2. Check version codes (new must be > old)
3. View logs: `adb logcat | grep UpdateManager`

---

## Build Variants

### Debug vs Release

**Debug Build:**
- Debuggable
- Unoptimized
- Unsigned or debug-signed
- For development only

**Release Build:**
- Not debuggable
- Optimized (ProGuard/R8)
- Signed with release key
- For distribution

### Creating Build Variants

In [build.gradle](app/build.gradle):

```gradle
buildTypes {
    release {
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
    debug {
        applicationIdSuffix ".debug"
        versionNameSuffix "-DEBUG"
    }
}
```

---

## Continuous Integration (Optional)

### GitHub Actions Example

Create `.github/workflows/build.yml`:

```yaml
name: Build APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Build Release APK
        run: |
          cd FireVisionIPTV
          ./gradlew assembleRelease
      - name: Upload to Release
        uses: actions/upload-release-asset@v1
        with:
          upload_url: ${{ github.event.release.upload_url }}
          asset_path: ./FireVisionIPTV/app/build/outputs/apk/release/app-release.apk
          asset_name: FireVisionIPTV.apk
```

---

## Best Practices

1. **Version Control**
   - Always increment `versionCode` for releases
   - Use semantic versioning for `versionName`
   - Tag releases in git

2. **Testing**
   - Test on real Fire TV device before release
   - Test update flow with previous version
   - Verify offline fallback works

3. **Security**
   - Never commit keystore files
   - Never commit `keystore.properties`
   - Use environment variables for sensitive data

4. **Release Notes**
   - Always include meaningful release notes
   - List new features and bug fixes
   - Keep it concise

---

## Support

For issues:
- Check logs: `adb logcat`
- Verify server connectivity
- Contact: support@cadnative.com
