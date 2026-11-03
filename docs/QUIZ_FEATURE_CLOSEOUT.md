# R2H-Play — Quiz: adult-content and gameplay source review

**Status (2026-11-03):** Updated draft with real adult sexual-topic questions, source-side logic for three quiz modes; **NOT owner-approved, compiled, tested, installed, or merged**.

## Product mismatch corrected

The previous bank presented itself as daring/adult but contained mostly general romance, icebreakers, and relationship-advice prompts. This failed the owner's expectation that an 18+ couple's game includes genuinely sexual subject matter.

This review now explicitly covers **sexual desire, intimacy, preferences, erotic fantasies in non-graphic terms, differing libidos, safer communication, boundaries and consent**. It does not depict sexual acts graphically, and a question never requires an actual sexual activity to be performed.

## Canonical editorial state

- `content/quizzes_ar_v4.json`: 7 topics × 25 questions = 175 prompts, with 3 answer choices each = 525 options. `version: 4` stays unchanged for existing consumers.
- 92 questions were rewritten in the **adult-content pass**, leaving 83 unchanged from the prior Quiz branch baseline.
- Breakdown by topic: quiz_01 **0** (lighter personal intro), quiz_02 **9**, quiz_03 **10**, quiz_04 **16**, quiz_05 **19**, quiz_06 **18**, quiz_07 **20**.
- The original 43-question focused rewrite in the preceding draft remains historically documented in `docs/QUIZ_V4_EDITORIAL_CHANGES.json`.
- Every change from the immediate pre-adult version, including old and new question text **and** all 3 choices, is recorded in `docs/QUIZ_ADULT_CONTENT_CHANGES.json`. Baseline: commit `27779839a53247e67ffbc81589ea28c8c74202d9`.
- The existing topic sequence and counts are preserved, and the light introductory pack is deliberately kept separate from the other six adult-topic packs.
- Gender-neutral, actor-relative question choices continue to be avoided when comparing two people: matching choice indices must represent the same preference to each player, not opposite referents like "me" and "my partner."
- New content is **not owner-approved yet**. No implied acceptance by the owner merely because it was written.

### Canonical source synchronization

- `content/quizzes_ar_v4.json` — source of truth.
- `app/src/main/java/com/r2h/sirr/core/EgyptianQuizCatalog.kt` — generated Kotlin.
- `backend/public/quizzes.json` — public JSON mirror.
- `tools/generate_content_v2.py` — existing generator. It also regenerates historical challenge assets: do not execute in write mode indiscriminately, especially while challenge-v10 content is frozen.

The Kotlin and public mirror were updated with the canonical source. A static text comparison is required after further edits.

## UI disclosure and gameplay

- The entry screen now identifies the questionnaire's adult sexuality focus, rather than misleading users into expecting generic dating trivia.
- Topic `quiz_01` remains a lighter personal pack. The other six topics include explicit discussion of sex (non-graphic), intimate preference and sexual boundaries.
- The UI introduces an explicit adult-content acknowledgement (18+) and affirmative per-session consent before play. **This is a self-attestation UI, not verified age assurance.**
- All questions are optional. Either player may skip or end the session without penalty or enforced sexual activity.
- Three source-side quiz modes: SOLO, ordinary paired descriptive comparison, and privately predict-partner-answers before partner completes their own round.
- Only comparable answers (both non-skipped) count toward ordinary comparison or prediction accuracy. The result is not a relationship compatibility score.
- `QuizSession.kt` keeps staged state (FIRST, optional PREDICT, HANDOFF, SECOND, RESULTS) and preserves exact question/choice snapshots.
- `QuizSessionStore.kt` saves encrypted, versioned (v2) local snapshots and can read v1 stored rounds. Private answers are not placed in Compose `savedInstanceState`.
- No changes to the already approved Couple Challenges v10 on its separate parent branch.

## Source review and verification limits

The project has source-level Kotlin tests: **20** `QuizSessionTest`, **6** `QuizContentV4Test`, and **3** existing `QuizInsightsSkipTest` declarations, a total of **29**. Their presence does not demonstrate successful execution.

**None of the following has been run in this editing session**: Gradle compilation, Android/JVM tests, instrumented encrypted-vault restore/clear tests, APK generation, installation, emulator or on-device verification.

Outstanding acceptance gates:
- Owner editorial review and explicit approval of the new adult-content bank and its product tone.
- Compile all Kotlin/Compose sources and execute the new and existing unit tests.
- Test secure local storage round trips for v1/v2 sessions, interruption, corruption and process death.
- Test paired consent/handoff, skip/back, prediction results, RTL/TalkBack/accessibility and age-content presentation.
- Verify old remote quiz compatibility and regression safety for the rest of R2H-Play.
- Only merge when the reviewed feature has real evidence and acceptance.

**Assessment:** Editorially corrected adult topic coverage, with content and supporting code staged in Draft PR #33. Not a finished, verified APK.
