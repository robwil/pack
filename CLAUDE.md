# CLAUDE.md

## Verification

At the end of every work task, always:

1. Compile: `./gradlew assembleDebug`
2. Run unit tests: `./gradlew testDebugUnitTest`
3. Build test APK and run instrumented tests via adb (requires a running emulator):
```bash
./gradlew assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w me.robwilliams.pack.test/androidx.test.runner.AndroidJUnitRunner
```

Do not consider work complete until build and all tests pass.

Note: Do NOT use `./gradlew connectedDebugAndroidTest` — it uninstalls and reinstalls the app, wiping the database on the emulator. The adb approach above preserves app data.
