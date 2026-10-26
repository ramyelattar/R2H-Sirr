# M26 — Egyptian Arabic content editorial correction

Base branch: feature/m25-3-cinematic-cards
Working branch: feature/m26-egyptian-content-editorial

## Actual editorial changes (source of truth)
1. `content/challenges_ar_v2.json`: **129 of 230** challenges rewritten/corrected:
   - Couple SOFT: 26, NORMAL: 26, SEXY: 10, and 5 targeted HOT/HARD fixes
   - Group SOFT, NORMAL, SEXY: 60; group HOT/HARD: 2 corrections
   - Replaced unfinished commands, misleading role-specific actions, repetitive phrases and illogical statements with coherent Egyptian colloquial challenges. Group prompts no longer assume all participants are a couple.
2. `content/quizzes_ar_v4.json`: **81 of 175** questions and their 3 answers (243 choices) rewritten across six packs. The sixth `راحة وحدود` pack was already coherent and remains unchanged. Questions now have choices that actually answer them; eliminated placeholders and vague/non-answers.
3. `core/ExperienceLogic.kt` and `DiceScreen.kt`: 5 dice preset banks aligned into action + topic + manner, each with 6 faces (5×216 = 1080 possible natural preset sentences). Preset rolls now show one full instruction. Custom dice and images still show their original independently labeled faces.
4. `ScratchScreenV2.kt`: 20 fresh, linked, escalating triplets (60 secrets) with clear language, without changing 1/2/3 layers, scratch engine, TTL, encrypted user photos or close/clear behavior.
5. `WhispersScreen.kt`: 20 linked triplets (60 whispered messages), progressing premise → confession → optional invitation; preserved one-use deletion, audio/photo, TTL and no-persist behavior.
6. Both canonical v4 JSON banks regenerated into Kotlin Android catalogs and backend/public JSON mirrors with exactly the same programmatic template used by `tools/generate_content_v2.py`.

## Data invariants
- Exactly 230 unique challenges; 26 per couple level and 20 per group level.
- Exactly 175 unique questions in 7 packs of 25; exactly 525 choices.
- 20 scratch groups×3 and 20 whisper groups×3, no duplicate full messages.
- The established normalized token Jaccard 0.73 near-duplicate rule has no flagged challenge/question pairs at the checkpoint.
- Do not confuse textual/static source checks with human language review: approval for Egyptian naturalness and intensity progression requires actual use.

## Scope and safety
- Offline-first UI, navigation, animations, wallpapers, app identification (`com.r2h.sirr`), crypto and games stay untouched aside from text and composed dice sentence.
- Custom user-entered material is never modified by content imports.
- Version 0.9.4-m26-content-editorial, code 13, for in-place signed app update.
- Build only on Windows with `tools/Build-M26.ps1`; no GitHub Actions, CI, APK build, installation, or merge is run from this source-editing phase.

## Caveats
- Remaining unchanged prompts may still merit human copy-editing.
- The rewrite emphasizes natural, coherent and consensual adult flirting instead of literal graphic commands. Review whether levels match the intended product positioning.
- Android Kotlin compilation and rendered page behavior are NOT VERIFIED until local build.
