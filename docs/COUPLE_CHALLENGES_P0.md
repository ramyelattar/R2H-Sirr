# R2H-Play — Couple Challenges P0

Status: **source implemented on an isolated branch; not built, not tested, not accepted**.

## Scope

This change covers **offline local two-player couple challenge sessions**. It does not alter the party mode, remote backend protocol, quizzes, whispers, scratch, or dice content. The original shared challenge bank remains intact.

## Content architecture

- Canonical editorial file: `content/couple_challenges_v1.json` (100 original non-graphic drafts, 20 per intensity).
- Generated Kotlin catalog: `app/src/main/java/com/r2h/sirr/core/CoupleChallengeCatalog.kt`.
- Deterministic local generator: `node tools/generate_couple_challenges.mjs`.
- Planned future sync check: `node tools/generate_couple_challenges.mjs --check` (**not run**).
- Six built-in challenge kinds: talking, whispers, choice, closeness, together, surprise.
- User-authored activities are marked `PERSONAL`, editable as one TXT line per activity (up to 60 prompts of 6–500 characters). Import and export use Android's document picker; encrypted in-app persistence uses the existing local vault.
- **Exported TXT is plaintext outside the app** and must be handled as sensitive by the owner.
- When the user's private pack has enough activities for a session, the game chooses only from those custom activities. A smaller pack is supplemented with built-in content.
- Fresh prompts are prioritized over recently seen prompts; within a session prompt text does not repeat. New local couple selection balances activity kinds rather than repeating only one category.
- Built-in catalog is **not approved as erotic/explicit final copy**. It deliberately includes only a draft adapted from the user's challenge-001 direction and additional non-graphic draft activities. Owner review or user-authored private content is required to reach the desired editorial identity.

## Competition rules (approved product intent)

- Both partners agree on the reward, game intensity, number of challenges (6/10/12/18), and optional duration (15/30/60 minutes or unlimited).
- The two local player roles alternate each round.
- The participant responsible for the challenge receives +10 for a completed activity; -10 for activity not completed; 0 for noncompletion explicitly agreed by both.
- Either partner can decline, end, or interrupt any interaction. Scores represent only the game's voluntary bookkeeping and must not enforce a sexual act or a reward.
- Challenge countdowns have per-activity durations; the first HOT challenge (the user's challenge 001) uses 300 seconds.
- Completing a timed activity is only eligible for +10 after the countdown has elapsed; time spent paused does not count.
- The round timer is persisted in encrypted session v3, supports pause/resume, and keeps elapsed time across process restoration. The overall session timer is an advisory threshold: when reached, users may continue or settle scored rounds without automatic negative scoring.
- The scoreboard and end-of-session summary show scores and results per player; the winner sees the previously agreed reward.

## Tie-break

1. Equal points after normal scoring triggers one shared challenge. Players may define its wording before the session.
2. If exactly one partner completes that challenge, that partner wins, with no additional regular points.
3. If both or neither completes it, each author writes one question about themselves and a reference answer.
4. Both answers are committed to encrypted storage before either partner answers.
5. Only one correct response decides the winner. If both correct or both wrong, use two new questions.
6. Reference answers are removed from the tie-break state after the winner is decided.
7. The winner's reward remains subject to both partners' agreement. Partners may end the tie-break without a winner or reward at any stage.

## Persistence and backward compatibility

- Legacy session v1 is still read with +1/skip legacy semantics.
- Draft competition v2 is still accepted on load.
- New couple competition v3 persists: rules, reward, tie challenge, outcomes, typed activity metadata, countdown state, tie-break state, and scores.
- Load rejects malformed scores, invalid schema versions, and impossible tie states. The previous project/user data is not modified automatically.
- Party sessions retain v1 persistence and legacy scoring.

## Not yet verified

No Android compilation, tests, APK installation, runtime review, or GitHub Actions success has been demonstrated. GitHub Actions previously returned `startup_failure` before executing jobs.

Unit-test source has been added for scoring, timing, early stop, custom content, alternating turns, and tie states. These tests have **not been executed**.

## Deferred verification after feature-content approval

- Confirm JSON generator sync and baseline content tests.
- Compile Android on JDK 17 / Gradle 8.13 / Android SDK.
- Execute Kotlin unit and data round-trip migration tests for v1/v2/v3 on representative isolated data.
- Test start/pause/resume/expiry after process death and screen changes.
- Verify score integrity for all outcomes, including cancellation, 0-point mutual agreement, timer completion, and tie-break.
- Verify no regressions to party, remote, quiz, dice, scratch or whispers.
- Review real Android portrait layouts, accessibility, Arabic RTL and screen-reader labels.
- Run dedicated real-device tests and confirm the APK behavior before merging into `main`.

## Owner/content acceptance remaining

The owner explicitly asked to finish the complete feature before building and testing. Source implementation alone is **not** evidence of readiness. The non-graphic draft copy is subject to editorial review and replacement where the owner wants different user-provided private copy; do not automatically treat it as accepted.
