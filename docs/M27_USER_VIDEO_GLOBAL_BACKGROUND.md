# R2H-Play M27 — User-Supplied Animated Background, All Routes

**Source implementation: complete on feature branch; Android build and phone validation NOT YET EXECUTED.**

## Grounded media

The owner uploaded `WhatsApp Video 2026-11-04 at 12.23.06 PM.mp4`, a portrait cosmic purple/magenta abstract bubbles/particles animation. The media was inspected before integration; it does not include branded text. The source is 478×850, 24 fps, ~10.05 seconds, H.264 with an AAC audio track.

A version designed for continuous background display was prepared **without any audio**:
- `app/src/main/assets/r2h_cosmic_background.mp4`: 478×850, H.264, 20fps, ~10.05s, 796,774 bytes. SHA-256: `29B24049B701975145A0C73EAB31AE53698CE4B25CE2B1A333C2175D35EBE0E2`.
- `app/src/main/res/drawable-nodpi/r2h_cosmic_poster.webp`: one still frame from the same clip, 24,060 bytes. SHA-256: `237CF7DEB6CEE70DE290B4AFA8C15DEAB17DB777D2B78C4C70D822EBE6366DDB`.

**Binary assets are distributed in the local M27 installation ZIP, not yet committed to GitHub.** Do not claim the video appears in a GitHub-only APK without these files. The build script must check both hashes and verify asset paths before Gradle. The owner can subsequently commit the media files to this feature branch after successful review.

## Implementation

- `MainActivity.SirrApp` already calls `SirrAmbientBackground` exactly once, behind the scene route UI. That single root site covers Splash, Adult Gate, Home, Couple, Party, Quiz, Dice, Scratch, Whispers, Remote, Settings, Privacy.
- Replaced the old M25 static branded wallpaper + artificial edge particle animation with `R2HVideoWallpaper` in `SirrAmbient.kt`.
- New `R2HVideoWallpaper.kt`: one silent looping `TextureView` / native `MediaPlayer`, center-cropped to screen. No network requests and no new dependency. Poster rendered underneath, and a gradient vignette above to maintain legibility of controls.
- `TextureView` is used **instead of a `SurfaceView`** to avoid drawing on top of Compose dialogs/cards. Video is outside the route-specific subtree, so navigation does not recreate playback.
- `MediaPlayer` pauses on Activity lifecycle stop, is released on disposal/detach and is skipped when `cinematic` is disabled or Android animations are globally disabled.
- While the owner plays media inside Whispers or another screen, the background decoder is suspended and the still poster remains. No audio focus is requested; the video variant has no audio track and playback gain is zero.
- `androidResources.noCompress.add("mp4")` ensures `assets.openFd` provides the segment as a file descriptor.
- The previous static wallpaper reserved 94–126dp for baked-in R2H letters. This video does **not** have baked-in branding; global header is 12dp on every route and the home toolbar now displays `R2H-Play` explicitly.

## No-media and reduced motion paths

- Without media in the worktree, the app compiles and uses a dark gradient. **This is NOT successful implementation of the owner's requested video in the APK.** The M27 build script rejects missing or checksum-mismatched media before building.
- With video present but motion disabled, a static poster from the same video appears.
- Video never includes user-authored game content, camera, microphone or personal local-vault data.

## Verification gates

1. `app/src/main/assets/r2h_cosmic_background.mp4` and `res/drawable-nodpi/r2h_cosmic_poster.webp` exist and exact SHA-256 hashes match above.
2. `:app:assembleDebug`, `:app:testDebugUnitTest` exit 0 and the generated APK actually contains `assets/r2h_cosmic_background.mp4` and the poster image resource.
3. Install with `adb install -r`, **never uninstall or clear app data**.
4. Inspect moving background on splash, adult gate, home, every game, remote, settings and privacy; navigation retains playback and no white flash appears.
5. Confirm muted video does not suppress page music; app background/foreground pauses/resumes correctly.
6. Check effect preference `cinematic=false`, system animator scale=0 and playback of owner media to ensure stationary poster and no unnecessary decoder.
7. Verify a bright scene doesn't reduce contrast, keyboard and bottom sheets still draw above background, and no Video/GL surface stacking fault occurs in Dice.
8. Measure CPU/GPU/thermals and power over 20+ minutes on target midrange Android device.

## Boundaries

Do **not** change approved challenges, quiz or story content, photo storage, encryption, sync, dice schema or random logic for this background feature. This PR is stacked on #40 and must not merge into main before device verification and media availability.
