# R2H-Play — Dice P0.5: grounded 3D scene and roughness-aware materials

**STATUS:** Source implementation; Android build, shader compilation, CPU/GPU frame timing, unit tests and phone visuals **NOT VERIFIED**.

## Motivation
Owner's P0.4 photos show 3 cubes still appearing suspended inside a black rectangle with insufficient contact cues, a relatively flat roll CTA, and materials resembling plastic. More gradient/glow tuning will not solve the scene geometry.

## Implemented
1. **One world-space floor** inside the existing shared GLES2 scene, drawn using the same view/projection as all 3 cubes. Unlike the former screen-aligned fake glow, the contact darkening is computed from actual cube x/z positions in the floor fragment shader.
2. **Shared ground contact:** `TripleDiceStageGeometry.FLOOR_Y=-1.05f`. Two rear cubes are positioned at x±0.81, z=-0.62; the front hero cube is larger and placed at x=0, z=0.70. All objects have independent sizes and rest on the same floor.
3. **Rotation-aware support:** `restingCenterY(rotation, scale)` finds the worst vertical corner extent of a rotated cube by summing the absolute 3×3 rotation Y components. A small in-flight rise (0.15f * sin(pi * progress)) is added during roll. This prevents mesh corners penetrating the floor while preserving deterministic landing faces.
4. **Camera adjusted** to (0, 1.65, 4.25) looking toward (0, -0.44, 0), allowing rear cubes and front cube to appear separated by true 3D perspective rather than arbitrary screen Y offsets.
5. **Material lighting:** GLES2-compatible lightweight GGX normal-distribution + Schlick geometry/Fresnel approximation, cube-specific roughness and low metallic factors. Textures still contain only user-authored content; this is **not full PBR, IBL, reflections, or hardware ray tracing**.
6. **World-space contact occlusion:** soft analytical darkening around the 3 cube footprints. Not real shadow mapping; it is stylized local shading, with softness responding to the roll's lift.
7. **Roll button:** subdued deep plum surface, thin gradient border, higher text contrast and restrained elevation.
8. Replaced stage unit tests (6 tests declared) for shared plane/rotation contact, footprint separation, texture mapping 0..17 and invalid inputs. Existing mesh/roll and encrypted vault remain unchanged.

## Hard safeguards
- No modifications to `TripleDiceConfiguration`, `TripleDiceStore`, `PersonalDiceStore`, `GameLogic.weightedFace`, vault, old Classic Dice storage, user-authored text/photos, or approved game banks.
- One gameplay GLSurfaceView retained. Face Studio still has one independent preview.
- No new 3D native dependency, no cloud, no added content or photo assets.
- New P0.5 branch is stacked on draft PR #39 and must not be merged to `main` without build + device acceptance.

## Required verification (not run)
1. Compile `:app:assembleDebug`; check **both GLES2 vertex/fragment shader programs** link and render on the target phone (Android build alone does not compile runtime GL shaders).
2. Run `:app:testDebugUnitTest` and inspect JUnit XML result counts.
3. Check all three cubes are visible, have natural separation and sit on the same floor across six selected faces and during varied spin durations.
4. Compare before/after at the same screen brightness using an Android screenshot, not just a camera photograph.
5. Inspect contact AO under each cube during lift and landing. Watch for sharp floor boundaries, excessive darkness, false clipping, and implausible hovering.
6. Validate Face Studio screenshots, Arabic text, long strings, user photos and 18 texture mapping.
7. Stress test repeated rolls; use real frame-time/performance/thermal data before claiming efficient 60fps on Snapdragon-class hardware.
8. Confirm no errors in lifecycle (background/foreground, rotate if supported, leave/re-enter), local storage and legacy mode.

## Known tradeoffs
- Ground occlusion is a physically **placed** approximation, not cast shadows from a real light source.
- A flat floor and a GGX-inspired direct-light BRDF are not physically complete material rendering. Actual appearance depends on screen calibration.
- Fragment precision is mediump for broad GLES2 compatibility; some devices may show banding or precision issues.
- Calculating rotation-based support each frame is inexpensive in theory but has not been profiled; no measured FPS or thermal numbers.
- Saved photo tile bitmaps and 18 GPU textures have not been profiled on the target hardware.
