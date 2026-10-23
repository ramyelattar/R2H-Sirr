# Arabic-Only 18+ Interactive Games for Android — Scoped Product Requirements

**Version:** 1.0 — 2026-10-23  
**Reference site:** https://interactivewish.com/en/  
**Goal:** Build an independent Android application, Arabic-only and RTL, inspired by the adult-oriented interactive functionality of InteractiveWish. This is not a WebView wrapper or an extraction of the reference site's code, media, or proprietary content.

## 1. Authoritative scope

### Include

1. **تحديات الشريكين (1 vs 1):** consent-based invitations; selectable intensity levels; ten synchronized rounds as documented by the reference; text / voice / image / short-video response capabilities; per-turn lifecycle, optional completion/skip, graceful reconnection, and an end-of-session screen.
2. **تحديات المجموعة (Party):** 2–4 participant slots; participant invitation; shared state; selectable intensity; roulette for roles / task category; 12 rounds and score/closing challenge (documented on the reference landing page); participant removal and withdrawal. One participant slot may represent a couple, according to the source; design this explicitly rather than assuming every slot is one human.
3. **النرد التفاعلي (Dice):** three dice, six faces per die; Soft, Normal, Sexy, Hot, Hard and Custom presets; custom face labels and images; weighted randomness with per-die weights summing to 100%; re-roll face; roll duration choices (5/7/10/13/20 seconds), sound on/off, save/reset presets. Ensure reproducible unit tests for weighted selection.
4. **اختبارات الكبار (Quizzes):** seven thematic collections present on the reference (identity; love/bonds; compatibility; seduction/power; desire; shadow; lust/sin); adult-only selection while retaining softer difficulty levels within the adult module; solo completion; optional pair comparison; results based on documented, locally defined scoring rules. **Author original Arabic question banks and scoring descriptions** — do not copy the reference's protected questions or fabricate a claim that its exact scoring is known.
5. **الكشط والكشف (Adult scratch):** Sexy reveal and related adult-appropriate multi-layer reveal / puzzle modes only; photo, custom Arabic text, tactile scratch gesture, progress threshold, preview and configurable expiration. Exclude family celebration themes.
6. **همسات خاصة (Whispers):** three private messages, optional names, photo at the end, short signature, optional music, one-time viewing, short-lived share link / expiration options. Respect different source character limits if retained, but validate for Unicode grapheme clusters.

### Exclude

Birthday, Father's/Mother's Day, pregnancy/gender reveal, proposal, wedding invitations, general love greeting cards, fake news, blogs, and external book sales. No account, payment or cloud AI dependency in the baseline.

### Sources of confirmed reference functionality

- Home adult grouping: https://interactivewish.com/en/
- Couple challenge / 10 rounds: https://interactivewish.com/en/couple-card/
- Couple and Party marketing description: https://interactivewish.com/en/landing-page/couple-cards/
- Dice controls: https://interactivewish.com/en/erotic-dice/
- Quiz groups: https://interactivewish.com/en/quiz-card/
- Adult scratch fields: https://interactivewish.com/en/scratch-card/
- Whispers fields: https://interactivewish.com/en/whispers-card/

**Evidence boundaries:** Browsable content and controls were inspected, but no authorized live multi-device end-to-end test of the third-party site was conducted. Its transport protocols, source implementation, production scoring algorithms and retention guarantees remain unverified. Build independent, testable behavior instead of imitating undisclosed internals.

## 2. Languages, navigation, and UX

- All application-owned strings in **Arabic only**, with native right-to-left layout, Arabic typography, localized numeral/date handling where relevant, and accessible Arabic content descriptions. User-authored content can naturally be in other languages.
- Native Kotlin, Compose UI, Navigation Compose, Material 3. No default English fallbacks visible in release UI; CI detects untranslated string keys / hardcoded strings in Compose code.
- Dashboard: تحديات الشريكين، تحديات المجموعة، النرد، الاختبارات، الكشط والكشف، الهمسات، جلساتي، الإعدادات.
- Core flows: home → select experience → select level / create → preview → local play or invite → interact → conclude / expire.
- Privacy-first look: discreet app title / launcher icon, no sensitive content in notifications or Android recents, explicit biometric lock option and clear exit buttons.
- Age-screen 18+ before access, explicit participant consent and opt-out at every turn. An age declaration is only a preliminary control and not a substitute for applicable age-assurance requirements.
- Accessibility: TalkBack semantics, sufficient contrast, reduced-motion option, no required audio response, touch targets >=48 dp, robust font scaling.

