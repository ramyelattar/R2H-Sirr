# R2H-Play — Couple Challenges feature: source close-out

**State:** owner-approved v10 editorial copy; Android source integrated with conservative runtime eligibility; **NOT compiled, tested, merged, or released**.

## Editorial source: immutable

- Owner-uploaded baseline: `content/couple_challenges_style_v10_original.json`.
- Owner-approved wording, spelling edits and exact timing fields: `content/couple_challenges_v10_refined.json`.
- Exactly 100 entries across SOFT / NORMAL / SEXY / HOT / HARD (20 each).
- All original `id`, `kind`, `title`, level and `seconds` values are preserved in approved content. 14 wording-only corrections were accepted by the owner. The 10 text-vs-seconds differences were accepted unchanged.
- The source JSON is generated to `CoupleStyleV10Catalog.kt` using `tools/generate_couple_style_v10.mjs`; this generator has **not** been executed for verification.
- The prior v2 JSON and Kotlin bank remain in the repository as historical material; new local couple sessions now select from v10 through `CoupleV10Policy`, not from v2.

## Safety and visibility policy

The owner approved the **text**, not physical safety. The separate `CoupleV10Policy` keeps all 100 records in the source but excludes 19 previously flagged IDs from automated challenge drawing. Eligible counts: SOFT 16, NORMAL 20, SEXY 20, HOT 17, HARD 8. This list is targeted and is **not a certification that the remaining 81 are risk-free**. No flagged wording is silently modified. Further activation of flagged content is not inferred from editorial approval.

One-level sessions do not draw from harder levels and respect the available count. The mixed-level option explicitly uses the chosen level plus the next lower one. The screen adjusts available round-count options (6, 10, 12, 18) to prevent requesting an impossible unique draw. In the HARD one-level setting only 6 is currently feasible without repeats; selecting mixed-level allows longer sessions.

## Feature flow integrated in source

- Couple session setup with two named players, 5 intensity choices, custom challenge pack, rounds, advisory overall time limit (15/30/60 min or no limit), reward selected before start, optional agreed shared tie-break challenge.
- Alternating player turns, v10 title/type/duration display, reveal and two-party consent.
- Duration-based persisted countdown with start, pause, resume; revoking either partner's consent pauses a running timer.
- Score changes per round: completed +10, not completed -10, mutually agreed skip 0. A deliberate stop ends without a winner or reward.
- On points tie: one shared challenge, then two pre-registered knowledge questions if necessary. Subsequent tied question rounds repeat until resolved or participants choose to stop.
- End screen with totals and breakdown, declared winner and previously agreed reward, with separate consent to the reward.
- Offline user-authored TXT challenge import/export. Content is encrypted within the existing app vault; exported TXT is plaintext and clearly labelled as such.
- New session persistence format **v4** records the original v10 `kind` as `actionCode: String` plus the legacy broad UI category. Reads v1/v2/v3 records with backward-compatible defaults. The existing party mode remains v1 with legacy +1 scoring.

## Verification and release constraints

**Do not claim release readiness from source changes.** No Android Gradle build, Kotlin unit test, instrumentation test, APK install or device runtime review has been performed. A static source inspection cross-referenced 100 v10 catalog entries, the 19 exclusions and the v4 save/read code; that inspection is **not** compile or test evidence. Kotlin test source currently declares 24 cases but none have been executed.

Remaining acceptance checks before merging/release:
- Android compilation and JVM tests on supported Gradle/JDK/SDK.
- Instrumented encrypted-vault persistence round-trips for versions v1–v4.
- Real-device and process-death resume, countdown/consent, scores, exit, shared tie-break and knowledge question behavior.
- Regression checks for party, remote, and other app routes, as well as actual Android RTL, accessibility and portrait layouts.
- Confirm that the excluded 19 are never selected, including custom/fallback paths, and that the user understands the eligible count.
- Preserve frozen editorial content unless owner explicitly requests a change.

This is **source-level completion of the currently eligible subset**, not complete operational acceptance of all 100 challenge records.
