# R2H-Play — Editorial review of owner-supplied Couple Challenges v2

**Status:** Source cleanup committed; editorial acceptance pending. No Android build or automated tests executed.

## Summary

- 100 original owner-authored challenge IDs preserved across 5 × 20 levels.
- 60 entries changed, including 50 text edits and 5 title edits.
- 12 duration fields updated to reduce contradictions with stated activity times.
- The six activity kinds, order, and level membership are unchanged.
- User-supplied graphic detail was not systematically expanded or invented; edits correct wording, clarity, agreement and a few contradictory instructions.

## Activities requiring manual editorial acceptance

| Challenge ID | Why review |
|---|---|
| `couple2-SOFT-5` | Wording, logic or consent clarification may alter nuance |
| `couple2-SOFT-20` | Wording, logic or consent clarification may alter nuance |
| `couple2-NORMAL-2` | Wording, logic or consent clarification may alter nuance |
| `couple2-NORMAL-5` | Wording, logic or consent clarification may alter nuance |
| `couple2-NORMAL-11` | Wording, logic or consent clarification may alter nuance |
| `couple2-NORMAL-16` | Wording, logic or consent clarification may alter nuance |
| `couple2-NORMAL-18` | Wording, logic or consent clarification may alter nuance |
| `couple2-SEXY-5` | Wording, logic or consent clarification may alter nuance |
| `couple2-SEXY-8` | Wording, logic or consent clarification may alter nuance |
| `couple2-SEXY-17` | Wording, logic or consent clarification may alter nuance |
| `couple2-HOT-5` | Wording, logic or consent clarification may alter nuance |
| `couple2-HOT-17` | Wording, logic or consent clarification may alter nuance |
| `couple2-HARD-1` | Wording, logic or consent clarification may alter nuance |
| `couple2-HARD-8` | Wording, logic or consent clarification may alter nuance |
| `couple2-HARD-11` | Wording, logic or consent clarification may alter nuance |
| `couple2-HARD-20` | Wording, logic or consent clarification may alter nuance |

## Timer corrections

| Challenge ID | Before | After |
|---|---:|---:|
| `couple2-SOFT-6` | 180s | 60s |
| `couple2-NORMAL-6` | 180s | 60s |
| `couple2-SEXY-5` | 120s | 60s |
| `couple2-SEXY-11` | 90s | 30s |
| `couple2-SEXY-12` | 180s | 120s |
| `couple2-HOT-3` | 150s | 120s |
| `couple2-HOT-6` | 180s | 240s |
| `couple2-HOT-12` | 200s | 180s |
| `couple2-HOT-18` | 200s | 180s |
| `couple2-HARD-2` | 180s | 120s |
| `couple2-HARD-8` | 180s | 120s |
| `couple2-HARD-15` | 150s | 120s |

## Canonical files and controls

- Source: `content/couple_challenges_v2.json` (version 2).
- Kotlin generated representation: `app/src/main/java/com/r2h/sirr/core/CoupleChallengeCatalog.kt`.
- Generator: `tools/generate_couple_challenges.mjs` (now reads v2).
- Original owner upload remains available in the conversation; the repository's v1 catalog remains as an archived draft, not the canonical runtime source.
- The Kotlin catalog is the source read by new offline local couple sessions. Neither remote content nor party mode was updated.
- The GitHub repo is private; the changes are on an unmerged draft branch.

## Remaining

- The owner must approve the wording and whether the selected SOFT/NORMAL/SEXY/HOT/HARD placement fits the desired progression.
- Distinguish copy editing from reclassifying levels. No automatic redistribution was performed.
- Compilation, test execution, real-device UI testing and formal acceptance are intentionally deferred until feature/content review is complete.
