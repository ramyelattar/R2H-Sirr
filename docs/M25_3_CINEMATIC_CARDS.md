# M25.3 — Cinematic card / icon polish

Base: feature/m25-2-premium-game-cards-icons
Branch: feature/m25-3-cinematic-cards

## Implementation
- Six home-game tiles are now distinct premium compositions, not enlarged copies of the same card. Direction of the individually drawn 83dp artwork alternates across games, with unique edge engraving, ambient light, catalogue chapter numbers and contextual labels. Text occupies its own stable lower region.
- Featured game has its own wide editorial composition and visible magenta-to-violet gradient CTA instead of a washed-out pale rectangle.
- Neon CTA material is reused for other primary game actions in SirrAction, preserving enabled/disabled logic, button semantics, onClick callbacks and explanatory text.
- R2HGameArt icons have different metallic framing and micro-glints according to the actual game rather than using a ring around every icon.
- Reduced header clearance from 15.5% / 96–152dp to 12.8% / 94–126dp, maintaining the Android statusBarsPadding safe zone and the original +18 / R2H / Play All The Day graphics.
- Home cards are a little more compact (minimum 172dp rather than 182dp) and spacing is tighter while keeping accessible click targets.
- No new third-party dependencies, images, GIFs, network requests or emoji.

## Preserved
- The approved on-device WebP wallpaper and animation.
- Every game, audio, session, consent, cryptography, private user data and navigation callback.
- Canonical 230 challenges, 175 questions and generated assets.
- Existing applicationId com.r2h.sirr and in-place update compatibility.

## Build handoff
User Windows checkout, with M25 wallpaper already at:
  app/src/main/res/drawable-nodpi/r2h_neon_wallpaper.webp

Run tools/Build-M25-3.ps1 from the branch. Produces debug APK only,
no ADB, install, uninstall, wipe, GitHub Actions or CI.
APK remains at app/build/outputs/apk/debug/app-debug.apk.
Version is 0.9.3-m25-cinematic-cards (versionCode 12).

## Verification
Source-only GitHub read-back and protected-blob comparison.
NOT YET compiled in Kotlin, built as APK, visually tested on Android,
or declared release-ready. First device build may reveal actual
compiler/runtime issues that must be resolved before visual sign-off.
