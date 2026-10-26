# R2H-Play M25.1 — Safe Masthead + Floating Glass UI

Base: `feature/m25-animated-wallpaper`. Candidate: `feature/m25-1-safe-header-floating-glass`.

## Implemented in Kotlin
- `SirrAmbient.kt`: The **same local WebP wallpaper** now starts **below the actual Android status-bar inset**. The baked-in R2H logo, +18 and the decorated Play All / The Day label move as a single static image; none are repainted separately. The remaining status-bar background is dark for legible Android clock/battery/network glyphs.
- Native 36-second motion preserved, with a low-opacity edge-ribbon drift; only stars/reflections and edge glow animate, **never the baked logo or text**. System animation scale and the existing cinematic preference continue to work.
- `MainActivity.kt`: Removed duplicated +18 labels from the top toolbar and home headline, and the decorative gate label; the **actual age-confirmation checkbox remains**. Replaced the fixed `92.dp` header spacer with screen-height-proportional clearance (15.5% of device screen height clamped to 96–152dp), and preserved independent `statusBarsPadding()` and `navigationBarsPadding()` for input screens.
- New shared `R2HGlass.kt`: visually translucent, three-tone glass gradient, two-step border light, soft native shadow and top specular line. No expensive dynamic blur or video, and no external assets.
- Applied the glass treatment across existing reusable Material surfaces: buttons, consent, level selectors, highlighted badges, content intros, main hero, 6 game tiles, navigation rows, seven quiz packs, individual quiz answer cards, challenge reveal/sealed cards, secret letter and private remote cards. Palette color preserves each game's identity.
- Preserved wallpaper resource path `app/src/main/res/drawable-nodpi/r2h_neon_wallpaper.webp`; no image modifications needed: shifting the entire drawable below system inset moves the baked +18/logo/tagline into the safe region.

## Protected and boundaries
All 230 challenge source entries, 175 quiz question entries, 60 whispers, 60 scratch prompts, custom dice, encrypted storage, network/session/game algorithms, screenshot security, existing app id `com.r2h.sirr` and saved user data are untouched. VersionCode rises 9→10 for a normal in-place update; never uninstall or clear app data.

## Run on Windows locally
The M25 wallpaper resource is already installed in the user's checkout and required by `tools/Build-M25-1.ps1`, which only runs `:app:assembleDebug` and never installs anything. Use `git fetch origin feature/m25-1-safe-header-floating-glass`, `git switch -c feature/m25-1-safe-header-floating-glass FETCH_HEAD`, then `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Build-M25-1.ps1`.

## Verification limits
Static repository/source and bracket comparisons only. **No Gradle/Kotlin compilation, APK build, phone installation, GitHub Actions tests or merge** has been performed. Verify status icon / graphic separation on both notched and non-notched devices and assess text contrast for 100%, large-font and landscape before release.
