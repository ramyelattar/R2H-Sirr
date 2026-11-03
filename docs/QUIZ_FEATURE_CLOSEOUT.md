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

**Editorial policy:** Preserve the original 175-question M26 bank wherever it works. The first pass corrected three phrases. The current review modifies **43 more question entries**, preserving 132 verbatim, with the full before/after question AND three choices recorded in `docs/QUIZ_V4_EDITORIAL_CHANGES.json`. The previously ambiguous role-relative prompts in `quiz_02:3`, `quiz_03:14`, and `quiz_03:18` were rewritten as first-person preferences. No topic, question or answer-option count changed. New wording remains **pending owner editorial approval**.

### Canonical content mapping

- `content/quizzes_ar_v4.json` — canonical JSON.
- `app/src/main/java/com/r2h/sirr/core/EgyptianQuizCatalog.kt` — generated Android Kotlin.
- `backend/public/quizzes.json` — canonical public mirror for the existing remote protocol.
- `tools/generate_content_v2.py` — existing generator (also writes historic challenge assets; don't invoke in write mode while the separately approved v10 couple runtime bank is frozen).

A static cross-file text comparison found the edited Kotlin bank and the mirror exactly matched the edited JSON. This is an **inspection, not test execution**. 175 unique normalized question texts, 525 nonblank options, zero exact intra-question duplicated options, and no answers of four or fewer characters were observed in the inspected source.

## Gameplay logic — source changes

### Pure state machine: `QuizSession.kt`
- Five guarded stages: FIRST, optional PREDICT, HANDOFF, SECOND, RESULTS.
- Start 8/12/20/25 distinct questions chosen without replacement from one 25-question topic.
- The selected question text and all three choices are snapshotted; future content changes cannot silently reassign older saved answer indices to a different question.
- A participant may answer 0/1/2, skip using -1, or undo only their own previous answer before their turn finishes. Prediction mode also supports skip and undo of a forecast. No answers become mandatory.
- In paired mode, all of participant A's answers are finished before the second participant opts in and answers the identical ordered set. Their answers remain undisclosed in the handoff/second phases in the normal UI.
- After both finish, each question can be reviewed forward/backward; a single-person quiz goes directly to results. In the optional third gameplay mode, participant A records private predictions of participant B's choices **after completing their own answers**; B answers the same questions independently; both actual answers and predictions are revealed only at the result stage.

### Results and scoring
- No correct/wrong labels. These are subjective conversation choices.
- Display per-participant answered/total counts.
- In normal paired comparison, only identical **role-neutral** answer choices where both participants answered count as descriptive agreement; skipped items are excluded. Three previously misleading role-relative questions were rewritten to satisfy this rule.
- In the separate prediction mode, count forecasts that match the partner's actual answer; only questions with both an actual answer and an unskipped prediction are comparable. No comparison is shown if none can be compared.
- **No psychological profiling, compatibility grade, diagnosis, winner or invented percentage score.**

### Local persistence and privacy
- `QuizSessionStore.kt` v2 stores the complete selected-question snapshot, mode flag, phase, own answers, forecasts, partner answers and reveal position in the existing `LocalVault` backed by Android Keystore AES-GCM and AtomicFile. Previous store v1 sessions load with prediction mode disabled.
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

- 20 pure-JVM `QuizSessionTest` source cases including 7 additional prediction-mode cases; all unexecuted.
- 5 `QuizContentV4Test` source cases including a guard for misleading self/other role-label choices, plus 3 existing `QuizInsightsSkipTest` cases. 28 Quiz-related test declarations in total; all unexecuted.
- These tests have **not been executed**, and the source has **not been compiled**.

Remaining release gates:
- Compile Android Kotlin/Compose, run Gradle/JUnit (and fix compiler/runtime failures).
- Instrumented encrypted-vault save/load/clear under process death, corrupt file and interrupted writes.
- On-device check all seven packs, full 25-question modes, handoff privacy, prediction-mode stages, skip/back, and both types of results.
- Check Arabic RTL, small displays, accessibility semantics and TalkBack.
- Verify existing remote quiz protocol v4 and all other app routes remain unaffected.
- Do not merge or claim feature production-ready until those checks have evidence.

**Source-level outcome:** Content and core quiz flow are implemented on an isolated development branch. Runtime acceptance remains pending full verification.

## Editorial/play-mode refinement — 2026-11-03

- Source: `content/quizzes_ar_v4.json`, 175 questions and 525 choices; 43 entries edited, 132 left unchanged from the preceding v4 Quiz baseline. The initial 3 corrections are also retained.
- Targeted topics: 9 edits in attraction/signals, 9 in hidden wishes, 18 in daring/truth; 4 boundary questions and 3 role-semantic repairs in other packs. Pack 01 was preserved completely.
- The updated bank avoids the previous «أنا غالبًا / هو غالبًا» false-comparison defect by comparing actual preferences rather than perspective-relative identity labels.
- The new partner-prediction mode is **not** a compatibility score. Its numerator is correctly predicted answers; denominator excludes predictions or partner answers that were skipped. Normal comparison remains a separate optional mode.
- Do not treat this static editing and source implementation as compile or device evidence. No Android build, APK or execution of tests occurred.
- Editorial approval of the 43 rewrites and both UI/game modes is pending. No challenge-v10 content was changed.
