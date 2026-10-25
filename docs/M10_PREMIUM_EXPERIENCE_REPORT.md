# R2H-Sirr — M10 Premium Experience Implementation

**Date:** 2026-10-25
**Branch:** `feature/m10-premium-experience`
**Base:** `feature/m9-stabilization`
**Package:** `com.r2h.sirr` (unchanged)
**Version:** `0.5.0-m10-preview`, versionCode 5

## Goal

Respond to the owner's nine real-device screenshots from M9: repetitive pink gradients, tiny choice chips, weak contrast, unclear disabled buttons, hard-to-interpret participant consent, excessive empty space and generic forms in place of active play. The prior quality estimate (~67/100) is subjective; M10 has **not** yet been scored from device screenshots.

## Changes implemented directly in Kotlin source

| Area | Evidence | M10 change |
| --- | --- | --- |
| Shared UI | `SirrPremiumUI.kt` | Accessible high-contrast CTA, clear disabled-state explanations, large selectable choice tiles, labeled consent rows, readable section headers |
| Home and adult gate | `MainActivity.kt` | Stronger home content hierarchy, compact group entry and discovery cards, improved adult opt-in guidance and typography contrast |
| Couple and party | `ExperienceScreen.kt` | Visual stages for mood/rounds/consent, larger 2-column mode choice, named consent feedback, clear start gating, active challenge stays above the fold |
| Quiz | `QuizScreen.kt` and `SirrDiscovery.kt` | Seven distinct chapter teasers, unique chapter tones, fewer oversized repeated rows, solo/two-player selection, readable consent handoff and full-width answer cards |
| Dice | `DiceScreen.kt` | Result directly beneath roll action and before advanced settings |
| Whispers | `WhispersScreen.kt` | Three-act composer with earlier suggestions, clear per-step copy, explicit ephemeral-message deletion, concise reveal experience |
| Scratch | `ScratchScreenV2.kt` | Guided creation, larger 1–3-secret choice, explicit reveal progress and completion/cleanup |
| Challenge reveal | `SirrVisuals.kt` | Replaces the generic circular reveal area with a full-tap sealed-keyhole card, stronger hierarchy and explicit reveal cue |
| Touch targets | `SirrPremiumUI.kt` | Larger selectable options with a minimum 60dp height |
| Local install | `tools/Build-Install-M10.ps1` | Official Gradle 8.13 bootstrap with SHA-256 verification and `adb install -r` update; never uninstalls or clears data |

## Verification and limits

- Source edits pushed to the independent M10 branch, preserving M9 and `main`.
- New composable references and branch-level code inspected. A lightweight source-only brace-balance check passed for all changed Kotlin source files; **this is not compiler validation or an Android device test**.
- **Android build, visual acceptance, runtime feature checks, performance and 90/100 UX acceptance have not been performed** for M10.
- GitHub Actions is intentionally excluded per owner's direction. Do not claim CI success or release approval.

## Local single-build handoff

When the owner is ready for one consolidated build and device acceptance, use PowerShell from `E:\Projects\R2H-Android\R2H-Sirr`:

```powershell
git fetch origin feature/m10-premium-experience
git switch -c feature/m10-premium-experience FETCH_HEAD
powershell -ExecutionPolicy Bypass -File .\tools\Build-Install-M10.ps1
```

If a local M10 branch already exists, use `git switch feature/m10-premium-experience` followed by `git pull --ff-only origin feature/m10-premium-experience` instead. Never use reset/clean or uninstall to handle local modifications. Specify `-DeviceSerial` if two ADB transports (USB/Wi-Fi) are visible.

## Acceptance focus

Review actual screen screenshots and gameplay: first impression, named consent and disabled button clarity, how early the challenge appears, quiz choices, dice outcome visibility, whisper one-time reveal, scratch completion, data preservation, and screen-reader/tap-target behavior. Only then reassess the 90/100 target.
