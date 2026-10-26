# M22-C — Content improvement checkpoint

**Base:** M22-B `3509fe9cd3335271964b14c7f283826b9f54722f`  
**Branch:** `feature/m22c-content-quality`

## Implemented
- **Dice:** rewritten all five preset intensity tiers into compatible Egyptian Arabic action/topic/style fragments. 3 dice ×6 faces each, with 216 possible triples per tier = 1,080 combinations. CUSTOM remains editable, probability weights and stored user dice unaltered. New unexecuted JUnit grammar test.
- **Challenges:** 92 HOT/HARD items comprehensively rewritten, then 42 further targeted items refined across SOFT/NORMAL/SEXY to address residual repetition, force-related phrasing and inappropriate group challenges. **134 positions** revised in total; canonical v4 counts remain 130 couple +100 party =230 unique.
- **Quizzes:** 56 questions/question-choice sets edited, including 25 completely reworked items in pack `quiz_06` (راحة وحدود). Canonical v4 remains 7 packs ×25 questions, each with 3 answer choices, 175 unique questions.
- Both canonical JSON files were used to regenerate their exact Kotlin counterparts and the two backend/public JSON mirrors. No direct editing of generated content without changing canonical data.
- A repeatable **source QA script** `tools/check_m22c_content_quality.py` was added; it is NOT executed or wired into CI by this branch.

## Source metrics using M21's normalized-token Jaccard threshold ≥0.73
| Metric | Baseline M21 | M22-C static |
|---|---:|---:|
| Candidate challenge near-duplicate pairs | 62 | 0 |
| Candidate question near-duplicate pairs | 11 | 0 |
| Answer options with 4 or fewer characters | 78 | 0 |
| Challenge total / exact unique | 230 / 230 | 230 / 230 |
| Quiz total / exact unique | 175 / 175 | 175 / 175 |
| Dice preset face combinations | 1,080 | 1,080, with grammatical fragment conventions |

**Caution:** 0 token-Jaccard candidates does not imply 0 semantically repetitive or inappropriate items. Some untouched source content remains graphic and needs detailed editorial/consent review, particularly prior to distribution. Static scripts cannot validate emotional pacing, real players' comfort, naturalness of every possible combination, or Play Store suitability.

## Protected behavior inherited from M22-A/B
- Actor renewed consent after each revealed challenge, free skip/quit, per-pack quiz opt-in, quiz `-1` skip handling and only-joint-answer comparison;
- Selected scratch layer count and byte-unchanged scratch gesture engine;
- Private vault storage, audio/photo handling, TTL, session persistence, network protocol, app identity, customized dice and zero data migrations.

## Gate/status
Not approved as '90%' or release-ready. No Kotlin compilation, Gradle task, unit test execution, emulator/device run, APK build, CI execution or merge into main. After the owner authorizes a testing phase, run `python tools/generate_content_v2.py --check` and `python tools/check_m22c_content_quality.py`, followed by Android/JUnit and on-device UX tests.
