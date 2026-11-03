# R2H-Play — v10 preservation & editorial review

Status: **owner-approved editorial content as-is (2026-11-03); preserved and staged, not yet activated in the Android game**.

- The owner-supplied v10 source remains exactly preserved as `content/couple_challenges_style_v10_original.json`.
- The separate `content/couple_challenges_v10_refined.json` makes 14 small spelling/grammar edits; see the review JSON.
- All 100 `id`, all 100 unique `kind` values, five levels, original order, all titles and all `seconds` values are retained.
- 10 timing differences were recorded; the owner accepted the current `seconds` values as-is. Text and timing fields remain unchanged.
- 19 entries have targeted safety/consent flags. These are warnings, not an exhaustive audit. No flagged text was silently replaced.
- Owner editorial approval is recorded for the exact current refined v10. The previously active v2 catalog, current Kotlin model and sessions remain untouched; v10 remains a separate, staged catalog pending a runtime integration/safety decision.
- No Gradle build, APK installation, unit tests, or runtime tests were run.

## Compatibility plan without loss of original `kind`

- v10 unique `kind` strings are **action codes**, not values of the historical seven-value `CoupleChallengeKind` enum.
- Add a distinct staged v10 data model with `kindCode: String`; never cast original v10 kinds to the legacy enum.
- The generated staging catalog remains disconnected from the current session UI. Editorial approval is recorded, but runtime activation remains a separate integration and safety gate.
- Subsequent activation should add a distinct `actionCode` persistence field to saved sessions, with backward-compatible defaults for v1/v2/v3 records.
- Avoid silently activating flagged entries. Maintain a visible review/acceptance workflow before enabling them.

## Review priority

1. Resolve the runtime treatment of dangerous/no-stop instructions (IDs listed in `content/couple_challenges_v10_review.json`) without modifying owner-approved copy silently.
2. Preserve the 10 owner-accepted timing differences as-is; do not normalize text or `seconds` without a new request.
3. Preserve the owner's level placements; do not relabel automatically.
4. Owner-approved language edits are frozen. Runtime activation requires a separate reviewed change; do not infer that owner approval certifies physical safety or compile readiness.

## Owner acceptance — 2026-11-03

The owner explicitly approved the challenges **exactly as they currently appear** in `content/couple_challenges_v10_refined.json`. No additional text, title, level, kind, duration or ordering changes are authorized. The original v10 remains preserved byte-for-byte for reference. Acceptance is editorial/product intent only, not evidence of runtime safety, build success or testing.
