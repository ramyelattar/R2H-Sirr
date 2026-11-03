# R2H-Play Quiz — Source close-out, owner-approved copy

**Owner editorial decision:** `OWNER_APPROVED_AS_IS` on 2026-11-03.
**Source implementation:** `SOURCE_COMPLETE_VERIFICATION_DEFERRED`.
**Release/build state:** Uncompiled, untested, unmerged Draft PR #33. **Do not label this a verified release.**

## 1. Frozen editorial scope

The owner explicitly approved the current adult-themed bank:
- Canonical source: `content/quizzes_ar_v4.json`.
- Approved content blob SHA: `fd35caee10e9159fd27f7bc85c28f5bfcaa7cd78`.
- 7 packs, exactly 25 questions each = 175 prompts and exactly 3 answers per prompt = 525 choices.
- Pack 1 (`quiz_01`) is a lighter personal introduction. The other six packs directly discuss sexuality, sexual desire, intimate preferences, fantasies in non-graphic terms, privacy and mutual consent.
- 92 adult-theme edits from the preceding Quiz baseline; 83 entries preserved from that baseline; exact before/after preserved in `docs/QUIZ_ADULT_CONTENT_CHANGES.json`.
- Earlier 43 focused edits from the initial Quiz revision remain documented in `docs/QUIZ_V4_EDITORIAL_CHANGES.json`. This historical diff is **not** an additional 43 edits on top of the final 175.
- No further edit, replacement, reordering, category renaming or answer reshuffling is permitted without renewed owner approval.
- The owner-approved Couple Challenges v10 catalog is unchanged.

## 2. Canonical assets and compatibility

- `content/quizzes_ar_v4.json` is the canonical bank.
- `app/src/main/java/com/r2h/sirr/core/EgyptianQuizCatalog.kt` is the generated Kotlin representation used by `ExpandedQuizzes.packs`.
- `backend/public/quizzes.json` mirrors the canonical source for existing remote clients.
- All three were compared by source inspection, with matching JSON and generated Kotlin string representations; this is not proof of compilation.
- The content schema version remains `4`. Existing remote protocol was not intentionally changed.
- The historical content generator also manages legacy challenge sources. Do not run it in write mode without reviewing its full output; the approved v10 challenges must remain frozen.

## 3. Gameplay and user experience

Three discrete modes are available in source:

1. **Solo:** one player answers 8, 12, 20 or 25 randomly selected non-repeating prompts from a chosen pack. Players can skip or undo their current round's prior answer and review results.
2. **Paired descriptive comparison:** two participants answer the same prompts individually. The normal handoff view hides A's answers from B. Only questions both actually answered count toward matching-choice descriptions. No clinical personality or relationship compatibility score.
3. **Guess partner:** participant A answers, privately predicts B's choices, then hands off to B after B's consent. Prediction scores compare the forecast to B's actual choices, excluding skipped forecasts and unanswered choices. Ordinary matching and prediction results are strictly separate.

Technical path:
- `core/QuizSession.kt` implements guarded state transitions `FIRST`, optional `PREDICT`, `HANDOFF`, `SECOND`, `RESULTS`.
- The session snapshots the chosen question wording and choice order, so content updates cannot rebind previous saved numeric answers to new text.
- `QuizScreen.kt` renders the above modes and requires an affirmative age/self-attestation and quiz-content consent for adult packs, plus independent consent from the second participant at handoff.
- This is a **self-attestation, not third-party age verification**.
- The participant may always skip or stop without an outcome penalty.
- `QuizSessionStore.kt` uses the existing Android Keystore AES-GCM / `AtomicFile` `LocalVault`. V2 records include optional predictions and support reading v1 saved sessions.
- A corrupted/unreadable saved vault record is **distinguished from an absent record** using `LocalVault.hasPrivate`; the user receives an explicit warning and must deliberately clear the record before starting a new session, instead of silently overwriting the old one.
- `MainActivity.kt`'s Quiz exit confirmation accurately informs the user that Quiz sessions resume from encrypted local storage.
- Data stored in the vault is encrypted at rest; the app cannot guarantee protection from someone who already has access to an unlocked phone or from screenshots. A secure content warning does not constitute verified age assurance.

## 4. Static review and verification debt

The last recorded source-only content review checked:
- Seven packs of 25, 175 distinct normalized question texts and three nonempty, different choices per question.
- Generated Kotlin and backend JSON matching the canonical file.
- 92-item pre/post editorial log, adult-topic coverage and UI disclosure.
- A prior snapshot recorded 11/11 static checks as matching. Changes since then were limited to Quiz restore/exit code and approval documentation; these changes are **not** independently compiled or device-tested.

Source contains 29 Quiz-related JUnit declarations: 20 session, 6 content and 3 existing skip-insight checks. These tests are **written, not executed**.

**Deferred to the global build/test gate, not falsely certified as passing:**
- Run a real Android Gradle compilation and all 29 Quiz unit test cases.
- Add/run instrumentation scenarios for vault v1 → v2 recovery, corrupted ciphertext, interrupted write, missing key, clear and process-death restoration.
- Validate every gameplay mode for all selected round lengths, repeated taps, last-question stage transitions, stop/restart, and repeated entry from the main menu.
- Verify paired handoff secrecy at UI level, consent reset, no skip counting, correct forecast numerator/denominator, and back navigation.
- Validate Arabic RTL, font size, accessibility and TalkBack on target Android hardware.
- Verify adult-content disclosure, unchanged remote quiz consumers, and navigation to other R2H-Play games.
- Merge/release only on actual build, test and device evidence.

## 5. Owner-facing verdict

**Quiz editorial content is approved and frozen. Source features are implemented, and the source work can be set aside while the next R2H-Play feature is developed.** Technical verification remains open and must be completed once all app modules are ready for the integrated build, following the owner's prior instruction not to build APKs between individual feature edits.

PR #33 remains Draft and unmerged; parent PR #32 (Couple Challenges v10) remains separate.
