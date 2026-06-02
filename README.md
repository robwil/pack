Simple Android app for managing Packing Lists and Trips which use them. For more information check out the blog post here: robwilliams.me/2015/07/pack-android-packing-list-app/

# Developer notes

## OAuth setup

We have two separate OAuth client_ids, which are necessary to interface with Google Drive for the "Cloud Backup" option within the app. We need two different ones, one for the Release APK and one for the Debug APK.

To get the SHA1 signature from the two different APK builds:

```bash
keytool -printcert -jarfile app.apk
```

## What to do with the SHA-1:
1. Copy the SHA-1 fingerprint from above command
2. Create the OAuth credential in Google Cloud Console:
   - Package name: `me.robwilliams.pack`
   - SHA-1: `[your keystore SHA-1]`
3. Update `app/src/{debug,release}/res/values/google_oauth_config.xml` with the debug and release client IDs respectively

This way, debug builds use debug OAuth and release builds use release OAuth automatically.

## Building

```bash
./gradlew assembleDebug
```

## Running tests

### Unit tests

No emulator required:

```bash
./gradlew testDebugUnitTest
```

### Instrumented tests

Requires a running emulator or connected device.

**Important:** Do not use `./gradlew connectedDebugAndroidTest` — it uninstalls and reinstalls the app, wiping the database on the emulator. Use adb directly instead:

```bash
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w me.robwilliams.pack.test/androidx.test.runner.AndroidJUnitRunner
```

To run a specific test class:
```bash
adb shell am instrument -w -e class me.robwilliams.pack.data.DatabaseMigrationTest me.robwilliams.pack.test/androidx.test.runner.AndroidJUnitRunner
```
