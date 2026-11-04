# Dice P0.3 — Single Renderer & Screen Density Repair

**Status: Source change only. Not compiled, not device tested, no visual approval.**

## Why P0.3
Owner's screenshots from P0.2 confirm that three cubes appear, but materials remain toy-like, vertical padding wastes the first viewport, and results are oversized. P0.2 uses three distinct GLSurfaceView instances and cannot guarantee shared 3D depth.

## Changes
- New `TripleDiceStage3D.kt` owns **one** GLSurfaceView / GLES2 context, six geometry faces per cube, three cubes, **18 owner-authored textures**, one camera, and one depth buffer.
- Real beveled geometry reused from `RoundedDiceMesh`; the canonical face orientation remains `DiceCubeProjection.baseRotation`. Independent roll progress and selected face indices remain under DiceScreen state.
- Shared directional key/fill shader, toned-down specular light, per-cube soft stage shadows (screen-space, stylized, **not physically accurate**). Side-by-side cubes share a depth buffer with front/center cube.
- `TripleDiceStageGeometry.kt` centralizes stage positions and cube-major texture indices; 4 new JVM test cases cover arrangement, non-overlapping model-space footprints, indices 0..17, and invalid indices.
- `MainActivity.kt`: Dice-only header reservation reduced to 12dp instead of 94–126dp; other scenes unchanged.
- `DiceScreen.kt`: replaces three separate cube surfaces with one 314dp-wide stage, visually toned-down primary CTA, and compact 3-result cards that expand only with user content. Face Studio still uses `PersonalCube3D` for individual preview.
- `PersonalCube3D.kt`: previously private face-tile factory now internal so both views share exactly the same owner-authored textures.

## Protected functionality
- No edits to `TripleDiceConfiguration` or `TripleDiceStore` schemas; no re-migration.
- No edits to old two-cube or classic vault storage, approved game decks, weighting, randomness, or user's text and images.
- No new dependency, internet requirement, or generated face content.
- Draft PR must remain unmerged until real device evidence.

## Mandatory verification
1. Run `:app:assembleDebug :app:testDebugUnitTest` from an isolated Worktree pinned to the P0.3 commit.
2. Confirm there is exactly one gameplay GLSurfaceView and a working editor preview; verify correct texture order and no mirror/flipped face.
3. Check all three models and contact shadows render at 360dp and larger, portrait/landscape/RTL.
4. Check all six landing faces on each cube match weighted selections and text result cards.
5. Confirm image import, save, reset and classic mode remain unaffected, including user data.
6. Measure frame pacing and thermals over repeated 20+ rolls and background/foreground switches.
7. Review real screenshots/video before deciding whether the appearance reaches the owner's target (>=90/100). GPU shader compilation and actual screen fidelity are **not** verified by source review.

## Known limitations
- Stylized GLES2 shading, not a PBR material system or ray-traced shadows.
- Screen-space shadow positions are approximations; require visual tuning against real video.
- The new renderer is source-only and may require Android compilation repair.
- Face Studio and global UI design system are not comprehensively redesigned in this phase.
