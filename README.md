# Paper Glide

A one-button paper-plane game for Android phones and Android TV, in a single APK.
Tap (or press OK) to lift the plane and glide through the gaps in the cloud pillars.

- Kotlin + Jetpack Compose canvas, no game engine and no image assets (everything is drawn).
- Phone: portrait, touch. TV: landscape, remote or gamepad (shows in the TV launcher row).
- Rising difficulty, Bronze / Silver / Gold medals at 10 / 25 / 50, local best score.
- Day, sunset, night and sunrise cycle each round, with clouds, birds, planes and baby animals passing by.
- Sound effects, haptics on phone, looping background music, mute button.

## Controls

| Action | Phone | TV remote / gamepad / keyboard |
|---|---|---|
| Fly / start / restart | Tap anywhere | OK, Enter, D-pad Up, A, Space |
| Pause | Back | Back |
| Quit | Back while paused | Back while paused |
| Mute | Speaker button (top right) | Move focus to the speaker with the D-pad, then OK; Left/Down returns to the game |

## Download and install (no Android SDK needed)

Get `paper-glide.apk` from the [Releases page](https://github.com/stoufam71/glidepaper/releases). The latest one is always at
`https://github.com/stoufam71/glidepaper/releases/latest/download/paper-glide.apk`.

- **Phone:** open the link on the phone, download, tap the file, and allow "Install unknown apps"
  for your browser or file manager when Android asks.
- **Android TV / Google TV:** install a file manager or the *Downloader* app from the TV's app store,
  open the APK link in it, and allow it to install unknown apps when asked.
  Or download the APK on a computer and use the `adb` steps below.

New releases install over old ones and keep your best score.

## Releases (CI/CD)

`.github/workflows/android.yml` runs on every push to `main`, every pull request, and manual runs.
It tests, lints and builds a signed release APK. Each run attaches the APK as the `paper-glide-apk`
artifact; downloading artifacts needs a GitHub login.

To publish a release anyone can download, push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow then creates a GitHub Release `v1.0.0` with `paper-glide.apk` attached.
`versionName` comes from the tag, and `versionCode` from the workflow run number, so each release can update the one before.

### One-time signing setup (maintainer)

Android only installs an update when it is signed with the same key as the installed app. Create one release key and keep it safe:

```bash
keytool -genkeypair -v -keystore paper-glide-release.jks -alias paper-glide \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -i paper-glide-release.jks | pbcopy      # macOS; Linux: base64 -w0 paper-glide-release.jks
```

Add these in GitHub under **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | the base64 text copied above |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | `paper-glide` |
| `SIGNING_KEY_PASSWORD` | key password |

Never commit the `.jks` file (it is in `.gitignore`). If you lose it, users must uninstall the app (losing their best score) before they can install a build signed with a new key.
Without these secrets the workflow still builds, but signs with a throwaway debug key and shows a warning.

## Requirements (building from source)

- JDK 21 (Gradle toolchain), Android SDK with platform 37 and build tools.
- `adb` on `PATH` (Android SDK `platform-tools`).
- Devices: Android 7.0 (API 24) or newer.

The Gradle wrapper is checked in, so no separate Gradle install is needed.

## Build and test

```bash
./gradlew testDebugUnitTest   # unit + Robolectric UI tests
./gradlew lintDebug           # lint
./gradlew assembleDebug       # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # release APK; signed with SIGNING_* env vars if set, else the debug key
```

## Install on a device (for agents and humans)

Package: `com.paperglide`. Launch activity: `com.paperglide/.MainActivity`.

### Phone or emulator over USB

```bash
adb devices                                   # device must show as "device", not "unauthorized"
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -n com.paperglide/.MainActivity
```

To use an emulator, start an AVD first:

```bash
~/Library/Android/sdk/emulator/emulator -list-avds
~/Library/Android/sdk/emulator/emulator -avd <name> -no-snapshot-save -no-audio &
adb wait-for-device
until [ "$(adb -s emulator-5554 shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
```

A fresh emulator may show a "Viewing full screen" system prompt over the game; dismiss it before sending input.

### Android TV over Wi-Fi (wireless debugging)

One-time setup on the TV (a person has to do this):

1. Settings → System → About → click **Android TV OS build** 7 times to enable Developer options.
2. Settings → System → Developer options → turn on **Wireless debugging**.
3. Inside Wireless debugging, choose **Pair device with pairing code**. The TV shows an
   `IP:PORT` and a 6-digit code. Ask the user for both; they are only valid while that screen is open.

The computer and the TV must be on the same network. Then:

```bash
# Avoid a second, flaky mDNS transport to the same TV.
export ADB_MDNS_AUTO_CONNECT=0

# 1. Pair once per computer (pairing port differs from the connect port).
adb pair <PAIR_IP>:<PAIR_PORT> <CODE>

# 2. Connect using the IP:PORT shown on the Wireless debugging screen (not the pairing one).
adb connect <TV_IP>:<CONNECT_PORT>
adb devices                                   # TV must show as "device"

# 3. Install and launch.
adb -s <TV_IP>:<CONNECT_PORT> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <TV_IP>:<CONNECT_PORT> shell am start -n com.paperglide/.MainActivity
```

If the connect port is unknown, a paired TV advertises it over mDNS (macOS shown):

```bash
dns-sd -B _adb-tls-connect._tcp                    # lists "adb-<id>" services; Ctrl-C to stop
dns-sd -L "<service name>" _adb-tls-connect._tcp local.   # prints host name and port
dscacheutil -q host -a name <host>.local           # resolves the host name to an IP
```

### Troubleshooting

| Symptom | Fix |
|---|---|
| `No route to host` on macOS | Allow the terminal under System Settings → Privacy & Security → Local Network, then `adb kill-server && adb start-server`. |
| `device offline` or install stalls | Wi-Fi is flaky or two transports exist: `adb disconnect`, set `ADB_MDNS_AUTO_CONNECT=0`, connect again. |
| TV not found after it changed network | Wireless debugging turns off on network change. Turn it on again; the IP and port change. |
| `failed to authenticate` / `unauthorized` | Pair again with a fresh code from the TV. |
| Install fails with signature mismatch | `adb -s <serial> uninstall com.paperglide` first (this clears the saved best score). |

### Rules for agents

- Only install and launch when the user asks. Installing replaces the app but keeps its saved best score and mute setting.
- A TV is a shared screen. Do not send key events (`adb shell input ...`) or take screenshots
  unless the user asked for it **and** Paper Glide is in front:
  `adb -s <serial> shell dumpsys activity activities | grep mResumedActivity` must name `com.paperglide`.
- Never put a TV's IP, pairing code or device ID into committed files.
- `adb shell screenrecord` does not work on some emulators; use repeated `adb exec-out screencap -p > frame.png` instead.
- Robolectric tests must not call `Robolectric.buildActivity(...).setup()` on `MainActivity`: its endless
  frame loop never idles. See `docs/solutions/test-failures/robolectric-hangs-on-compose-game-loop.md`.

## Project layout

```
app/src/main/java/com/paperglide/
  MainActivity.kt        key handling, back, lifecycle, TV vs phone orientation
  GameViewModel.kt       game state for the UI, sound, haptics, music, saved best
  game/                  pure-Kotlin engine: physics, pillars, scoring, difficulty
  ui/                    Compose screen, dialogs, sky, clouds, meadow, passers-by
app/src/test/            unit and Robolectric tests
docs/                    plan, credits, solved-problem notes
```

## Credits

See [docs/CREDITS.md](docs/CREDITS.md).
