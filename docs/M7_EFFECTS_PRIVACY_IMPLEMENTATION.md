# M7 Effects + Private Text/Quizzes — Source-only

24 October 2026. Based on the M7 remote-customization branch (which includes M6).

## Implemented
- Android: per-app persisted local preferences for generated audio tones, optional vibration, cinematic Compose transitions and particle bursts, respecting system animation-scale setting.
- Browser: opt-in WebAudio tones and vibration, visual bursts with reduced-motion behavior, user-only local preferences.
- Lobby: host edits 6/10/12/18 rounds, intensity, difficulty mixing and up to 20 encrypted Arabic custom prompts before other players consent; explicit consent replaces auto-acceptance on web.
- Text chat: AES-256-GCM client-side with 96-bit random IV and room-scoped AAD, never plaintext on the relay.
- Quiz answer privacy: 8 HMAC-SHA256 tags per player with domain separation including room UUID, quiz UUID, question index and choice. Server compares equality only, discards tags after both submit, returns agreement count; it does not receive raw choices.
- WebSocket rejects previous clients without protocolVersion=4 (and contentVersion=3).
- Shared invite key is retained in the URL fragment: compromise of invite compromises session privacy.

## Local checks
- Node server unit tests: 27/27 passed (including updated private quiz tests and new ciphertext-only/lobby tests).
- Node syntax checks on JS modules passed.
- Canonical Arabic content check: version 3, 230 challenges and 175 questions.
- Kotlin pure-JVM AES-GCM/HMAC core compiled and ran known-vector smoke test.
- Kotlin vectors match independent Node crypto vectors.
- All 49 browser client DOM id references resolve in the HTML.

## NOT verified
- NO Gradle/Android SDK or APK, Kotlin Compose/Android-framework compile, UI instrumentation, connected two-device Android/WebCrypto test, deployed HTTPS/WSS or independent security audit.
- Server still learns participant metadata, challenge catalog selection, quiz agreement result and media sizes.
- This is a shared group key without per-user identity verification or forward secrecy; dishonest clients can send arbitrary HMAC tags and falsify quiz agreement.
- Browser audio needs user interaction; haptic support varies. Sounds are short generated cues, not a full sound library.
- Existing M6 file transport and server-restart persistence limitations remain.

Do not merge to main or mark release-ready without separately authorized builds and device acceptance.
