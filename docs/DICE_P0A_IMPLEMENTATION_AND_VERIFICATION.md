# R2H-Play — Dice P0-A (Rounded 3D Renderer)

Status: **source implementation of P0-A plus initial P0-C/D UI; NOT yet compiled or device verified**.

## Scope implemented

- The former perspective-warped Android Canvas cube is replaced by a **real GLES2 mesh**.
- Two independent renderer instances, each containing 6 user-textured faces.
- 24x24 tessellation per physical face; rounded geometry radius 0.135 cube units.
- Per-vertex curved normals, localized dynamic shading, controlled specular highlight.
- Directional lighting and a low-strength contact glow below each cube.
- Six local texture tiles per cube, generated from the existing owner's exact text and decrypted private photo thumbnail. No preset content, no new network dependency.
- Canonical selection angles shared with the existing DiceCubeProjection model: FRONT, RIGHT, BACK, LEFT, TOP, BOTTOM.
- OpenGL work on dedicated rendering thread, request-render mode, lifecycle stop/start handling.
- Restrained Obsidian Dice stage using Lilac/Pearl differentiation.
- A first-pass dedicated **Face Studio** `ModalBottomSheet`: one cube and one face at a time, 2×3 face selector, live GLES2 preview, authored text/image actions, per-face weight slider, equal-weight reset, duration controls, encrypted draft save.
- First-pass premium result cards with user-supplied images/text and an accessible primary rolling button.
- Existing roll weighting, timing, photo storage, draft persistence, and legacy 3-cube mode remain unchanged.

## Engineering trade-offs

This initial P0-A implementation uses **platform OpenGL ES 2.0** and not Filament. It adds no large native renderer dependency and no extra remote services. Specular lighting is lightweight shader-based; it is **not** full PBR/physically based reflection. The renderer is a candidate, not an approved performance result.

Geometry uses ~20,736 triangle vertices per cube, ~664 KB interleaved CPU vertex data each, plus six 384×384 face textures and bitmaps. Actual GPU memory use, rendering precision, pacing, and thermal behavior have **not** been measured.

## Tests added (not yet executed)

`RoundedDiceMeshTest.kt` defines six JVM tests for:
- vertex count
- finite bounded coordinates and unit normals
- canonical face ordering
- actual rounded corner geometry
- outward triangle winding on all 6 faces
- deterministic geometry and rejected parameters

The prior `DiceCubeProjectionTest.kt` remains, including selection and final orientation checks.

## Mandatory gates before merge

1. `gradle :app:assembleDebug :app:testDebugUnitTest --no-daemon` must succeed.
2. Install the feature-branch debug APK without deleting user data.
3. Confirm that the final **face rendered** matches each chosen face index 0..5, for BOTH cubes.
4. Verify Arabic RTL labels and photo texture orientation (not mirrored or inverted).
5. Exercise all durations, abort/navigation, background/foreground, and editor updates.
6. Profile frame-time p95 on the actual target Android device; evaluate `RENDERMODE_WHEN_DIRTY`.
7. Exercise 20 repeated rolls with photos on all faces; inspect GPU/CPU and memory.
8. Evaluate visual quality directly on screenshots and screen recordings. Do not infer a 90/100 design score from code.
9. Confirm legacy three-dice storage remains unchanged; verify existing encrypted user data.
10. If an unsupported GLES2 environment is encountered, define a graceful fallback before production release.

## Still outside P0-A

- Final Face Studio polish (P0-C): gesture/keyboard UX, independent draft transactions, and discard dialog.
- Global Design System overhaul and final visual acceptance (P0-D). Initial Dice-specific result cards and CTA have been implemented.
- Physically based reflections, optional PBR renderer comparison
- Comprehensive device/accessibility/security acceptance (P0-E)

**No code review, source-only inspection, or GitHub PR status can substitute for the Android build and actual device testing.**
