# M8 P0 — Runtime Safety and Source Audit
Date: 2026-10-24
Scope: Android app source + CI observation. **No Gradle, APK, device test, deploy or merge** performed.

## Confirmed bugs addressed
| Priority | Defect in M8 source | Fix |
|---|---|---|
| P0 | System Back / header Back discarded an open game's UI state immediately | Both paths now go through `navigateHome()`, requiring an explicit confirmation on gameplay routes; cancel keeps the current screen and round |
| P0 | `امسح بياناتي الخاصة` deleted encrypted records with a single tap | Separate deletion confirmation with clear irreversible-action wording |
| P0 | Manual data wipe left temporary decrypted `sirr_media_*` cache files until next app launch | The explicit wipe now removes those temporary files in addition to the encrypted vault |
| P0 | User-selected whisper soundtrack could keep playing when activity went to background | Lifecycle observer pauses playback on `Lifecycle.Event.ON_STOP`; removes observer and releases player on disposal |

## Files changed
- `app/src/main/java/com/r2h/sirr/MainActivity.kt`: exit confirmation, delete confirmation, local decrypted cache cleanup
- `app/src/main/java/com/r2h/sirr/WhispersScreen.kt`: lifecycle-aware pause of user-selected audio
- `tools/check_m8_ui_source.py`: static source guards for safety paths and relevant AndroidManifest restrictions

## Inspected before further builds
- `app/src/main/AndroidManifest.xml` includes `allowBackup=false`, `usesCleartextTraffic=false`, Android RTL support and opt-in Internet/Vibrate permissions.
- `app/src/main/java/com/r2h/sirr/Security.kt` uses Android Keystore AES-GCM local vault and no-backup directory.
- Active canonical catalogs are version 4. Repo source contents observed: 230 unique challenges and 175 unique quiz questions in seven packs, 25 each.
- Existing Kotlin/JUnit and Node test files exist, but no Android typecheck or live runtime test was performed in this audit.

## CI blocker: observed, not fixed
Latest observed failed GitHub Actions run:
https://github.com/ramyelattar/R2H-Sirr/actions/runs/40096161261

Repository API reports:
- `name = ""`
- `path = "BuildFailed"`
- `conclusion = "startup_failure"`
- `event = "pull_request"`
- `jobs = 0`

**The workflow never got a runner or executed tests.** Similar synthetic runs are reported publicly across repositories; this evidence does **not** prove the root cause is the same for this repo. Changing the project's Kotlin or workflow YAML without evidence is not a justified repair. Owner can inspect repository Settings → Actions → General, account/organization billing, then share the fingerprint and run ID with GitHub Support if the issue persists.

## Next P0 verification gates
1. Execute `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Test-M8-Source.ps1` on the already-configured Windows dev PC. It checks Python content-generation mirrors, UI guards, Node backend tests and Node syntax. It does **not** compile or install Android.
2. Before 90% acceptance, run Android debug compilation and unit tests with explicit owner approval; resolve actual Compose errors by compiler diagnostics, not guesses.
3. Test locally: open a game → system back → Cancel leaves round unchanged; exit confirmation returns home. Privacy → Delete → Cancel preserves vault; Confirm removes encrypted records and decrypted previews.
4. On a real Android device: play user-selected whisper audio, background the app; audio must pause. Return to app; no autoplay or recording.
5. Check rotation/process recreation; in-progress state still uses `remember` in multiple screens and may be lost. Consider a proper `SavedStateHandle`/saveable game state after verifying requirements; do not claim this is fixed.
6. Investigate actual GitHub Actions admission failure before calling CI green.

## Release decision
**P0 source fixes are committed, P0 remains PARTIAL / NOT ACCEPTED.** Static source checks are not proof of successful Compose compilation, app runtime, secure deletion or visual quality. User's assessed score remains **65/100**, target **>=90/100**. PR stays Draft; do not merge to `main`.