## 3. Functional behavior and requirements

### Modes

- **Local (offline):** dice, quizzes, scratch creation/viewing, locally drafted whispers, and optionally turn-taking on one device work with no internet.
- **Remote:** participants joining from separate networks, link sharing, reliable synchronized game state, and remote media transfer require internet connectivity and an endpoint. Do not claim remote multiplayer is fully offline.
- **Browser recipient:** if recipients are not required to install Android app, implement a separately hosted, Arabic RTL web player for invite links. An Android APK alone cannot make those links work across arbitrary browsers.

### Shared game state

- Session statuses: CREATED, WAITING, ACTIVE, PAUSED, ENDED, EXPIRED, REVOKED.
- Participants: anonymous per-session identities, join consent, seat ownership, leave/rejoin and expulsion.
- Rounds: deterministic server-authoritative index, current actor, challenge card ID, allotted submission channel, duration (if applicable), completion or skip; deduplicate delivery.
- Network: sequence numbers/acknowledgements, replay on reconnect, timeout, and explicit conflict policy; retry must never unintentionally reveal or duplicate media.
- Party: roulette outcomes generated once per round by authoritative logic and broadcast to everyone; fairness test for random selection; scoreboard calculations unit-tested.

### Dice

- Each of 3 dice has 6 editable faces and separate probability weights.
- Validate nonnegative weights and sum == 100% (or normalize with a clearly labeled user action).
- Handle reroll faces without infinite loops; bounded retry and deterministic fallback.
- Persist presets and user edits locally, not to external cloud.

### Quizzes

- 7 collections; content stored as versioned local JSON or SQLite seed data with stable question/answer IDs.
- Distinguish quiz completion from scoring/interpretation; mark results as entertainment, not clinical/diagnostic measurement.
- Support comparison by invitation with both parties consenting; do not disclose one person's private answers by default.

### Media and expiry

- Android Photo Picker, scoped storage, image resizing and safe file-size/format constraints; AudioRecord/camera permissions only at point of use.
- Ephemeral media must have documented limits (size, duration, retention). An expiration or one-time view means the service deletes or revokes stored payload and removes local cached copies; it cannot prevent photographing a screen externally.
- Prefer client-side encryption before uploading. If promising end-to-end encryption, implement an audited protocol; do not market E2EE until it is verified.
- Redacted logs, no sensitive analytics, backups disabled for sensitive local data, no arbitrary media URL exposure.
- Abuse controls: report/block, join-token expiry, invite revocation, rate limits, content ownership attestation and explicit no-nonconsensual-media rules.

## 4. Architecture

### Android

- Kotlin + Jetpack Compose + Coroutines + Flow + ViewModels
- Navigation Compose; dependency injection (Hilt or Koin, choose one)
- Room for local structured state; Android Keystore-backed encryption for secrets, encrypt sensitive payloads and media files at rest
- OkHttp WebSocket / Retrofit for remote signaling; Media3 for playback; Android Photo Picker; WorkManager only for best-effort cleanup/retries
- Optional offline-only model via ONNX or llama.cpp **only if a feature genuinely needs AI**, with no dependency on external AI APIs
- Architecture modules: `core:design`, `core:localization`, `core:security`, `core:data`, `core:network`, `feature:couple`, `feature:party`, `feature:dice`, `feature:quiz`, `feature:scratch`, `feature:whispers`, `feature:sessions`, `app`

### Optional self-hosted service

