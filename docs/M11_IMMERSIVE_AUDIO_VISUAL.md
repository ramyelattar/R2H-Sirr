# R2H-Sirr — M11 Cinematic Audio/Visual Upgrade

**2026-10-25 — source implementation on `feature/m11-immersive-audio-visual`.**
Base: `feature/m10-premium-experience` (unchanged).
APK package: `com.r2h.sirr` (same, data-preserving update).
Preview versionCode: **6**, versionName: `0.6.0-m11-preview`.

## Four requested features — source status

| Feature | Implementation | Status |
| --- | --- | --- |
| Unique animated splash | Android 12 system icon/window splash backed by custom adaptive icon; 2.45s Compose opening scene with logo pulse, glowing rings, fade/scale copy and timed progress animation | IMPLEMENTED IN SOURCE; DEVICE VISUAL NOT VERIFIED |
| Animated background on every app page | One global Compose `SirrAmbientBackground`: drifting light pools, orbit lines, shimmer particles, route-specific colors, low alpha for content readability. Obeys device animation scale and cinematic setting | IMPLEMENTED IN SOURCE; DEVICE FPS NOT VERIFIED |
| High-impact app icon image | New scalable custom crescent/lock-seal vector, golden keyhole, rose/purple rings, bright star accents; integrated as round and standard adaptive launcher icon and Android 12 system splash | IMPLEMENTED IN RESOURCES; ACTUAL LAUNCHER DISPLAY NOT VERIFIED |
| Different background music per page | Native offline PCM synthesis with distinct pitches/motifs for splash/gate/home/couple/party/dice/quiz/scratch/whispers/remote/settings/privacy. Looped through one `AudioTrack`, using Android AudioFocus and lifecycle pause/resume; volume and enable controlled separately from existing game cues. Suppressed during user-selected Whisper soundtrack | IMPLEMENTED IN SOURCE; AUDIO DEVICE ACCEPTANCE NOT VERIFIED |

The music is **locally synthesized original ambient music**, not licensed commercial songs and not streaming assets. The app does not request extra audio permissions or microphone access.

## Modified files

- `app/src/main/java/com/r2h/sirr/SirrAmbient.kt` — all-pages animated background and Compose intro
- `app/src/main/java/com/r2h/sirr/SirrSoundtrack.kt` — unique per-scene musical score and `AudioTrack` focus/lifecycle controller
- `app/src/main/java/com/r2h/sirr/MainActivity.kt` — global rendering / music navigation integration, splash state, adult-gate overlay
- `app/src/main/java/com/r2h/sirr/GameEffects.kt` — music sound controls, encrypted settings storage, live update
- `app/src/main/java/com/r2h/sirr/WhispersScreen.kt` — user-selected song suppresses page background loop
- `app/src/main/res/drawable/sirr_launcher_foreground.xml` — exclusive custom icon vector
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` — Android adaptive/round icons
- `app/src/main/res/values/colors.xml`, `values/styles.xml`, `values-v31/styles.xml` — launcher and system splash styling
- `app/src/main/AndroidManifest.xml` — icon and theme applied
- `app/build.gradle.kts` — M11 version
- `tools/Build-Install-M11.ps1` — build-once, in-place update helper; checks current branch and device serial

## Protection / acceptance

- No background music recording, no streaming and no forced microphone permissions.
- Music enabled by default at a low `21%` user setting; users can mute or adjust independently of sound effects.
- Stop/abandon audio focus on app background, focus changes and teardown.
- Honor Android motion accessibility scale as well as own cinematic preference.
- User content, existing on-device app and earlier GitHub M9/M10 branches are not modified by this source change.
- **No Gradle or Android device build, APK install, audio playback test, screenshot capture or 90/100 design sign-off was performed from this GitHub session.** Android compiler/device are the next verification gate; do not mark ready for production.
- No GitHub Actions tests are needed for the owner acceptance route.

## Consolidated device build/update — Windows

Run from the correct repository (not `R2H-18`):

```powershell
Set-Location 'E:\Projects\R2H-Android\R2H-Sirr'
git fetch origin feature/m11-immersive-audio-visual
git switch -c feature/m11-immersive-audio-visual FETCH_HEAD
powershell -ExecutionPolicy Bypass -File .\tools\Build-Install-M11.ps1 -DeviceSerial 00170156M000568
```

If the local branch already exists, use `git switch feature/m11-immersive-audio-visual` and `git pull --ff-only origin feature/m11-immersive-audio-visual`. Don't reset or clean local changes and don't uninstall the Android app. Gradle 8.13 auto-bootstraps/reuses verified cache. `adb install -r` preserves data if Android signing permits.

Accept only after real-phone checks: splash timing, launcher icon, scene-specific motion, readability, persistent audio setting, volume changes, audio focus with other apps, media override in whispers, background/foreground, and battery/heat.
