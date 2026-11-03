# R2H-Play — Quiz feature source close-out

**State:** Content structurally reviewed and source gameplay logic implemented; **not compiled, executed, device-tested, owner-accepted or merged**.

## Content: 7 topics × 25 questions

The canonical bank is `content/quizzes_ar_v4.json` (`version: 4`). It contains 175 questions with 525 contextual choices (three choices per question). Topics:

1. إنت من جوّه
2. الحب على مزاجك
3. إحنا شبه بعض؟
4. الجاذبية والإشارات
5. أمنيات متخبية
6. راحة وحدود
7. جرأة ولا صراحة؟

**Editorial policy:** Preserve the existing M26 question bank. Only three targeted wording fixes have been made (`quiz_02` question 20, `quiz_05` questions 9 and 24). Changes are logged with before/after in `docs/QUIZ_V4_EDITORIAL_CHANGES.json`. No question, option, topic, count, or IDs were deleted or replaced. The original `main` JSON blob is recorded in that log for recovery.

### Canonical content mapping

- `content/quizzes_ar_v4.json` — canonical JSON.
- `app/src/main/java/com/r2h/sirr/core/EgyptianQuizCatalog.kt` — generated Android Kotlin.
- `backend/public/quizzes.json` — canonical public mirror for the existing remote protocol.
- `tools/generate_content_v2.py` — existing generator (also writes historic challenge assets; don't invoke in write mode while the separately approved v10 couple runtime bank is frozen).

A static cross-file text comparison found the edited Kotlin bank and the mirror exactly matched the edited JSON. This is an **inspection, not test execution**. 175 unique normalized question texts, 525 nonblank options, zero exact intra-question duplicated options, and no answers of four or fewer characters were observed in the inspected source.

## Gameplay logic — source changes

### Pure state machine: `QuizSession.kt`
- One of four stages: FIRST, HANDOFF, SECOND, RESULTS.
- Start 8/12/20/25 distinct questions chosen without replacement from one 25-question topic.
- The selected question text and all three choices are snapshotted; future content changes cannot silently reassign older saved answer indices to a different question.
- A participant may answer 0/1/2, skip using -1, or undo only their own previous answer before their turn finishes. No answers become mandatory.
- In paired mode, all of participant A's answers are finished before the second participant opts in and answers the identical ordered set. Their answers remain undisclosed in the handoff/second phases in the normal UI.
- After both finish, each question can be reviewed forward/backward; a single-person quiz goes directly to results.

### Results and scoring
- No correct/wrong labels. These are subjective conversation choices.
- Display per-participant answered/total counts.
- Only identical answers where **both participants answered** count as descriptive agreement; skipped items are excluded.
- If no question was jointly answered, show that no comparison can be made.
- **No psychological profiling, compatibility grade, diagnosis, winner or invented percentage score.**

### Local persistence and privacy
- `QuizSessionStore.kt` stores the complete selected-question snapshot, phase, answer arrays and reveal position in the existing `LocalVault` backed by Android Keystore AES-GCM and AtomicFile.
- Every advancement writes the next state **before** changing visible app state. If storage fails, the visible session does not advance.
- Resume after process or Activity recreation is designed from the vault record, rather than saving private answers in Compose `savedInstanceState`.
- Leaving the Quiz route and returning retains the locally encrypted session. 'New round' requires explicit confirmation and clears its saved answers.
- The encrypted vault protects data at rest; it does not authenticate the person currently holding the unlocked phone or prevent screenshots/shoulder-surfing.
- The privacy screen's existing clear-data action also clears these records.

### Content/UI safeguards
- Correct topic names are derived from the canonical Android catalog, avoiding duplicated labels.
- Explicit consent is required before starting the first participant. The second participant must consent at handoff, including following process restoration.
- Users can skip any question, revisit their previous question during their own turn, or exit without being given a score or penalty.
- Quiz is separate from Couple Challenges; no edit to the approved v10 challenge texts, levels, durations or action codes.

## Verification gates (not run)

- 13 new pure-JVM `QuizSessionTest` cases (stage transitions, solo/paired, undo, skip, handoff, non-overlap, snapshot invariants).
- 4 new `QuizContentV4Test` source cases, plus 3 existing `QuizInsightsSkipTest` cases.
- These tests have **not been executed**, and the source has **not been compiled**.

Remaining release gates:
- Compile Android Kotlin/Compose, run Gradle/JUnit (and fix compiler/runtime failures).
- Instrumented encrypted-vault save/load/clear under process death, corrupt file and interrupted writes.
- On-device check all seven packs, full 25-question modes, handoff privacy, skip/back, and results.
- Check Arabic RTL, small displays, accessibility semantics and TalkBack.
- Verify existing remote quiz protocol v4 and all other app routes remain unaffected.
- Do not merge or claim feature production-ready until those checks have evidence.

**Source-level outcome:** Content and core quiz flow are implemented on an isolated development branch. Runtime acceptance remains pending full verification.
