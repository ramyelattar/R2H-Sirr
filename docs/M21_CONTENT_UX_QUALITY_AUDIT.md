# R2H-Play — M21 Content & UX Quality Audit

**Audit basis:** `feature/m20-legacy-content-test-cleanup` (GitHub source; M20). **Audited:** 25 October 2026.  
**Method:** Static source review and token-based text analysis in the connected GitHub repository. **No Android build, compilation, APK installation, content rewrite, QA-device run or merge.**  
**Artifacts:** `docs/M21_CONTENT_UX_QUALITY_AUDIT.md` (this report), `docs/M21_CONTENT_QUALITY_METRICS.json`, `docs/M21_NEAR_DUPLICATE_REVIEW.csv`.

## Verdict

**P0 editorial and consent review required; not yet 90%-quality signoff.** Completeness checks pass, but raw item counts and absence of exact duplicates cannot establish originality, coherent pacing, clear consent, or adequate gameplay quality. Avoid assigning a fabricated percentage score without representative playtesting and a predeclared rubric. The current user-supplied sources contain graphic sexual language and some phrases that may imply force/pain or inadequate consent; this is an editorial risk, not a determination that each flagged item depicts coercion.

## Quantitative inventory

| Bank | Current size | Exact duplicate check | Quality findings |
|---|---:|---|---|
| Couple challenges | 130 / five levels ×26 | no exact duplicates | high reuse of templates in adjacent higher levels |
| Party challenges | 100 / five levels ×20 | no exact duplicates | narrow activity types and repetitive opening templates |
| Challenges combined | **230** | **230 distinct** | **62 candidate near-duplicate pairs** across modes/tiers |
| Quiz questions | **175** (7 ×25) | **175 distinct** | **11 candidate near-duplicate pairs** |
| Answer choices | **525** | **403 distinct strings** | **78 entries** have four or fewer characters |
| Whispers | **60** (20 ×3) | 60 distinct | zero near-duplicate pairs above the chosen heuristic threshold |
| Scratch secrets | **60** (20 ×3) | 60 distinct | 3 candidate near-duplicate pairs |
| Dice | 3 ×6 faces × six modes | schema retained | **216** possible face combinations per mode; coherence not enforced |

**Near-duplicate method:** normalize Arabic diacritics/tatweel and basic letter variants, remove punctuation, compare token sets by Jaccard similarity ≥**0.73**. Pairs are **candidates**; a high score is *not* a human judgement of semantic duplication. Exact suspect item coordinates and similarity scores appear in `docs/M21_NEAR_DUPLICATE_REVIEW.csv` (without reproducing graphic source text).

## P0 — editorial, age suitability and consent (approval blocker)

**M21-P0-01 — content intensity and informed opt-in.** Challenges and questions contain explicit sexual content; later tiers include some terms that can suggest force, pain or pressure. Source has an 18+ self-attestation gate and a per-player start consent gate in `MainActivity.kt:250-298` and `ExperienceScreen.kt:150-181`, plus a per-turn skip control at `ExperienceScreen.kt:272-275`. These are useful but **not consent to each physical act**. Screen-level skip should remain possible without penalty; add clear per-activity or per-card opt-in/consent cues before any sensitive participation, consider content intensity preview and easy opt-out, and flag coercive language for individual editorial review. Never infer affirmative consent from entering the game.

**M21-P0-02 — party context.** All five intensity tiers of the 100-item party bank contain highly personal scenarios, while the app can involve 2–4 players. Review party-specific consent, audience expectations and physical-contact tasks; keep private couple activities separate from group gameplay unless participants explicitly opt in. Distinguish consensual language from instructions that presume physical access or endurance. Sensitive text should not auto-appear without informed gating.

**M21-P0-03 — quiz disclosure.** The `quiz_01` pack is more explicit than its generic title suggests. Before its first question, consider a visible content-intensity label; don't promise neutral self-reflection then abruptly show explicit content. `QuizScreen.kt:88-135` moves from selecting a pack directly into questions. Quiz screen offers overall topic reset at `QuizScreen.kt:241` but **no dedicated skip for the current question**. A no-answer skip requires careful state design so `answersA`/`answersB` and `QuizInsights.compare` remain aligned, and comparisons exclude unanswered questions or handle them explicitly.

## P1 — significant content-quality problems

**M21-P1-01 — templated adjacent intensity tiers.** Comparing corresponding positions in the couple bank: HOT→HARD has **17/26** pairs at Jaccard≥0.73 (mean 0.788); SEXY→HOT has 5/26 (mean 0.619); NORMAL→SEXY has 3/26 (mean 0.454). Higher tiers often repeat the same scenario with only words of intensity changed, rather than introducing new kinds of challenges. In party, SEXY→HOT has 8/20 such aligned pairs. **Rewrite concept variety first**, not just stronger adjectives; preserve 26/20 per mode/tier and existing v4 schema.

