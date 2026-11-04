# Dice P0.4 — Screenshots-Based Visual Defect Repair

Status: **SOURCE ONLY / NOT COMPILED OR TESTED ON DEVICE.** Owner screenshots from the P0.3 phone preview informed these changes.

## Actual observations from 2026-11-04 screenshots
- Three cubes render, and the gameplay result has three separate cards.
- The label row does **not align** with the visual placement of the cubes. The labels read in UI layout order, while the 3D stage positions cubes in its own coordinate system; this is a concrete mapping/UI defect.
- Champagne/pearl pale surfaces and thin white face lines appear excessively bright; perceived as toy-like. Camera exposure can magnify the issue, so color correction is provisional until direct screen capture is inspected.
- Short user-authored face text is too small to read comfortably in the photographs.
- The stage occupies substantial vertical room, and a material-label row adds visual clutter without providing functional information.
- The first photo shows the stage top clipped while the screen is scrolled. **That alone does not prove renderer clipping**, since the second photo shows the full stage.

## Scoped changes
1. Remove the misleading material legend below the 3D stage; retain cube numbering in the results and editor.
2. Reduce the stage height (314dp -> 306dp), do not alter the proven landing-face index or layout coordinates.
3. Darken all three face textures, especially Pearl Rose and Champagne, to reduce pale-surface blowout.
4. Remove the 5px high-alpha white face border; retain a subtle 2px low-alpha seam.
5. Increase owner-authored short-face typography; limit photo-overlaid text sizes to avoid overflow.
6. Reduce shader diffuse/ambient energy and specular reflection, with sRGB-safe clamping.
7. Rebalance primary CTA background to a deeper plum with light text.
8. Add subdued numeric result identifiers and shorten local privacy helper text.

## What this phase does NOT prove
- No confirmed improvement in FPS, shader compatibility, thermals or text orientation.
- No measured visual score or owner acceptance; review actual screenshots and a roll video after building.
- It does not convert GLES2 stylized materials to physically-based rendering.
- No Android unit tests or builds have been executed for this branch yet.

## Verification checklist
- Gradle :app:assembleDebug + :app:testDebugUnitTest.
- Install without wiping data; all 18 owner-authored faces, saved results and Classic Dice still intact.
- All three cubes legible; compare same device brightness with P0.3.
- Face Studio preview remains consistent with main stage colors and content.
- Verify no wrong material labels; verify results map each cube 0, 1, 2 to correct chosen face.
- Check longer Arabic text and photo+text combinations for clipping.
- Repeated roll smoothness and foreground/background lifetime; no GL regressions.