- Rust (Axum) HTTP + WebSocket, with database and explicit TTL cleanup, single-use invitation tokens and session identity checks
- Web invitation client built with React/TypeScript, Arabic-only RTL
- Use store-and-forward encrypted blobs for brief disconnects if necessary; do not falsely claim the backend stores no media when it does
- Self-hosted infrastructure is not equivalent to offline operation. Document what is retained and for how long
- Media envelope limits + deletion worker + secure metadata defaults + audit of auth rules

## 5. Security, legal and distribution gates

- Restrict to adults; explicit and revocable consent by all participants; never solicit non-consensual imagery.
- No unsolicited sharing, public discovery feed or searchable profiles in baseline.
- Provide delete local data, revoke pending invitation, expire session, and clear cached media actions.
- Private-install APK is the baseline distribution assumption, **not** an assertion of legal compliance. Validate content/distribution against laws where operated or distributed.
- Google Play's sexual-content policy may prohibit publication of an app primarily offering sexually gratifying content; do not assume an age gate alone allows Play listing. Reference: https://support.google.com/googleplay/android-developer/answer/9878810
- Create original source code, original designs, original Arabic question/challenge content and original branding. Do not extract or republish the reference site's proprietary assets or text.

## 6. Delivery milestones

### M1 — Native offline MVP

- Arabic-only UI / RTL / age gate / settings / six module navigation placeholders with functioning local data
- Functional customizable dice, solo quizzes with original Arabic question packs, adult scratch and offline creation/preview of whispers
- Local data deletion and basic privacy hardening

### M2 — Pair play

- Create/join/exit pair game, 10 synchronized rounds, invitation links / Web RTL join, text replies and stable reconnect
- Encrypt shared media when implementation is tested; photos, audio, video with constrained duration/size; correct expiration

### M3 — Group game and comparison

- 2–4 seats, roulette, 12 rounds, score and closing screen
- Quiz comparison by invite, shared constraints for participation/consent

### M4 — Release quality

- Tests, security audit, accessibility, Android battery and thermal profiling, release signing plan, reproducible APK build
- Explicit native-vs-web parity testing for invited users

## 7. Non-negotiable acceptance tests

1. Arabic-only strings and correct RTL across every screen, including dialogs, validation and error states.
2. Dice preset distribution validated statistically and per-die weight validation unit tested.
3. Quiz scores consistent offline/restart; original content and content-version migrations covered.
4. Two real devices synchronize a 10-round pair session; reconnect and duplicate delivery behave correctly.
5. A group of 4 distinct clients runs 12 rounds with one agreed roulette outcome each round and a reproducible scoreboard.
6. Expired/revoked invite links fail; no old session leakage after logout/reinstall/reuse of token.
7. Recipient can use link from supported browser without Android APK **only if** browser client is a committed deliverable.
8. Photos/audio/videos never appear in cleartext logs or unprotected disk cache; cleanup after expiry is tested.
9. Accessibility and performance on Android 8–12 GB RAM midrange hardware; avoid always-on background networking or local AI runtime.
10. All capabilities are shown as VERIFIED / PARTIALLY VERIFIED / NOT YET TESTED in the final engineering handoff; do not claim functionality solely because code compiles.

## 8. Implementation agent brief (copy into Codex)

> Implement a new native Android application scoped **only to the Arabic-language, adults-only interactive modes** documented in this PRD. Do not implement non-adult cards, gifts, news, invitations or book catalog. Use Kotlin, Jetpack Compose, modular MVVM, and privacy-first local storage. Build M1 end-to-end and test it before implementing any remote backend. Do not use WebView wrappers, copied proprietary content/assets or cloud AI. Make all UI and built-in game content Arabic RTL; create original question and challenge banks and ensure age/consent gates. Keep offline interactions completely functional without a server. Define explicit API contracts and test stubs for future pair/group multiplayer; do not present mock synchronization as real. Follow M1→M4 milestones, complete each with reproducible verification evidence, and report blockers rather than invent success. Only build release APK after acceptance criteria are met. Observe applicable distribution restrictions and make no assertion that age confirmation alone ensures legal or Google Play compliance.
