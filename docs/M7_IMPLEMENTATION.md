# M7 — Arabic content, configurable local games, descriptive quiz results

Status: source-only changes on top of `feature/m6-media-cards` (not a release).

## Functionality

- Source catalogs expanded by **60 newly authored Arabic challenges** and **35 newly authored Arabic questions**. Totals: **230 prompts** (130 couple / 100 party) and **175 questions** (7 packs of 25).
- Canonical generator still named `tools/generate_content_v2.py` for continuity; it now expects content metadata **version 3**. `tools/create_content_m4.py` is a *legacy authoring tool* and MUST NOT be rerun against the new catalogs.
- Offline Kotlin `SessionFactory` supports 6, 10, 12, or 18 rounds; fixed or lower-neighbor mixed difficulty; up to 60 custom prompts; recent prompt avoidance when alternative prompts exist. It preserves rotation fairness and unique prompts in each game.
- Compose local-game UI exposes those controls and a round progress bar. The history stores only last-game challenge text in the existing encrypted LocalVault. No history is uploaded.
- Offline quiz UI now offers 8, 12, 20, or all 25 questions per pack. Descriptive response counts/percentages and exact agreement replace personality-style conclusions in visible results.
- Remote quiz results now additionally expose per-participant option counts but retain the legacy numerical scores field for compatibility with source-level historical tests; **this score is not presented as a validated measure**. The Node server still sees regular chat/quiz responses; M5 E2EE privacy remediation remains outstanding.
- The remote service selects eight unique questions out of 25 per pack. Web and Android now authenticate with `contentVersion: 3`. A mismatched or absent version is rejected with WebSocket status **4009**; deploy server and both clients together. M6-only clients cannot join an M7 relay.

## Boundaries

- Remote game rooms still use their original 10 or 12 rounds; local configurable rounds and local recent-history preference do not yet synchronize into remote sessions.
- No Android build, UI instrumentation, deployment, browser E2E, or HTTPS/WSS live tests were run.
- No verification of subjective Arabic content quality by native-speaking focus groups. Quiz questions are entertainment, not diagnostic instruments.
- M6 upload/preview and security gaps remain open. Do not merge or label release ready until device integration tests.

## Verified checks

```
python tools/generate_content_v2.py --check
(cd backend && node --test test/*.test.mjs)       # 20 passing
# Kotlin standalone core smoke tests:
kotlinc app/src/main/java/com/r2h/sirr/core/{GameLogic,ExperienceLogic,ExpandedQuizzes,AdultChallengesV2,MoreQuizzesV2,QuizInsights}.kt tools/M7SmokeTest.kt -include-runtime -d /tmp/sirr_m7_features.jar
java -jar /tmp/sirr_m7_features.jar
```
