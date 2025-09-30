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
