# R2H-Play — M19 Content Usage and Legacy-Code Audit

**Reference branch:** `feature/m18-dice-content-integration`  
**Baseline SHA:** `8b673a4940656657dd8af86ee5c60c2d4f7c241d`  
**Scope:** Read-only static source audit of all 27 Kotlin main-source files, canonical/legacy JSON, both Kotlin test source files, content generators, remote web client, backend server and CI/source-check configuration. No build, installation, or runtime execution was performed.

## Executive verdict

1. The live quiz catalog is **175 questions in `content/quizzes_ar_v4.json`**, generated into `EgyptianQuizCatalog.kt`, exposed through `ExpandedQuizzes.packs`. Local `QuizScreenV2` and remote `RemoteScreen` both access `ExpandedQuizzes`; the backend web client loads the matching `backend/public/quizzes.json` and requires `version: 4`.
2. **`content/additional_quizzes_ar_v2.json` is *not* in this live path**. It has `version: 3`, 7 groups × 17 questions = **119 unique questions** (all have 3 choices). It has **zero exact question-text matches** with the 175-question canonical catalog. Its corresponding legacy `MoreQuizzesV2.kt` contains the same 119 question texts, but has **no static references** in the 27 inspected main Kotlin files, the two test files, or the current generator.
3. **`GameLogic.kt` is only partially legacy and must not be deleted**. `GameLogic.weightedFace` is used for actual dice rolls by `DiceConfiguration.roll()` in `ExperienceLogic.kt`; `GameLogic.evenWeights` is used to construct every dice preset; `Question` and `Quiz` are shared types used by the active quiz catalog.
4. The `OriginalContent` object within `GameLogic.kt` is a **legacy example catalog**: 7 quiz packs × 3 questions = **21** questions, 12 couple prompts, 12 party prompts and a three-die template. No active screens or catalog generators read these values. `GameLogicTest.dataIsConsistent` **does** assert legacy counts, so direct deletion without test migration would break this test.
5. `GameLogic.score()` and `GameLogic.interpretation()` have no identified live Kotlin call sites. The test `GameLogicTest.deterministicScore` calls them. The active comparison flow uses `QuizInsights.compare` instead.

## Traceable runtime paths

| Component | Actual chain / consumer | Status |
|---|---|---|
| Core challenges | `content/challenges_ar_v2.json` → `tools/generate_content_v2.py` → `AdultChallengesV2.kt` → `Challenges.deck()` → `SessionFactory.create()` | ACTIVE |
| Core quizzes (Android) | `content/quizzes_ar_v4.json` → generator → `EgyptianQuizCatalog.packs` → `ExpandedQuizzes.packs` → `QuizScreenV2()` | ACTIVE |
| Core quizzes (remote Android) | `ExpandedQuizzes.packs` → `RemoteScreen.kt` | ACTIVE |
| Core quizzes (web) | `content/quizzes_ar_v4.json` → generator → `backend/public/quizzes.json` → web `loadQuizzes()` | ACTIVE |
| Dice presets | `ExperienceLogic.kt::DiceCatalog.preset()` → `DiceScreen.kt` → `DiceConfiguration.roll()` → `GameLogic.weightedFace()` | ACTIVE |
| Legacy 119 questions | `content/additional_quizzes_ar_v2.json` → old `MoreQuizzesV2.kt` (historical generation) | UNREFERENCED / NOT PRESENTED |
| Legacy 21 questions | `GameLogic.kt::OriginalContent.quizPacks` → `GameLogicTest.kt` only | TEST-ONLY |
| Legacy couple/party prompts | `GameLogic.kt::OriginalContent.couplePrompts/partyPrompts` | NO STATIC CONSUMERS |
| Legacy dice labels | `GameLogic.kt::OriginalContent.diceTemplates` → `GameLogicTest.kt` count assertion only | TEST-ONLY |
| Score / interpretation helpers | `GameLogic.score/interpretation` → `GameLogicTest.kt` only | TEST-ONLY |
| Dice RNG/weights | `GameLogic.weightedFace/evenWeights` → `ExperienceLogic.kt`, tests | ACTIVE |

## Evidence anchors

