# M22-A — Informed choice and renewed participation consent

## Changes
- `ExperienceScreen.kt`: warning on intensity, checkbox for active actor after each newly revealed challenge; “challenge completed” button disabled until the current actor confirms. Existing skip/quit options remain and do **not** require consent.
- `QuizScreen.kt`: replaced misleading all-light title; per-pack explicit/personal content disclosure and opt-in prior to entering questions, reset on topic or new session; retains separately collected consent for second player.

## Limitations
- Each actor's checkbox is not a substitute for other participants' consent. UI explicitly instructs asking each other participant before any contact or shared activity. Consent is revocable at any time.
- This is UI acknowledgement, not independent age verification.
- Before deployment, M22-B must implement quiz-question skip (currently mentioned in preview but not yet implemented); maintain draft status until then.
- Original content, game rules, scores, storage, server, generated catalogs, and data are untouched.
- No build, tests, APK or device execution.
