# M4 — Verification report (source-only)

Date: 2026-10-23. Baseline: M3 copied unchanged first, then extended.

## Performed successfully

- `python tools/generate_content_v2.py --check`: canonical Android/browser/backend content matches, version 2.
- Parsed canonical JSON: 170 distinct Arabic challenges (100 pair, 70 group); 140 distinct quiz questions over 7 × 20. Every question has 3 answer choices.
- Compiled Kotlin **core only**, with the standalone `kotlinc` CLI: `GameLogic.kt`, `ExperienceLogic.kt`, `ExpandedQuizzes.kt`, `AdultChallengesV2.kt`, `MoreQuizzesV2.kt` plus `tools/ContentSmokeTest.kt`.
- Ran `java -jar /tmp/sirr_m4_content_selftest.jar`: **PASS** (7 quiz packs, score balancing across 10 mode/level combinations, no prompts repeated in one session, custom challenge validation).
- Node `--test backend/test/*.test.mjs`: **14 tests passing, 0 failing**. Covers room lifecycle, authority, deduplication, quiz outcomes, content v2, 8-of-20 indexing, encrypted one-use card metadata, limits, and author/recipient permission.
- Node `--check` on backend and browser JS, Python JSON and XML parse.

## Explicitly NOT performed

- No Gradle invocation, APK/AAB compilation, Android SDK, emulator, installation, or UI instrumentation.
- No Kotlin type check of files depending on Android framework, Compose, or OkHttp; standalone core compilation does **not** establish full Android build correctness.
- No live REST/WSS test, HTTP gateway deployment, HTTPS configuration, simultaneous Android/web test, end-to-end AES-GCM cross-language verification or unreliable-network testing.
- No long-term restart persistence on the server. Restoring local credentials depends on an existing server room.
- No independent security assessment or production readiness acceptance.

## Specific implementation limitations

- Card first-download policy is not proof of actual viewing or deletion on the receiver. Lost response may consume a card without usable delivery.
- Android process death can leave draft input unpersisted; only encrypted session credentials are saved.
- Cards remotely support text-only three-whisper / two-layer scratch, not shared photos or soundtrack yet.
- Quiz comparison uses an entertainment score heuristic; agreement is option-index equality, not psychological compatibility.
- Server has access to regular chat/quiz data; shared image/audio/video and card payloads are AES-GCM ciphertext if clients correctly execute encryption.
- The invite URL contains access to the 256-bit content key in its fragment. Sharing the URL with untrusted recipients compromises privacy.
- No claim of prevention of screen recording, camera captures, or forwarding.
