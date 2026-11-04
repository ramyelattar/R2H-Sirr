# M29 — Visual Polish & Dice Renderer Correction

**Scope:** grounded visual corrections prompted by M28 phone screenshots and short roll video. **Status: source changes implemented; Android APK not built or device-tested on this branch.**

## Observations from owner-provided phone captures
- The centered M28 horizontal R2H Play logo and M27 purple cosmic video are visible. Keep both; no rebranding.
- The front Champagne cube dominates projected screen area and hides much of the two rear cubes.
- Champagne looks overexposed; previous shader summed high ambient, diffuse and specular lighting before clipping.
- The floor is largely indistinguishable from a black box; the same scene is used across three dice.
- Home/featured cards have bright white-violet borders and pink glows directly over active video particles; small descriptions need higher legibility.
- Screenshots/video do not establish precise FPS, thermal or battery measurements, nor all face results.

## M29 implementations (no content changes)
1. `TripleDiceStageGeometry.kt`:
   - Rear dice centers x±1.04, z=-0.65, scale 0.92; front die x=0, z=0.64, scale 0.85.
   - Camera depth 4.60. A **depth-only perspective-size proxy** is ~1.225 front/rear instead of ~1.76 prior; it is an estimate, not measured screen geometry.
   - Pairwise conservative bounding-sphere clearances remain **positive** (~0.124 for front-to-rear and ~0.487 between rear dice).
   - Rotation-aware floor support, weighted dice and 18 face texture indices preserved.
2. `TripleDiceStage3D.kt`:
   - Lower camera: eye (0, 1.24, 4.60), aiming at (0, -0.40, 0). Front cube no longer dominates by model size.
   - GGX-inspired shader tuned for higher roughness, lower metallic factors, and **energy-limited** ambient/diffuse/specular terms; Champagne additionally toned down to protect labels.
   - World-space floor shader receives a modest center-weighted satin illumination, so the platform is distinguishable without returning to toy-like neon.
   - Original shader fallbacks remain intact; runtime GPU shader compatibility still requires device verification.
3. `R2HGlass.kt`: reused by pages; higher-opacity contrast under animated particles, quieter edge/spot light and less bright shared primary button gradients.
4. `R2HCinematicCards.kt`: card secondary text enlarged to 14sp, index caption to 11sp, decorative radial light reduced. Includes Home/Featured/Feature rows; content, ordering, navigation and imagery unchanged.
5. `TripleDiceStageGeometryTest.kt`: updated front-vs-rear invariant and added one test for balanced perspective proxy; **7 JUnit test declarations, not run**.

## Verification evidence — exact classification
- **Source code:** committed to dedicated M29 branch, stacked above draft M28.
- **Pure Kotlin geometry:** exact GitHub blob `8a90a7b22f77728ac663f3b8b415f34b1e3bf069` was matched by `git hash-object`, compiled with local `kotlinc` and executed with standalone assertions. They passed with front/rear proxy **1.2248847**, front/rear gap **0.124150395**, rear/rear gap **0.48651314**, and texture mapping 0..17.
- **Not verified:** Android Compose/Kotlin compilation, 7 JUnit tests, actual GL shader link, pixel-level visual comparison, UI contrast metrics, device FPS/thermal/battery, rollout success, or production readiness. Standalone Kotlin sanity is NOT the Android build.

## Required acceptance gates
- Build Android debug APK on an isolated pinned Worktree with **both M27 media files and both M28 logos included**, keeping `main` and saved user data untouched.
- Run `:app:testDebugUnitTest` and parse JUnit XML; verify test names/counts and failures.
- Confirm APK resources include user video, poster and both logo assets, byte-for-byte hashes.
- Install via `adb install -r` only (compatible signature required); never uninstall/clear data to solve a debug build issue.
- Re-capture Home and Dice screens at same brightness. Verify lighter Champagne no longer blows out; three dice remain individually legible and non-overlapping at rest and over at least 20 rolls.
- Check Android Logcat for GLES shader fallback and GL errors. Runtime GPU shader compilation only happens on the phone.
- Verify Arabic strings/photos map to the actual front faces, without orientation/mirroring; test longer owner texts.
- Confirm all pages continue to display M28 centered logo and M27 looping backdrop; check smaller displays and large font scale.
- Confirm the darker card materials still preserve tactile interaction and accessibility.
- Do not merge draft PRs #36–#42 or M29 until owner visually approves on-device.

## Decisions / limits
- No new PBR library/engine; GLES2 remains chosen for midrange Android until real performance measurements motivate a switch.
- No logo, media, game content, probability, vault, schema or data edits in M29.
- Geometry-proxy improvement and changed shading are engineering predictions, **not** confirmation of achieved 90/100 visual quality.
