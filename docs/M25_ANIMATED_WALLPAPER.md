# M25 — Animated background across every R2H-Play page

## Exact asset from this conversation
The approved reference is the final vertical `R2H`, `+18`, `Play All / The Day` cosmic magenta wallpaper. It is distributed separately as `r2h_neon_wallpaper.webp` (941 × 1671) to preserve the user's generated image and avoid network calls from the app.

**Place it at:** `app/src/main/res/drawable-nodpi/r2h_neon_wallpaper.webp`. An accompanying M25 downloadable ZIP contains this exact asset in its destination folder.

## Integration
- `SirrAmbientBackground` is already the single root-layer backdrop behind the splash, age gate, home, couple, party, dice, quizzes, scratch, whispers, remote, settings, and privacy routes. It now draws the supplied wallpaper if installed, with a subtle static central contrast vignette.
- Ambient motion is a 36-second native Compose cycle: edge-only slow fuchsia bloom and sparse twinkling stars; the baked +18, R2H and Play All / The Day masthead **never shift**, so it cannot be cropped by parallax.
- Honors the app's cinematic-motion preference and Android animation scale; no coroutine downloads, no GIF, no video or allocation of new bitmaps each frame.
- Renders with FillBounds to preserve both top-corner labels on narrow handsets.
- Global content header is shifted 92dp downward so it does not overlap the top masthead; redundant home R2H text is removed. Splash dimming is reduced to expose the background.
- If the image is not copied, app retains a dark gradient fallback: the build can still succeed but the **requested graphic will not be shown**. Check installation of the binary asset.

## Local preparation
1. Extract the provided M25 wallpaper ZIP at the repository root (it contains `app/src/main/res/drawable-nodpi/r2h_neon_wallpaper.webp`).
2. Or run `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Install-M25-Wallpaper.ps1 -ImagePath "<wallpaper.webp>"`.
3. Build only after the file exists; the branch does not execute Gradle, GitHub CI or ADB on your behalf.

## Protected
No game/session/network backend changes; no quiz/challenge edits; no custom media or vault changes; package ID unchanged; no data deletion. Static review only. Android runtime and device visual confirmation still needed.
