# M8 — UI Refinement / QA Status (24 October 2026)

## Objective
Make R2H Sirr visually mysterious and immediately inviting, with concise everyday Egyptian Arabic and gameplay that feels like a game rather than a data-entry form.

## Source changes in latest follow-up
- Home: dark wine/charcoal/champagne hero with illustrated keyhole/orbits/sparkles, a clear primary action and responsive 184dp minimum game cards.
- Couple/party: sealed challenge scene. Tap once to reveal the prompt; keep **Skip** and **Stop** available even before opening. Users must still consent to joining the local game.
- Scratch: connected gesture interpolation and more realistic foil styling; optional audio/haptic reveal cue (user preference defaults apply).
- Whispers: manually start and stop an optional selected soundtrack **during** reading or at the ending; no autoplay and no upload. MediaPlayer safely released.
- Quizzes: question-by-question result comparison; no misleading personality claims; removed the inert final disabled button.
- Remote discovery: primary landing card honestly states that a private HTTPS server is required.
- Navigation/text: preserved RTL labels, dynamic Arabic typography and opt-in adult self-attestation remembered in encrypted vault.

## Verification
The GitHub branch `feature/m8-egyptian-experience` contains the changes. Source-inspection assertions verified existence of the new calls and guard tests; this is **static source inspection only**. No Gradle, Kotlin/Compose build, APK packaging, Android instrumentation, UI screenshots or live two-phone sessions were performed in this follow-up.

- `tools/check_m8_ui_source.py`: guards previous handoff, scratch, privacy and gameplay bugs.
- `tools/Test-M8-Source.ps1`: Windows local, one-command Python and Node checks without Android/Gradle.
- `.github/workflows/m8-source-tests.yml`: workflow definition for these checks.

### GitHub Actions blocker
Observed run: https://github.com/ramyelattar/R2H-Sirr/actions/runs/40088613290
API shows `name=""`, `path="BuildFailed"`, `conclusion="startup_failure"`, and **zero jobs**. That means this run did not execute our tests or reach a runner; the cause (workflow resolution, repository policy, or GitHub infrastructure) is not proven. Repeatedly changing the app's code will not fix this observation by itself.

Suggested owner-side investigation: GitHub repository Settings > Actions > General, make sure permitted actions are enabled for this private repo; review actual Actions workflow registration. If the same zero-job `BuildFailed` failure persists, provide the run URL and API signature to GitHub Support. Do not claim CI green until named jobs actually pass.

### Local fallback without building APK
From repo root on Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Test-M8-Source.ps1
```

This runs catalog sync, UI-source guards, and Node backend tests only. First invocation may install Node dependencies. It does **not** compile Android. The command has been authored, **not executed on the owner's PC**.

## Acceptance before an owner-requested APK
1. Compose typecheck and Debug APK compilation.
2. Confirm no clipping/overlap at 360dp, 393dp, 412dp and font scale 1.3–1.5.
3. Inspect gate, home, first sealed challenge, skip/stop, 3 dice, scratch, whisper soundtrack and two-player quiz handoff.
4. Confirm animator scale 0 disables optional cinematic motion; muted sound/haptic settings stay silent.
5. Test account/data handling, temporary media cleanup, and screenshots blocked by FLAG_SECURE.
6. Test Node + Android + browser protocols and contentVersion 4 separately for remote multiplayer.

**PR stays Draft; no merge into main.**
