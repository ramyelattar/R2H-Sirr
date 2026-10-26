# M24 — Local debug-build handoff (no GitHub tests)

## Branch
`feature/m24-local-build-ready` contains all previous chained changes from M14–M23.

## Final source fixes
- `MainActivity.kt`: +18 acknowledgement now fails closed if encrypted storage fails and shows an error message instead of allowing entry with consent not persisted.
- `tools/Verify-M23-Source.ps1`: corrected PowerShell argument passing by using explicit named parameters and array argument values.
- `app/build.gradle.kts`: same stable application ID `com.r2h.sirr` and no schema migration, but increased debug version code 7→8 and preview label.
- `tools/Build-M24.ps1`: created a **BUILD-ONLY** script based on the previous verified Gradle setup strategy (JDK17, SDK, Gradle8.13 or signed/hashed download). It checks the current branch, invokes `:app:assembleDebug`, outputs the APK path and stops. No ADB, uninstall, install, wipe, merge, or GitHub Actions.

## Explicitly outside this phase
- No developer-host build or Kotlin compilation has been run by the assistant; Gradle and Android SDK are on the user's Windows PC.
- The feature branch is a build candidate, **not certified as compiled or 90% UX**. A local build run must establish that compilation works.
- The previous M23 static content checks and historical app data are unchanged. Remote internet play still needs its configured server.
- If local build fails, preserve the full first error and fix the exact source; do not wipe app data.

## Build
From Windows PowerShell, switch to `feature/m24-local-build-ready` after fetching it, then run:

```powershell
& .\tools\Build-M24.ps1
```

On success, the output is `app\build\outputs\apk\debug\app-debug.apk`. The script does **not** install it.