**M21-P1-02 — short and recycled quiz choices.** `quizzes_ar_v4.json` has 525 choices but only 403 distinct strings. 78 choices have ≤4 characters. Pack `quiz_06` is worst: **46 distinct labels among 75** and **34 choices of ≤4 characters**. The `quiz_04` pack has 66/75 distinct choices. Short answers are not intrinsically wrong, but frequent context-free single words compromise engagement and easy comparison. Maintain *three meaningful independent responses*, not filler.

**M21-P1-03 — repetitive quiz openings.** **48/175** question texts begin with “إيه أكتر” and **34/175** begin with “إيه اللي” after normalizing Arabic spelling, total **82/175**. Introduce genuinely different question types: decisions, situational reveals, short story choices, intuitive interpretations, conversational dilemmas. Avoid simply replacing initial words while leaving the same question.

**M21-P1-04 — dice sentence assembly.** `DiceScreen.kt:114-118` displays the **space-joined three selected face labels** as a single challenge. `ExperienceLogic.kt:121-154` exposes 3×6 faces for each preset, but `DiceConfiguration.validate()` only validates sizes, probabilities and individual face lengths, not joined sentence quality. The NORMAL verb “قول له إن” can combine with an object fragment lacking a suitable clause, producing unnatural text. **Before final signoff, enumerate all 216 triples for each relevant preset** and manually review, or introduce typed sentence-safe phrase fragments with compatibility testing while preserving 3 dice, 6 faces, weights, saved CUSTOM support, and the existing `preset(Intensity)` API.

**M21-P1-05 — scratch stage-selection regression.** `ScratchScreenV2.kt:178-181` fills three messages from one random group but then forcibly executes `stages = 1`. If the user previously selected two or three scratch layers, pressing “اختارلي سر جاهز” resets the choice unexpectedly. **Fix by preserving the current selected stages**. The original M16/M17 selection logic had the same reset; M17 did not introduce it. `ScratchPanel` gestures and reveal logic should remain untouched.

**M21-P1-06 — whisper and scratch editorial coverage.** All 60 whisper texts and 60 scratch texts are exactly distinct. Scratch contains **three** candidates over the chosen similarity threshold; narrative types and gender viewpoint are still limited. Confirm each trio follows a meaningful setup/development/reveal arc, a voluntary skip option, no demands on nonconsenting recipients, and copy suitable for the intended adult context. Do not overinterpret a 0 near-duplicate statistic for whispers as a clean creative-quality review.

## P2 — other concerns / keep intact

- `GameLogic.kt::OriginalContent` and `content/additional_quizzes_ar_v2.json` (119 unused questions) are legacy/archive, not a route to fixing currently displayed quiz text. Leave them unchanged.
- Content v4 JSON is canonical; update generated `AdultChallengesV2.kt`, `EgyptianQuizCatalog.kt` and web mirrors only via `tools/generate_content_v2.py` when an edit is approved.
- Avoid creating extra "score" claims for the quiz; `QuizInsights.kt` explicitly defines a **descriptive answer-agreement count** rather than a psychological or scientific compatibility score. Existing `QuizScreen.kt:173-182` accurately says so.
- Protect one-use whisper deletion, encrypted local images and private media, expiry and photo deletion in scratch, and unbiased 17/17/17/17/16/16 dice weights.

## Suggested measurable release-readiness gate (proposal, not yet achieved)

| Gate | Acceptance criterion | Current evidence |
|---|---|---|
| Structural completeness | 230 challenges; 175 questions ×3 answers; 20 ordered whisper/scratch trios; all dice presets valid | **Static PASS** |
| Content variety | Human review of all similarity-review pairs and all 175 question wordings; no unresolved repetitive filler | **OPEN** |
| Consent / suitability | No unresolved force/pressure ambiguity, clear per-activity boundaries, opt-in and skip where needed | **OPEN / P0** |
| Intensity progression | SOFT→HARD distinct in theme and perceived tone, not only repeated stronger words | **OPEN** |
| Dice grammaticality | 216×5 assembled preset phrases reviewed or mechanically constrained and sampled in UI | **OPEN** |
| UX flow | Scratch selection preserves stage count; quiz skip does not break paired answer alignment | **OPEN** |
| Actual behavior | Kotlin/Compose compiles, unit tests pass, Arabic/RTL phone playtest completed | **NOT TESTED** |

## Recommended next work order

1. **M22-A (P0 editorial and safety):** Prepare exact item-indexed review suggestions without overwriting user-authored content; explicitly assess consent and physical-action prompts. Source opt-in/skip issues should be separately defined and user-approved.
2. **M22-B (P1 safe UX fixes):** Fix scratch stage reset, and implement a careful quiz-question skip path with aligned indices. Run tests when build authorization is given.
3. **M22-C (P1 creative diversity):** Rewrite affected challenge templates, quiz filler and dice phrase grammar. Use v4 JSON as canonical and retain count/schema.
4. **M23 acceptance:** Validate content generation sync, compile, run tests, test actual UX, and confirm quality through human review before claiming ≥90%.

## Source limitation

All findings are **static analysis** of repository state at M20. The keyword/proximity heuristics can miss nuanced scenarios and return false positives. No real player feedback, on-device video/screenshot review, Kotlin compilation, test execution, Android build, APK installation, CI run or main-branch merge occurred during this audit.
