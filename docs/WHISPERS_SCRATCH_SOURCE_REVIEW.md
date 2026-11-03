# R2H-Play — Whispers + Scratch combined source review

**Branch:** `feat/whispers-scratch-closeout-20261103`, based on the Quiz source branch.  
**State:** both features revised at source level; **owner-approved editorial content (2026-11-03), Android compilation and verification pending**. No Kotlin/Gradle build, emulator, APK, JUnit, or device session was executed.

## Product goal

Two distinct, private, local-only experiences for consenting adults:
- **Whispers:** a sender creates a three-message arc; a recipient privately opens each message in order or refuses/erases the sequence.
- **Scratch / اكشف السر:** a sender creates one to three concealed story cards; the recipient reveals them by rubbing away a cover, or chooses the touch-free accessible reveal.

Both remain **offline-first**. Built-in suggestions offer **هادي**, **غزل**, and **حميم 18+**. Adult content discusses sexual desire, intimacy and fantasies in non-graphic Egyptian Arabic without instructing a real sexual act. The existing global age gate is self-attested, not age verification.

## Canonical content and source mapping

- JSON source: `content/whispers_scratch_stories_v1.json`, language `ar-EG`.
- Generated Kotlin: `app/src/main/java/com/r2h/sirr/core/SecretStoryCatalog.kt`.
- Dedicated, scoped generator: `tools/generate_secret_stories.mjs`. It writes **only** the secret-story Kotlin file; it never writes Challenge v10 or Quiz v4.
- Prior inlined banks are preserved in parent Git source and in full side-by-side form in `docs/WHISPERS_SCRATCH_EDITORIAL_REVIEW.json`.
- Each mode has **20** complete stories × **3** chronological lines = **60** lines per mode, **120** total.
- Each mode has 6 SOFT, 7 FLIRT, 7 ADULT stories; all IDs and line texts are unique.
- All 40 new stories (120 lines), IDs, tiers and sequence are **OWNER_APPROVED_AS_IS** and frozen. Canonical Git blob SHA: `fdceffe1210af47a33d86486780088436b424f0c`. Earlier Challenge v10 and Quiz v4 owner-approved content is untouched.

## Whispers — behavior implemented in source

1. Sender may write three original messages (max 350 characters each) or choose a coherent three-message preset from an explicitly selected tier. Choosing a different tier clears previously generated presets, but not manually typed copy.
2. Sender may optionally add names, a signature, an encrypted locally imported photo, and a phone-local audio URI. No file is uploaded by Whispers. Selected audio runs via `MediaPlayer.prepareAsync()`, avoiding synchronous media preparation on the main UI thread.
3. Sender **seals** the messages into an in-memory session with a 3/6/24-hour maximum while the component is active. Handoff screen is shown **before the first message is exposed**.
4. Recipient acknowledges that the content may be private/sexual and may decline and erase without viewing. The first stage cannot display until acknowledgment.
5. Recipient moves through three messages in order. Each previous text is removed from session state on advance. The finale optionally reveals the photo and signature. Close/expiry clears current in-memory session content and deletes the encrypted photo asset.
6. On Activity stop, custom audio is paused or an asynchronous prepare is canceled. On composition disposal, the player is released.
7. Device navigation away discards the temporary flow; the main-app leave dialog now says this truthfully. **No resume-after-process-death promise.** Screenshots, someone photographing the device, and forensic memory recovery are not prevented.

## Scratch — behavior implemented in source

1. Create one to three custom hidden notes (max 280 characters each) or choose a three-part curated story with an explicit tier.
2. For a built-in story, a **single scratch card reveals the last/conclusive line**, rather than an incomplete teaser. Two cards reveal the two concluding stages; three reveal all three in order. Switching the card count re-applies the original preset in correct order.
3. Seal the hidden content, then show a deliberate recipient handoff and acknowledgment before exposing any scratch surface.
4. Each card uses a **bounded 32 × 24 cell mask**, not an ever-growing pointer-stroke list. The percentage is derived from the same marked cells that draw the erased cover. The card reveals after approximately **58%** of cells have been cleared.
5. **Touch-free accessible alternative:** recipient can explicitly click “اظهر السر بدون كشط”. Covered story text uses `clearAndSetSemantics {}`, so screen readers should not be able to retrieve it from the underlying text node before reveal. **Device verification is required** for TalkBack/a11y guarantees.
6. Each new stage uses a fresh mask. Sender/recipient may close the ephemeral session and remove a locally encrypted photo; unlike Whispers, a Scratch card can deliberately be re-covered with “خبّيها تاني”.
7. Local texts are not durably persisted across navigation or process death. The app exit dialog now warns about losing ephemeral progress.

## Tests prepared, not executed

`app/src/test/java/com/r2h/sirr/core/SecretStoryCatalogTest.kt` includes **11** declared tests for:
- 20 × 3 items per mode, unique IDs and 120 unique lines;
- Tier isolation and 6/7/7 distribution;
- One/two/three-layer Scratch payoff order;
- Input validation of malformed presets and tier picker bounds.

**No Gradle, Kotlin compilation, unit/instrumentation tests or Android screen runtime was performed.**

## Remaining editorial/release acceptance

- Owner accepted the current 120 lines exactly as-is on 2026-11-03. Future editorial modifications require fresh approval. Exact before/after remains recorded.
- Compile Kotlin and run the 11 new source tests alongside the wider app suite, *after* the user-requested integrated build phase.
- Test on device: stage handoff, 3/6/24 h expiry, screen rotation, navigation loss, media stop/reselect/prepareAsync, malformed content, photo cleanup, Scratch threshold/drag/tap/multitouch, display size, TalkBack and no-gesture reveal.
- Confirm no automatic release of adult-tier stories into a lighter tier.
- Confirm the newly protected Challenge v10 and Quiz v4 source blobs are unchanged.
- Only merge/release after evidence, not just source assertions.

**Verdict:** Both content banks are owner-approved and frozen; feature source is implemented. They are **not compiled, tested, merged, or production-ready**.

## Approval record — 2026-11-03

The owner approved **all 20 Whispers + 20 Scratch stories** in `content/whispers_scratch_stories_v1.json` as they currently stand. The approval covers all 120 stage texts, story IDs, 3-part order and tier placements. The source text is frozen. Approval is editorial only; it is not evidence that playback, scratch masking, expiration, accessibility, Android compilation or unit tests work on a device. The approval is recorded in `docs/WHISPERS_SCRATCH_EDITORIAL_REVIEW.json`.
