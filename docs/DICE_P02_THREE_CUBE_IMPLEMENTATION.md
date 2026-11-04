# R2H-Play — Dice P0.2 (Three Premium Cubes)

**Status: SOURCE IMPLEMENTED ON STACKED BRANCH; NOT BUILT, TESTED OR DEVICE-VERIFIED.**

## Owner change request

- Three physical cubes, not two.
- Dark premium materials with better stage composition, avoiding toy-like colors.
- 18 owner-authored faces and three independent result cards.
- Keep photos/text exclusively owner-authored; generate no game content.
- Preserve all user data and old modes; protect the known-good main branch.

## Architecture

New `TripleDiceConfiguration` requires exactly 3×6 faces and 100% per-cube weights to play. Incomplete drafts are valid for saving. Three independently weighted face indices are selected using the existing `GameLogic.weightedFace`.

New `TripleDiceStore` writes solely to AES-GCM key `dice-triple-v1` and reads the existing `dice-pair-v1` only when there is no triple record. Both old faces, image IDs and selected duration are copied **in memory only**, and the third cube is blank. An explicit save (including photo upload) persists the triple. The old pair and legacy keys are never modified.

**Caveat:** Deleting or replacing a triple image does not erase underlying encrypted photo bytes: prior pair data might reference them. Storage compaction must be implemented only with reference-aware garbage collection across all dice versions. Reset writes a new blank triple record, preventing unintended pair re-import.

## Renderer and UX

- Reuses actual GLES2 bevelled mesh from P0-A, without heavier libraries.
- Three materials: Deep Plum, Pearl Rose, Champagne with distinct gradients, normals/specular and contact-light hues.
- Triangle-like presentation: two nonoverlapping upper cube GL surfaces, a central lower/front cube GL surface. No transparent overlay of independent Android `GLSurfaceView` buffers.
- Three staggered roll animations with distinct full-turn axes and a deterministic final selected face.
- Three owner-textured result cards and a dedicated Face Studio with three cube tabs × six selectable faces.
- The legacy 3-dice mode remains unchanged.

## Verification gates — **NOT YET EXECUTED**

1. `gradle :app:assembleDebug :app:testDebugUnitTest` — compile & unit test output required.
2. Ensure first run copies only first two owner-authored cubes and leaves 3rd blank.
3. Confirm old `dice-pair-v1` and legacy storage unchanged across edit, save, photo removal and reset.
4. Confirm three independent weighted rolls, all 6 faces each and final render matches result.
5. Confirm Arabic/English label orientation on all three cubes, with photos and blank faces.
6. Stress 20+ rolls, foreground/background, screen changes, photo import and editor save.
7. Capture real Android screenshots for Stage, Face Studio, results; **visual rating is not inferred**.
8. Profile GL contexts and GPU/CPU/memory/thermals on target device. Up to 3 GLES2 surfaces plus editor preview is an initial trade-off, NOT established optimal.
9. Test landscape, compact 360dp viewport, RTL, font scale, TalkBack and Reduce Motion.
10. Verify no default/generated game-face text/photos and no changes to approved banks.

## Known limitations

- Renderer uses stylized lighting, not physically based reflections.
- Face Studio photo import/remove currently persists immediately; other draft edits remain in-memory until save. Dedicated atomic draft/photo cleanup remains a future hardening task.
- The triangle uses non-overlapping `GLSurfaceView` rectangles, not a single scene with real inter-cube occlusion/shadows. Single-scene renderer is a possible later upgrade, subject to profiling.
- Final design acceptance and production merge remain blocked on actual device verification.

**The source review is not evidence of Gradle compilation or achieved 90/100 visual quality.**
