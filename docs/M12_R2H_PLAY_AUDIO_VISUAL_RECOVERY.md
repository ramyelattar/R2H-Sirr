# R2H-Play — M12 source handoff

Date: 2026-10-25
Branch: feature/m12-r2h-play-identity-sound
Base: feature/m11-immersive-audio-visual
Launcher label: R2H-Play
Preserved applicationId: com.r2h.sirr
Preview: 0.7.0-m12-preview / versionCode 7

## Why

Seven owner device photos of M11 received a user score of 75/100. The actual icon and screens looked insufficiently professional, the page music was reportedly inaudible and the required app name was R2H-Play.

## P0: replace silent audio

- Added eleven original, offline, packaged WAV PCM loops for splash, gate, home, couple, party, dice, quiz, scratch, whispers, remote, settings/privacy.
- Source audio is 16-bit 16kHz signed mono PCM, 4 seconds with soft fades. Home audio verified valid RIFF with peak 0.780 and RMS 0.149 before the playback volume. This confirms non-silent audio resources, not device playback.
- Replaced the runtime-only procedural AudioTrack player with Android MediaPlayer playing bundled resources; one live music player, audio focus, automatic loop, foreground pause, and user-selected Whisper music precedence.
- New-user music default 55/100. Explicit stored settings are not discarded. New-user game sound cues default on, stored user switches remain respected.
- Added top-bar sound mute/unmute, visible player diagnostic status and in-settings music test. User media volume must be audible.

## P0: rebrand and icon

- Android display label in the Manifest is exactly R2H-Play.
- R2H-Play appears in the home header, cinematic splash and adult consent screen.
- New adaptive icon foreground drawable r2h_play_foreground.xml features luminous R/play monogram in gold and magenta with dark geometric frame; used on the launcher and Android 12 system splash.
- Application package deliberately remains com.r2h.sirr, preserving installed app data and Android signing compatibility.

## P1: visual hierarchy

- Replaced the giant flat-home scroll layout with a compact cinematic feature card and a distinctive two-column six-game dashboard immediately available to the user.
- Strengthened legibility and selected-state contrast, reduced atmospheric lavender glare.
- Added six illustrated intro motifs: couple, party, dice, quiz, scratch and whispers.

## Safety and validation

- The user does not want GitHub Actions testing. No GitHub CI attempted.
- Source patches are committed on an isolated M12 branch; M11, M10, M9 and main unchanged.
- The M12 APK has NOT been built or run on an actual device here. Non-silent PCM and structurally plausible Kotlin do not guarantee device audio. Do not claim 90 percent visual acceptance.
- Build script installs only with adb install -r, and never clears app data or uninstalls.

## Windows handoff

Change directory to E:\Projects\R2H-Android\R2H-Sirr, not R2H-18.
Fetch origin feature/m12-r2h-play-identity-sound, create a local branch from FETCH_HEAD, then run tools/Build-Install-M12.ps1 with DeviceSerial 00170156M000568.
Do not force or reset local modifications. If the local branch already exists, switch to it and fast-forward pull instead.
After installation, inspect launcher label and icon, home layout, audible soundtrack in home/couple/dice/whispers/settings, sound effects, volume/mute toggle and background/foreground behavior.
