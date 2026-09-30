# SwiftLab Android Client - Build, Signing & Deployment Guide

This document describes how to build, test, sign, and install the native Android application for the SwiftLab Inventory & Billing Platform.

---

## 1. Project Architecture & Technology Stack

The Android client is located in [`/android/`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/android) and built using:
- **Language:** Kotlin 2.0.0
- **Build System:** Gradle 8.9 with Android Gradle Plugin 8.5.2 and KSP (Kotlin Symbol Processing)
- **UI Framework:** Jetpack Compose (Material3 Design System)
- **Local Database:** Room Database 2.6.1 (with SQLite offline caching)
- **Networking:** Retrofit 2.11.0, OkHttp 4.12.0, Gson
- **Background Synchronization:** Android WorkManager 2.9.1
- **Architecture Pattern:** Offline-First MVVM + Repository Pattern

---

## 2. Prerequisites & Environment Setup

### Required Tools:
1. **JDK 17 or JDK 21** (e.g. OpenJDK 21, Microsoft JDK 21).
2. **Android SDK:**
   - Platform: `android-36` (API Level 36) or `android-35`
   - Build Tools: `36.0.0`
3. **Android Studio** (Recommended: Android Studio Ladybug / Koala or newer)
   - Alternatively, CLI builds can use standalone Android Command Line Tools (`cmdline-tools`).

### `local.properties` Setup:
Ensure [`android/local.properties`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/android/local.properties) points to your local Android SDK directory:
```properties
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
```
*(On Linux/macOS: `sdk.dir=/home/<username>/Android/Sdk`)*

---

## 3. Build Commands

From the root repository directory or the `/android` directory, run:

### Build Debug APK:
```bash
cd android
./gradlew assembleDebug
```
*(On Windows: `.\gradlew.bat assembleDebug`)*

### Build Release APK:
```bash
cd android
./gradlew assembleRelease
```
*(On Windows: `.\gradlew.bat assembleRelease`)*

### Run Automated Unit & Room Tests:
```bash
cd android
./gradlew test
```

---

## 4. Generated APK Locations

Upon successful compilation, artifacts are generated at:
- **Debug APK:**
  [`android/app/build/outputs/apk/debug/app-debug.apk`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/android/app/build/outputs/apk/debug/app-debug.apk)
- **Release APK:**
  [`android/app/build/outputs/apk/release/app-release.apk`](file:///c:/Users/sshub/Music/SwiftLab/Invoice-Billing-Application/android/app/build/outputs/apk/release/app-release.apk)

---

## 5. API Configuration & Endpoints

The API Base URL is configurable both at compile-time and runtime:
1. **Compile-time Configuration (`app/build.gradle.kts`):**
   - Debug default: `http://10.0.2.2:8080/` (standard Android Emulator loopback to host)
   - Release default: `https://swiftlab-platform.onrender.com/`
2. **Runtime Configuration (Settings Screen):**
   - Open **Settings** $\rightarrow$ **Server Base URL**.
   - Enter your local LAN server IP (e.g. `http://192.168.1.100:8080/`) or cloud URL.
   - Tap **Save Server URL**.

---

## 6. Release Signing Instructions

For production distribution on Google Play Store or enterprise MDM:

1. **Generate a Production Keystore:**
   ```bash
   keytool -genkey -v -keystore swiftlab-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias swiftlab-key
   ```
2. **Configure Signing in `app/build.gradle.kts`:**
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file(System.getenv("KEYSTORE_PATH") ?: "swiftlab-release-key.jks")
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               keyAlias = System.getenv("KEY_ALIAS") ?: "swiftlab-key"
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               isMinifyEnabled = true
               proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
           }
       }
   }
   ```
3. **Sign with `apksigner` manually (if building unsigned APK):**
   ```bash
   zipalign -v -p 4 app-release-unsigned.apk app-release-aligned.apk
   apksigner sign --ks swiftlab-release-key.jks --out app-release-signed.apk app-release-aligned.apk
   ```

---

## 7. Device Installation & Testing

### Install via ADB:
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

### Install Directly on Android Hardware:
1. Transfer the `.apk` file to device storage via USB, Google Drive, or local HTTP server.
2. Open Files app on the device and tap the `.apk`.
3. Enable "Install from unknown sources" if prompted.
4. Launch the **SwiftLab** app.

---

## 8. Offline Mode & Sync Verification

1. **LOCAL_ONLY Mode:**
   - Launch app $\rightarrow$ Tap **"Continue Offline (LOCAL_ONLY)"**.
   - Add products, record POS sales, generate invoices.
   - All transactions are stored locally in Room SQLite database.
   - Zero background network calls occur, even if Wi-Fi / mobile data is connected.
2. **CLOUD_SYNC Mode:**
   - Go to **Settings** $\rightarrow$ Tap **CLOUD_SYNC** $\rightarrow$ Confirm prompt.
   - Enter API server URL and login credentials.
   - Tap **"Trigger Manual Sync Now"**.
   - Pending sales, inventory adjustments, and invoices are synced with the Spring Boot server and Supabase PostgreSQL.
