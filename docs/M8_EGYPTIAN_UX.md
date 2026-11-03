# M8 — Egyptian Arabic + Mystery-first Android UX

## Brief
- Arabic UI written as everyday Egyptian colloquial speech, not formal Arabic.
- A mysterious, inviting adult-only visual identity; sensual in mood without explicit imagery.
- A clear primary action on the first screen and progressively disclosed settings.
- Private, consensual games: skip/stop controls and explicit consent remain visible.
- Avoid promotional counts, implementation jargon, or technical descriptions in core gameplay.

## Source changes
- `MainActivity.kt`: dark wine, ink and champagne palette; new welcome gate; hero-first home screen; feature hierarchy, visual rings and differentiated cards; privacy copy.
- `ExperienceScreen.kt`: choose mood and rounds before starting; advanced settings, custom prompts and participant names are collapsed; consent, skip and stop remain available; one challenge per round.
- `QuizScreen.kt`: Egyptian colloquial headers and one question per step, with large response controls.
- `GameEffects.kt`: motion, sound, vibration preferences are collapsed by default, with reduced-motion behavior preserved.
- `DiceScreen.kt`, `ScratchScreenV2.kt`, `WhispersScreen.kt`: Egyptian colloquial microcopy while preserving game behavior.

## Voice rules
- Prefer: "يلا نبدأ", "مش دلوقتي… عَدّي", "سيبها على النرد", "اختار المود", "خدوا راحتكم".
- Avoid mechanical/formal headings: "إكمال التحدي", "تنتهي صلاحية العرض", "الإعدادات السينمائية".
- Keep instructions short and decisive. Never imply participation is obligatory.
- No misleading claims that privacy features prevent forwarding, photographing, or a recipient keeping media.

## Acceptance checkpoints — NOT YET VERIFIED
- Build `feature/m8-egyptian-experience` with Gradle only after owner opts for another Android test.
- Visually inspect 360dp and larger phone screens, RTL layout, long Egyptian Arabic labels, font scaling and safe-area insets.
- Verify options, consent, custom prompts, game round progression, reveal interactions, audio, haptics, privacy, and WebSocket flow remain functional.
- Photograph/render home, consent, couple setup, one challenge and quiz. Check against image references before calling UI release-ready.

## Remaining design work
- Dedicated 3D dice/artwork, scratch media scene and personalized gift reveal.
- Remote room UI, deeper Arabic copy editing of remaining screens and content itself.
- Native typography/fonts and asset system; accessibility contrast; RTL motion.
- Better original content with coherent play progression rather than number of generic prompts.
- Full Android compile, UI test and device acceptance. The changes in this branch are **source-only**.


## Second iteration — real gameplay implementation
- Added `SirrVisuals.kt`: reusable native mystery hero, faux-3D dice, custom secret-letter reveal, immersive challenge card, and three-act round chapter cue.
- `DiceScreen.kt`: dice scene above settings, default three-second roll, one prominent action, all advanced controls and 18-face custom editor collapsed by default. Custom durations now include two and three seconds; unchanged die weighting/storage.
- `ScratchScreenV2.kt`: mystery scene, one-tap locally generated Egyptian secret suggestions, progressive reveal stage and optional image/expiry settings concealed by default.
- `WhispersScreen.kt`: three-step letter composer, opt-in one-tap suggested messages, private letter reveal, optional audio/photo/recipient controls collapsed. Existing one-use semantics retained.
- `RemoteScreen.kt`: Egyptian lobby copy, optional host configuration and attachment controls collapsed; HTTPS/WSS restriction preserved.
- `GameEffects.kt` / `ExperienceScreen.kt`: challenge scene in place of default grey cards, three-act chapter labels.
- `content/challenges_ar_v2.json`: rewrote the first ten prompts across five difficulty levels in each of couple/party into 100 unique Egyptian mini-games; prioritization now favors those prompts in short local and remote sessions. 130 legacy fallback prompts still need editorial revision.
- `content/additional_quizzes_ar_v2.json`: rewrote the first ten additions in each of seven packs (70 questions). Existing 105 remaining questions still need Egyptian editorial revision.
- Synchronized generated Kotlin and backend JSON mirrors without changing the agreed `contentVersion=3` / `protocolVersion=4` wire schema.
- New backend test: `backend/test/m8-experience.test.mjs` verifies curated prompt coverage, default 10-round content, and uniqueness for 18-round mixed sessions.
- Checked generated challenge and quiz mirror content against source: all matched, 230/230 challenge texts unique and seven packs of 25 questions.

