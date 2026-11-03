# R2H-Play — Two-Cube Dice feature, owner-authored content only

**Source branch:** `feat/dice-two-cube-user-content-20261103`  
**Parent:** `feat/whispers-scratch-closeout-20261103`  
**Status:** source implementation staged, **not built, compiled, device-tested or owner-accepted as a UI**.

## Exact requirement

The user provided five reference images depicting **two realistic rounded cubes** with writing and optional artwork on each face. User clarification explicitly prohibits generating images or authoring any sexual/game content: **the user will create every face label and supply every optional face photo**.

This branch includes **zero pre-authored face labels, icons, suggestive prompts or artwork**. Decorative cube colors and materials are UI styling, not authored game content.

## Default two-cube UX

- The existing `DiceScreenV2` navigation target now presents **two independently configured six-sided cubes** (12 faces).
- Cube 1 is glossy pale lilac; cube 2 is metallic silver, with a rounded 3D-like appearance matching the photographed concept.
- A native Canvas-based renderer rasterizes each face from **user-supplied label or image only** and applies a perspective matrix to three visible sides, without a 3D engine, OpenGL dependency, cloud processing or generated assets.
- A 3D rotation model maps the randomly chosen **actual face index** to the cube's front orientation. Each cube spins about X/Y multiple revolutions and settles on the selected face, with an easing curve. Reduced-motion preference switches to an instant reveal.
- The selected faces are shown separately below the cubes. **No fabricated combining sentence, implied action or automatically generated text.**
- Each face supports up to 72 characters of custom user text, optional user-selected local photo and independently editable integer percentage weight.
- All 6 weights for each cube must sum to 100 before rolling; any face with positive probability must contain user-provided text or a photo. Incomplete drafts are allowed and can be encrypted/saved without allowing a roll.
- Six uniformly approximate default weights are 17/17/17/17/16/16. Faces are deliberately **blank**; there is no demo or placeholder text embedded into any side.
- The user chooses animation duration from the existing supported 2/3/5/7/10/13/20 seconds. Existing sound/vibration and reduce-motion settings continue to be respected.
- Every roll is computed by the same existing weighted-selection utility `GameLogic.weightedFace`, once per cube, independently.

## Storage and photo handling

- Separate `PersonalDiceStore` key: `dice-pair-v1` in existing `LocalVault` (Android Keystore AES-GCM + AtomicFile).
- `DiceStorage` and its original `dice` vault key remain untouched, so the older three-dice saved configuration is readable as before.
- The new UI can open the old 3-dice implementation as `LegacyDiceScreen`, copied with the exact old source logic. New blank cubes do **not** inherit historical preset game content.
- Face photo imports are user-initiated, re-encoded/metadata stripped by the existing `LocalVault.importPhoto`, then saved encrypted. A photo change first commits the new configuration before deleting the old image ID; a failed save cleans up the new import.
- `LocalVault.readPhotoThumbnail` decrypts a user photo in memory, bounded to about 384px before rendering a cube face. Original encrypted photo bytes remain unchanged and no disk thumbnail is generated.
- On explicitly clearing cube customization, only the two-cube config and its referenced photos are deleted; the legacy 3-dice vault and unrelated app data remain untouched.
- Unreadable/corrupted configuration is **not** silently overwritten; the UI offers an explicit delete/recover choice.
- User edits must be saved before navigating away; the app's Dice leave confirmation reflects this.

## Files

- `core/PersonalDiceConfiguration.kt`: validates 2×6 owner-authored faces and rolls using existing weighted mechanism.
- `core/DiceCubeProjection.kt`: independent, pure mathematical cube vertex projection/orientation, including selected-front mapping.
- `PersonalCube3D.kt`: Compose Canvas rendering of user text/user image on projected rounded faces.
- `PersonalDiceStore.kt`: encrypted local v1 persistence.
- `DiceScreen.kt`: default two-cube gameplay and editor.
- `LegacyDiceScreen.kt`: legacy 3-cube UI preserved as an accessible secondary mode.
- `Security.kt`: added bounded thumbnail helper only; no original storage format modification.
- `MainActivity.kt`: custom Dice exit warning only.

## Source tests written, **NOT run**

- `PersonalDiceConfigurationTest.kt`: **9** declarations for blank/default content, editable faces, weighting, roll behavior, input bounds and old-mode isolation.
- `DiceCubeProjectionTest.kt`: **4** declarations for six final face orientations, full spins, finite geometry and invalid inputs.
- Source checks do **not** verify visual equivalence, Kotlin compilation, persistent storage reliability or Android frame rate.

## Remaining acceptance gates

1. Compile Android/Kotlin/Compose and execute the above 13 unit tests, existing dice grammar tests, and full regression suite during the later global build phase.
2. On a target Android device, inspect two-cube bevels, lilac/silver gloss, Arabic labels on faces, perspective orientation for all six sides, rotation smoothness, and visible roll/result parity.
3. Exercise all editor controls, photo selection, permissions, input limits, every supported duration, partial drafts, weight validation and error feedback.
4. Instrument encrypted saved/restored settings after process death, corrupted/invalid ciphertext, old `dice` persistence, photo replacement/removal, explicit reset and interrupted atomic writes.
5. Verify memory/CPU/battery during repeated animation and photo-heavy faces, screen dimensions 360/393/412dp and font scale/RTL accessibility.
6. Verify user device photos are never copied to the server, that no generated content appears, and that approved Challenge v10/Quiz v4/Whispers/Scratch content is unchanged.

**Source status only:** complete first implementation of the requested two-cube feature, with actual correctness and design acceptance pending later Android testing. No APK has been built or installed.
