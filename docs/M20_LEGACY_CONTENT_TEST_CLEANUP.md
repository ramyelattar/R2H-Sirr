# M20 — Legacy content and current-catalog test cleanup

## Starting point
- Base: `feature/m19-content-usage-audit`, commit `120f8159037ce671441aee42611193a404788e9e`.
- M19 found legacy `OriginalContent` and `MoreQuizzesV2` unreferenced by live views; `GameLogic` math helpers and `Question` / `Quiz` remain active.

## Changes
1. `GameLogicTest.dataIsConsistent()` → `activeCanonicalContentIsConsistent()`. The test now checks 7 current quiz packs with 25 questions each (175 distinct, three nonempty answers each), 230 live challenges across couple/party mode (26/20 per intensity, unique), and all six live preset dice configurations (3 dice × 6 valid face labels, 17/17/17/17/16/16 weights, <=72 characters per label).
2. Expanded `ExperienceLogicTest.quizBankHas175DistinctQuestions()` to verify exact pack sizes and three nonempty answer choices.
3. Retained the `OriginalContent` and `MoreQuizzesV2` banks intact; annotated those **unused content containers only** as deprecated and updated their headers, so no historical text is lost.
4. Documented canonical v4 vs archived v3 content, active vs sample helpers, and preservation rules in `README.md`.

## Safety and invariants
- **No changes to any content JSON, generated v4 catalogs, quiz/challenge screens, dice preset logic, remote protocol, Kotlin public method signatures, saved data or app identity.**
- `GameLogic.score` and `GameLogic.interpretation` are intentionally retained and still independently tested.
- All 119 archived extra questions and 21 sample questions (plus all sample prompts/templates) remain unchanged in their original files.
- `@Deprecated` is a compile-time advisory; without live references it does not affect app gameplay.
- No Android/Gradle compilation, tests, APK installation, CI execution, or merge was done. Passing static checks is **not** a proof of passing JUnit.

## Recommended next gate
On a Windows machine with the repository checked out, explicitly run `python tools/generate_content_v2.py --check` and `./gradlew :app:testDebugUnitTest` only when build/test execution is authorized, and confirm no generated content drift. Once all content branches are integrated, perform an independent content-quality / consent / language review before any release.
