# R2H-Play M18 — Grok Dice Content Integration

## Scope
Imported only the `DiceCatalog` portion of the user-uploaded `ExperienceLogic.kt` to preserve all existing M17 game mechanics byte-for-byte.

## Content changes
- Revised all presets for SOFT, NORMAL, SEXY, HOT, HARD.
- Kept CUSTOM mapped to the base group.
- Preserved 3 dice with 6 face labels per die and unchanged `GameLogic.evenWeights[index]` 17/17/17/17/16/16 assignment.
- All 15 explicitly defined six-face label rows are <=72 characters; shared rows keep that invariant.

## Protected mechanics
Original M17 prefix through DiceConfiguration and suffix from EphemeralEnvelope are byte-identical. No changes to challenges, session factory, turn selection, local scoring, duration validation, audio/image IDs, custom dice, or application identity.

## Quality risks
The new dice preset is an improvement in colloquial wording compared with M17, but **not every one of 216 possible three-face combinations per intensity is grammatically natural or an actionable challenge** (e.g. some verb/object joins are awkward). This import is therefore a content-integration checkpoint and not a 90%-quality UX signoff.

## Checks and limits
Source-only checks: 6 intensity keys, 15 six-item literal face rows, text-length invariant, protected code byte-exact; original preset mapper retained. No Gradle build, Kotlin compilation, APK install, runtime test, GitHub Actions or merge was performed.
