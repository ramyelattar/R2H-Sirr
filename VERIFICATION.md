# M7 — Current verification (24 October 2026)

- `python tools/generate_content_v2.py --check`: PASS, version 3, 230 challenges and 175 questions.
- `node --test backend/test/*.test.mjs`: PASS **20/20**, including M6 chunk storage and M7 content/quiz aggregation.
- `kotlinc` standalone core and `tools/ContentSmokeTest.kt`: PASS, 175 original Arabic questions, session fairness.
- `kotlinc` standalone core and `tools/M7SmokeTest.kt`: PASS, 6/10/12/18 round counts, difficulty mix, recent history avoidance, descriptive response aggregation.
- `node --check` for backend, web and test JavaScript: PASS.
- Android/Compose/OkHttp full compile: **NOT PERFORMED** (user requested no build).
- Device/browser live E2E, deployed relay HTTPS/WSS, content-version rejection over actual WebSocket: **NOT VERIFIED**.
- No independent security audit or production readiness acceptance.

---

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

## M7.1 — Remote customization and effects (2026-10-24)

- Node suite: **25 passed, 0 failed** (five M7.1-focused tests).
- Canonical content integrity: **PASS** (230 challenges / 175 questions, v3 unchanged).
- JS syntax checks: **PASS**.
- Pure Kotlin-core smoke tests: `ContentSmokeTestKt` **PASS**, `M7SmokeTestKt` **PASS** (standalone `kotlinc`, NOT Android compile).
- Chromium navigation attempt: **blocked** by `ERR_BLOCKED_BY_ADMINISTRATOR`, hence no browser visual pass.
- Android Gradle / APK / device / relay HTTP/WSS / 2-client E2E: **NOT RUN**, still not accepted.
- Important privacy boundary: remote custom prompts and replies are not E2EE; server sessions are RAM-only.
