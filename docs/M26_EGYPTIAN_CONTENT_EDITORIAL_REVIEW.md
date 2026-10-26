# R2H-Play M26 — Egyptian-Arabic content editorial correction

Base: `feature/m25-3-cinematic-cards`. Branch: `feature/m26-content-editorial-rewrite`.

## Diagnosed from the actual content (not screenshots alone)
- The original quiz bank repeated generic personal questions and included unrelated or ungrammatical three-choice answers.
- 26 couple prompts in SOFT contained material better suited to higher intensities, some ill-formed or intrusive; group SOFT / NORMAL / SEXY also included content unsuitable for a mixed party setting.
- The 20×3 suggested whispers and 20×3 scratch reveals were dominated by near-identical, mechanically repetitive lines, sometimes gender-specific and coercive, with weak semantic progression across the three stages.
- Some user-facing labels and dice fragments were grammatically inconsistent (e.g. an agreement mistake after «سؤال محرج»).

## Changes made
- 102 distinct quiz questions were fully replaced, including all 25 in «إنت من جوّه», «أمنيات متخبية», and «جرأة ولا صراحة؟», plus 27 targeted questions across the other quiz packs. Replaced all 3 options for those questions.
- Corrected an additional nine quiz answer sets and one overly short choice. Every question still has exactly three choices that answer the question.
- 78 couple challenges (SOFT, NORMAL, SEXY) rewritten in contemporary Egyptian Arabic; the 52 HOT/HARD challenges were kept as authored because they already had more coherent roleplay/consent phrasing.
- 60 party challenges (SOFT, NORMAL, SEXY) rewritten for group-appropriate, playable conversation and party roleplay; existing 40 HOT/HARD challenges retained.
- 60 suggested whisper messages replaced as 20 linked 3-message arcs (opening → reveal → choice); 60 scratch prompts replaced as 20 linked 3-reveal arcs (clue → explanation → optional action).
- 14 direct strings fixed in Android screens, introductory descriptions and `DiceCatalog`. The original art, wallpaper and animation are unchanged.
- Regenerated both Android Kotlin canonical catalogs and backend public JSON mirrors from the exact two edited canonical JSON files.

## Editorial approach
- Colloquial Egyptian Arabic; concrete, direct, varied and understandable actions, with a progression from light to more personal themes.
- Preserve 18+ framing, intimacy/flirtation where appropriate, and meaningful agreement/skip choices. Avoid forced participation, gender assumptions, repeated formulae and text that is only explicit but lacks a playable question.
- Do not conflate `skip` with failure; all existing consent/skip mechanisms remain unchanged.

## Hard invariants
- 230 distinct challenges: couple 5×26, party 5×20, with existing five intensity identifiers unchanged.
- 175 distinct questions: seven packs ×25, exactly three answer options each (525).
- Whispers 20×3; scratch 20×3.
- Same package ID `com.r2h.sirr`, same storage/crypto/session/navigation and remote protocol.
- VersionCode 13 / `0.9.4-m26-content` (in-place upgrade from M25.3).

## Limitations and follow-up
- Kotlin/Gradle compilation, APK installation, interactive device QA and human review of every remaining HOT/HARD line were **not performed** here. Source counts, content uniqueness, language consistency and generator/mirror alignment are checked by source inspection; not proof of flawless player experience.
- The UI uses generated Kotlin from canonical JSON; manual edits to `AdultChallengesV2.kt` or `EgyptianQuizCatalog.kt` should never become a new source of truth.
- The remaining 73 quiz questions and 92 HOT/HARD prompts were retained unless individually corrected; do not claim 100% editorial rewrite or a verified 90% experience. User review on device will guide remaining targeted edits.

## Local build (only when requested)
`tools/Build-M26.ps1` runs `:app:assembleDebug`, does not install APK, wipe data or invoke GitHub Actions. Image asset at `app/src/main/res/drawable-nodpi/r2h_neon_wallpaper.webp` remains required and unchanged.