- `app/src/main/java/com/r2h/sirr/QuizScreen.kt:28`: `val packs = ExpandedQuizzes.packs`.
- `app/src/main/java/com/r2h/sirr/core/ExpandedQuizzes.kt:5`: `val packs: List<Quiz> = EgyptianQuizCatalog.packs`.
- `app/src/main/java/com/r2h/sirr/RemoteScreen.kt:555-559`: `ExpandedQuizzes.packs` for remote quiz selection and lookup.
- `backend/public/app.js:121`: fetches `/quizzes.json`, checks `version===4` and seven packs.
- `tools/generate_content_v2.py:13-14`: reads only the canonical challenges and quiz JSON. `tools/generate_content_v2.py:19-44`: asserts canonical v4 and expected counts. `tools/generate_content_v2.py:49-86`: writes Kotlin and web mirrors, not `MoreQuizzesV2.kt`.
- `app/src/main/java/com/r2h/sirr/core/MoreQuizzesV2.kt:3-5`: `MoreQuizzesV2.extra` legacy generated data.
- `app/src/main/java/com/r2h/sirr/core/GameLogic.kt:5-20,43-47,85,100,115`: live RNG helpers/types plus old embedded data.
- `app/src/main/java/com/r2h/sirr/core/ExperienceLogic.kt:115-117,121-154`: active dice face selection and weight construction.
- `app/src/main/java/com/r2h/sirr/DiceScreen.kt:30,97,100,176`: actual `DiceCatalog.preset` callers.
- `app/src/test/java/com/r2h/sirr/core/GameLogicTest.kt:25-35`: tests helper math and legacy `OriginalContent`.
- `app/src/test/java/com/r2h/sirr/core/ExperienceLogicTest.kt:37-40`: tests the live `ExpandedQuizzes` catalog (175 questions).
- `tools/ContentSmokeTest.kt`: verifies 175-question live catalog and active challenge/session game mechanics.
- `.github/workflows/m8-source-tests.yml:26`: executes `python tools/generate_content_v2.py --check`. This does not cover legacy extra JSON.

## What not to do

- **DO NOT** delete `GameLogic.kt`: this would break current dice behavior and core data type references.
- **DO NOT** replace `quizzes_ar_v4.json` with the 119-question extra bank or concatenate them without changing the explicit seven-packs ×25-questions schema, generator, consumers and web client.
- **DO NOT** modify `OriginalContent` assuming this will update on-screen challenges/quizzes/dice; the live display reads other catalogs.
- **DO NOT** remove the legacy `OriginalContent` block before migrating `GameLogicTest.dataIsConsistent`.
- **DO NOT** quietly delete the legacy extra bank: its 119 texts have no exact overlap with live quizzes and may be reusable after an explicit content design decision.

## Recommended next steps (no action taken by this audit)

**P0 — Content-source freeze:** Declare v4 challenges and quizzes as canonical and stop editing derived Android/backend files by hand. Use `tools/generate_content_v2.py --check` as the source synchronisation gate.

**P1 — Legacy quarantine:** Mark `additional_quizzes_ar_v2.json` and `MoreQuizzesV2.kt` as legacy/not shipped content in project documentation. Keep them intact until the owner decides whether to archive, selectively adapt, or discard the 119 additional questions.

**P1 — Correct misleading tests:** Change `GameLogicTest.dataIsConsistent` to validate real `ExpandedQuizzes.packs` and `DiceCatalog.preset`, or move legacy-only tests to a clearly named legacy test. Keep the live RNG tests.

**P2 — Extract shared types safely:** Only after evaluating reference coverage, consider extracting `Question`/`Quiz` and `GameLogic` math helpers into focused files, then deprecate or remove `OriginalContent`. Require compilation and unit tests in a *separate* explicitly authorised phase.

**P2 — Editorial follow-up:** The 119 untouched old questions are not a second active library. If more content is wanted, select and rewrite those questions for the active seven-pack format instead of assuming existing `MoreQuizzesV2` appears automatically.

## Audit limitation

Findings describe **static references in the inspected M18 repository files**. Dynamic reflection, unpublished code, other branches, and device runtime effects cannot be ruled out without further checks. No file was deleted, renamed or edited; no content catalog changed; no APK/build/test run; no merge to `main`.
