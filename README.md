# Call Volume Booster

Raises ringtone volume automatically when a chosen VIP contact calls.

## How to build the APK without a local Android setup

1. Create a new GitHub repository and push this whole folder to it.
2. Go to the repo's **Actions** tab. The "Build APK" workflow runs
   automatically on every push to `main`, or you can trigger it manually
   with the **Run workflow** button.
3. Once it finishes (a few minutes), open the completed run and download
   the **call-volume-booster-debug** artifact — that's a zip containing
   `app-debug.apk`.
4. Copy the APK to your Android phone and install it (you'll need to allow
   "install unknown apps" for whichever app you use to open the file).

## Supported Android versions

- Minimum: Android 5.0 (API 21)
- Target: Android 14 (API 34)

## Known limitations to fix next

- Reading the incoming caller's number reliably on Android 10+ is
  restricted by Google; some OEMs/devices may not report it through the
  broadcast this app listens to. A more robust version would use the
  `CallScreeningService` API (Android 10+) or ask the user to make this
  app the default call-screening app.
- The ringer volume is boosted but not automatically restored after the
  call — add that in `PhoneStateReceiver` (listen for `CALL_STATE_IDLE`
  and restore the saved previous volume).
- Only one VIP contact is supported right now; extending to a list is a
  small change in `MainActivity` and `PhoneStateReceiver`.
