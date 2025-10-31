# Build Fix - Java Version Requirement

## Issue

The Android Gradle Plugin and Gradle 8.6 require **Java 11 or higher**, but your system currently has Java 8 installed.

## Solution Options

### Option 1: Install Java 11+ (Recommended)

**Using Homebrew:**
```bash
# Install Java 17 (LTS version)
brew install openjdk@17

# Link it
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Verify
java -version
```

**Or download manually:**
1. Download Java 17 from: https://adoptium.net/
2. Install the DMG file
3. Verify: `java -version`

**Then rebuild:**
```bash
cd /Users/akshay/Documents/WorkDock/_CADnative/fieretv/FireVisionIPTV
./gradlew clean
./gradlew assembleDebug
```

### Option 2: Use Android Studio (Easiest)

Android Studio comes with a bundled JDK:

1. Open project in Android Studio
2. Studio will automatically use its bundled JDK
3. Build → Build APK(s)

### Option 3: Downgrade Gradle and AGP (Not Recommended)

If you must stick with Java 8, downgrade to older versions:

**Edit `gradle/wrapper/gradle-wrapper.properties`:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-7.5-bin.zip
```

**Edit `gradle/libs.versions.toml`:**
```toml
[versions]
agp = "7.2.2"
```

**Edit `build.gradle`:**
```gradle
id 'com.google.gms.google-services' version '4.3.10' apply false
```

**Then:**
```bash
./gradlew clean
./gradlew assembleDebug
```

## Quick Fix Applied

I've already downgraded the AGP version to 7.4.2 and Google Services to 4.3.15, but you still need to either:
- Install Java 11+, OR
- Use Android Studio, OR
- Further downgrade Gradle wrapper

## Current Versions After My Changes

- Android Gradle Plugin: 7.4.2 (was 8.4.1)
- Google Services: 4.3.15 (was 4.4.2)
- Gradle Wrapper: Still 8.6 (needs Java 11)

## Recommended: Install Java 17

This is the cleanest solution:

```bash
brew install openjdk@17
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Restart terminal
java -version

# Should show: openjdk version "17.x.x"

# Now build
cd /Users/akshay/Documents/WorkDock/_CADnative/fieretv/FireVisionIPTV
./gradlew clean
./gradlew assembleDebug
```

## Verification

After fixing, you should see:
```bash
BUILD SUCCESSFUL in Xs
```

And the APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## If You Get Other Errors

If you see errors related to the new code (ApiClient, UpdateManager), please let me know and I'll fix them immediately.

## For Production Build

Once the build works:
```bash
./gradlew assembleRelease
```

Output will be at:
```
app/build/outputs/apk/release/app-release.apk
```
