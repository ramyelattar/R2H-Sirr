# M28 — R2H-Play Cosmic Logo, centered at the top of every page

**Status: implemented as source code on a stacked draft branch. No Android compilation, APK packaging, or device validation has run for M28.**

## User requirement

Use the user-supplied `R2H Play` metallic black/chrome + violet cosmic logo currently stored locally at:

`E:\Projects\R2H-Android\R2H-Sirr\app\src\main\res\drawable\r2h_play_cosmic_logo.png`

as the **centered logo at the top of all pages**.

## Implementation

- `R2HBrandLogo.kt`: one shared brand composable. Uses the optional `r2h_play_cosmic_logo` drawable, and `r2h_play_cosmic_header` horizontal derived lockup in compact headers. The fallback is a plain text brand only when the owner-supplied image is missing. A source-only checkout can therefore compile, but a real asset build MUST verify the packaged logo resources.
- `MainActivity.kt`: the existing top row is now a `Box` of height 70dp. The centered logo is aligned independently of buttons; Back remains on Start (right in RTL) and Music remains on End (left in RTL). This applies to Home, Dice, Challenges, Quiz, Scratch, Whispers, Party, Remote, Settings and Privacy. The old duplicate Home `R2H-Play` text was removed.
- The Adult Gate now includes the same centered logo at the top, followed by scrollable consent controls. The redundant large text `R2H-Play` was removed; age and consent requirements are unchanged.
- `SirrAmbient.kt`: the cinematic splash now uses the full owner logo, and redundant extra wordmark text was removed. The existing entrance animation, route transitions and background video stay separate.
- The previous M27 purple cosmic video background remains unchanged, muted and looping. The logo has no tap handler or online dependencies.

## Image preparation for Android

The original square PNG has large black outer margins; displayed in a 70dp header it would be tiny and would create a conspicuous black block over the moving purple wallpaper.

For this reason, the build bundle includes two **derived versions of the exact provided visual**:

- `r2h_play_cosmic_logo.png` — full stacked logo, visually unchanged, cropped/transparent to remove external black background, for splash.
- `r2h_play_cosmic_header.png` — horizontal compact lockup made only from the original emblem and the original `R2H Play` wordmark, not new lettering, for the global top header.

The user's source image path is treated as an input and is **never overwritten**. The safe M28 build script verifies the source exists and logs its SHA256, then packages both optimized assets inside an independent Git worktree. It also includes the M27 MP4/poster, which are not yet stored as binary objects on GitHub.

## Testing requirements

1. Verify original source image exists at the user path.
2. Verify the optimized logo PNG files and M27 media are present and unchanged before building.
3. Build `:app:assembleDebug` and `:app:testDebugUnitTest` with a pinned PR commit, using a detached isolated worktree; **never update/replace main**.
4. Inspect compiled APK for the two PNG resource names and the original MP4 bytes.
5. Install via `adb install -r` only if signing is compatible; never uninstall/clear user data.
6. Screenshots: verify central brand size, readability, safe insets and no collisions with Back/Music on 360dp and 412dp widths, with system font scale 1.0 and 1.5.
7. Inspect Splash, Age Gate, Home, all games, Remote, Settings and Privacy for a consistent brand and no overlap.
8. Check that switching pages does not restart the wallpaper video or corrupt app navigation.

## Not included

- Android launcher icon replacement.
- New navigation functionality or gameplay content.
- Changes to Dice physics, encrypted storage or user-authored data.
- Changes to the supplied brand design beyond layout-safe transparent cropping and rearrangement of the same parts.
