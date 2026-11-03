# R2H-Play — v10 preservation & editorial review

Status: **owner-approved editorial content as-is (2026-11-03); gated v10 routing implemented in source, not compiled or runtime-verified**.

- The owner-supplied v10 source remains exactly preserved as `content/couple_challenges_style_v10_original.json`.
- The separate `content/couple_challenges_v10_refined.json` makes 14 small spelling/grammar edits; see the review JSON.
- All 100 `id`, all 100 unique `kind` values, five levels, original order, all titles and all `seconds` values are retained.
- 10 timing differences were recorded; the owner accepted the current `seconds` values as-is. Text and timing fields remain unchanged.
- 19 entries have targeted safety/consent flags. These are warnings, not an exhaustive audit. No flagged text was silently replaced.
- Editorial approval is recorded for the exact v10 wording. New local couple sessions are routed through `CoupleV10Policy` to the v10 catalog; the historical v2 catalog remains on disk but is no longer the new-session source. Existing saved sessions are unaffected.
- No Gradle build, APK installation, unit tests, or runtime tests were run.

## Compatibility plan without loss of original `kind`

- v10 unique `kind` strings are **action codes**, not values of the historical seven-value `CoupleChallengeKind` enum.
- A distinct generated v10 data model retains original `kindCode: String`; broad UI categories are derived separately, never replacing owner kind values.
- The v10 catalog is connected to new local sessions through `CoupleV10Policy`; its 19 flagged entries remain excluded from automatic selection. No Android compilation or device verification has been performed.
- `actionCode` is stored in v4 encrypted session persistence, with reading support for v1/v2/v3 saved records.
- Never silently activate the 19 flagged entries. Eligible counts: 16 / 20 / 20 / 17 / 8 by level from SOFT to HARD. Safety decisions remain distinct from approved copy.

## Review priority

1. Resolve the runtime treatment of dangerous/no-stop instructions (IDs listed in `content/couple_challenges_v10_review.json`) without modifying owner-approved copy silently.
2. Preserve the 10 owner-accepted timing differences as-is; do not normalize text or `seconds` without a new request.
3. Preserve the owner's level placements; do not relabel automatically.
4. Owner-approved language edits are frozen. Runtime activation requires a separate reviewed change; do not infer that owner approval certifies physical safety or compile readiness.

## Owner acceptance — 2026-11-03

The owner explicitly approved the challenges **exactly as they currently appear** in `content/couple_challenges_v10_refined.json`. No additional text, title, level, kind, duration or ordering changes are authorized. The original v10 remains preserved byte-for-byte for reference. Acceptance is editorial/product intent only, not evidence of runtime safety, build success or testing.

## Source integration update

New couple sessions use the owner-approved v10 text through `CoupleV10Policy`. Only the 81 non-excluded records are automatically eligible; all 100 remain intact in both original and approved files. This is a source implementation statement only. The app has **not** been compiled, tested or merged, and the remaining safety flags are not waived by approval.
