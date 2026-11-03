# R2H-Play — v10 preservation & editorial review

Status: **preserved and minimally edited; not approved or activated in the Android game**.

- The owner-supplied v10 source remains exactly preserved as `content/couple_challenges_style_v10_original.json`.
- The separate `content/couple_challenges_v10_refined.json` makes 14 small spelling/grammar edits; see the review JSON.
- All 100 `id`, all 100 unique `kind` values, five levels, original order, all titles and all `seconds` values are retained.
- 10 timing inconsistencies are recorded but left unchanged pending an owner decision.
- 19 entries have targeted safety/consent flags. These are warnings, not an exhaustive audit. No flagged text was silently replaced.
- The previously activated v2 catalog, current Kotlin model and sessions remain untouched until v10 is approved.
- No Gradle build, APK installation, unit tests, or runtime tests were run.

## Compatibility plan without loss of original `kind`

- v10 unique `kind` strings are **action codes**, not values of the historical seven-value `CoupleChallengeKind` enum.
- Add a distinct staged v10 data model with `kindCode: String`; never cast original v10 kinds to the legacy enum.
- The generated staging catalog remains disconnected from the current session UI until editorial approval.
- Subsequent activation should add a distinct `actionCode` persistence field to saved sessions, with backward-compatible defaults for v1/v2/v3 records.
- Avoid silently activating flagged entries. Maintain a visible review/acceptance workflow before enabling them.

## Review priority

1. Resolve dangerous/no-stop wording (IDs listed in `content/couple_challenges_v10_review.json`).
2. Confirm whether the written time or `seconds` field is authoritative for each of 10 mismatches.
3. Confirm level labels match the owner's preferred progression; do not relabel automatically.
4. Approve minimal language edits, then activate v10 through a separate reviewed change.