### Verification and risks
**No Android/Gradle build, emulator, deployed backend, or visual device test has been run for this branch.**
The new Node test file was added but has **not yet been executed** against the GitHub branch.
The Kotlin UI changes have **not yet been typechecked with Compose**; UI/runtime regressions are possible.
An actual device pass is still required for all layout sizes, RTL, transitions, scratch gestures, audio, privacy and saved-state behavior.
This is not release-ready. The earlier M7 device build succeeds, but that result does not validate M8.

### Further design work (superseded by latest M8 catalog revision)
- All 230 challenges and all 175 quiz questions have now been edited to everyday Egyptian Arabic and synchronized in the canonical v4 sources.
- Both native Kotlin gameplay and browser/relay use the v4 catalog; old clients with contentVersion 3 are rejected.
- The app still requires a new Android/Compose compile, visual phone acceptance, and real multiplayer tests.
- Full 3D dice, polished tactile/audio assets, and a truly premium scratch-card scene require visual and device refinement.

## Editorial closeout — 24 October 2026
- Source-of-truth challenges: `content/challenges_ar_v2.json` (catalog version **4**, legacy filename).
- Source-of-truth quizzes: `content/quizzes_ar_v4.json` (seven packs × 25 questions).
- Native runtime now uses `EgyptianQuizCatalog.kt` via `ExpandedQuizzes.packs`; the generated Android Kotlin and web/backend JSON copies were checked for exact text consistency.
- Challenges: **230/230** unique Egyptian prompts; quizzes **175/175**, three complete choices each.
- Backend contract: `contentVersion=4`, `protocolVersion=4` with incompatible earlier clients rejected.
- All remaining M7 corpus material is retained only in historical source/test files, not in active M8 quiz selection.
- Tests amended for v4. Created `.github/workflows/m8-source-tests.yml` to run Python catalog checks and Node tests without Gradle.
- **CI startup status:** the first run ended `startup_failure` with zero jobs. No passing tests or Kotlin build should be inferred from that.
- **Screen validation:** updated Arabic type scale for Android, but no new Android build or user-device runtime check was run.


## Gameplay QA follow-up (24 Oct 2026)

The M8 refinement pass made the following source changes **without generating or installing an APK**:

1. **First-run friction:** user confirmation of being 18+ and agreeing to the rules is stored in encrypted local `LocalVault` after explicit opt-in; later launches do not repeat the same declaration. The Privacy screen's Clear Data action erases it. This remains self-attestation, **not independent age verification**.
2. **Scratch:** finger drags interpolate intermediate points (no longer leave long skipped gaps), the scratch-progress approximation uses a 16×12 grid, and gesture recognition is rebound after the canvas receives its real dimensions. The amount of cover cleared is indicated in the Arabic interface.
3. **Two-player quiz handoff:** fixed the condition that made the second player's consent screen disappear immediately after checking “أنا موافق أبدأ”; it now remains visible until Start is pressed. Results reveal one shared question and both answers at a time, instead of presenting raw choice tallies as compatibility results. Agreement remains purely descriptive.
4. **Dice:** every preset face text uses Egyptian colloquial speech; three dice are identified as action / idea / manner and produce **one complete, prominent prompt** instead of three separate word cards. Custom image faces remain supported.
5. **Small screens:** quiz round-count chips scroll horizontally and the top back arrow points in the RTL direction.
6. **Regression checks:** `tools/check_m8_ui_source.py` guards these previously defective source paths and has been added to `.github/workflows/m8-source-tests.yml`.

**Verification:** GitHub-file static checks for these exact changes were carried out. This is **not** Compose type-checking or an actual device test. GitHub Actions previously returned `startup_failure`; until Actions runs jobs successfully, new checks are **not CI-approved**. Neither `main` nor M7's installed APK was changed.

Acceptance still requires Android compilation and layout/device runtime inspection on narrow and larger displays.
