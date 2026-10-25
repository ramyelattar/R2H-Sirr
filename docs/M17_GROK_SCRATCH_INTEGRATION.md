# R2H-Play M17 — Scratch content integration

## Origin
Imported the user's uploaded `ScratchScreenV2.kt` Grok content onto the existing M16 scratch-screen implementation, without replacing the tested-in-source scratch engine. All 20 source groups retained in their original order.

## Implementation
- Imported 20 ordered groups of 3 messages each (60 total).
- Fixed one verbatim duplicate between the 15th and 51st message by appending an opt-in consent phrase to the latter. All 60 messages are now distinct and below 280 characters.
- Random group selection always presents related stage 1/2/3 text together; users may still select 1, 2, or 3 stages.
- Cleared all 3 composed messages on explicit session close and expiry; the original M16 code preserved them in screen state.
- Kept M16 `ScratchPanel` byte-identical, including scratch gestures, progressive reveal and effects.
- Preserved gallery photo import/vault deletion, 3/6/24-hour TTL, all existing components and page layout.

## Editorial review required
User-provided text includes explicit sexual content, forceful language, pain/crying cues, and repeated narrow anatomical phrasing, with female-targeted address throughout. Structural validity is not a content, consent or app-store approval. This PR should stay draft pending review of opt-in and skip mechanics, suitability of the target audience and any distribution requirements.

## Validation
20 groups, 3 messages each; 60 unique messages; max message length 280; protected UI/state paths found; balanced code delimiters; M16 ScratchPanel byte-exact preservation. **No actual Gradle/Kotlin compilation, APK build, on-device test, CI execution or merge performed.**
