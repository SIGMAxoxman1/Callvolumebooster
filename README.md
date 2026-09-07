# Call Volume Booster

Raises ringtone volume automatically when one of your chosen VIP contacts
calls — even if the phone is on silent. Everyone else rings normally.

## How to build the APK without a local Android setup

1. Push this whole folder to a GitHub repository.
2. Go to the repo's **Actions** tab — the "Build APK" workflow runs
   automatically on push, or trigger it manually with **Run workflow**.
3. Open the finished run and download the **call-volume-booster-debug**
   artifact — a zip containing `app-debug.apk`.
4. Copy it to your phone and install it (allow "install unknown apps" for
   whichever app opens the file).

## What happens the first time you open the app

1. A short splash screen with the Sigma Apps mark.
2. The app asks for phone-state, contacts, and notification permissions.
3. It also opens two Settings screens you need to approve manually:
   - **Do Not Disturb access** — without this, Android blocks ring-volume
     changes whenever the phone is in silent/DND mode.
   - **Battery optimization exemption** — without this, Android may kill
     the background watcher after a while.
4. Once permissions are granted, the watcher starts automatically — there
   is no "Start" button. It also restarts itself after the phone reboots.

## Adding VIP contacts

Tap **+ Add contact** to see every contact on the device. Contacts you've
already added show a green checkmark. Tapping an unchecked contact adds it
(and shows "Added 1 of your contacts"); tapping a checked one removes it.

## Supported Android versions

- Minimum: Android 5.0 (API 21)
- Target: Android 14 (API 34)

## Known limitations to be aware of

- **Caller-ID reading on some OEMs**: the app now requests `READ_CALL_LOG`
  (required since Android 10 to receive the caller's number at all), which
  fixes the "nothing happens" symptom on stock Android. A few OEM builds
  (heavily customized ROMs) may still handle this differently — if it's
  still unreliable on a specific device, the sturdier alternative is
  registering as a `CallScreeningService` (Android 10+).
- **Android 13+ "restricted settings"**: because this APK is sideloaded
  (not from Play Store), Android may block some of the permissions above
  by default. If a permission screen looks blocked, open
  **Settings → Apps → Call Volume Booster → (3-dot menu) → Allow
  restricted settings**, then try again.
- **OEM auto-start lists**: phones from Xiaomi, Huawei, Oppo, and similar
  brands have their own separate "auto-start" toggle outside of Android's
  standard battery settings. If the watcher stops working after a while
  on one of these phones, check the manufacturer's battery/auto-start app
  settings too — this is a device restriction, not something the app's
  code controls.
- **Volume is boosted but not restored** to its previous level after the
  VIP call ends — a reasonable next step in `PhoneStateReceiver`.
