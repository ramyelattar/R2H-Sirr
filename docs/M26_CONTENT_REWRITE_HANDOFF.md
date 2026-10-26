# M26 — Editorial rewrite before next local APK

Branch: `feature/m26-egyptian-content-rewrite`; base: M25.3.

## Rewritten, not simply flagged
- **138 / 230 challenges** in couple SOFT/NORMAL/SEXY and party SOFT/NORMAL/SEXY replaced with distinct, coherent, playable Egyptian Arabic instructions. The already more coherent 92 HOT/HARD entries were preserved.
- **175 / 175 quiz questions** reauthored in seven meaningful subject areas with **525 contextual three-way answers**, rather than random generic or malformed sentences. All quiz groups have 25 items. **9 short responses** subsequently improved for readability.
- **60 / 60 scratch prompts**, arranged in 20 three-step story groups, revised to comprehensible reveal/progression/question arcs.
- **60 / 60 whisper suggestions**, in 20 consecutive three-message stories, revised with a beginning, development and final question. Secret selection and ephemeral clearing logic preserved.
- Canonical JSON and generated Kotlin are updated **together**, and backend public quiz/challenge JSON follows canonical JSON byte-for-byte. No Android business, security, media, drawing, or visual changes.

## Counts and checks
- 230 unique challenges, 26 per couple difficulty (5 levels), 20 per party difficulty (5 levels).
- 175 unique questions; 525 answered-choice strings (three per question). Preserve 7×25 layout.
- 60 whisper suggestions, 60 scratch messages. All source-only review.
- Near-duplicate scan (normalized Jaccard similarity >= .73) was performed on the challenge and quiz question lists.
- The static build candidate is **not a claim of APK compilation or human editorial sign-off**; local owner build supplies actual compile evidence.
- App identity unchanged: `com.r2h.sirr`; versionCode 13; versionName `0.9.4-m26-content`. Supports in-place upgrade if signed with the same key.

## Local-only build
From this branch run `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Build-M26.ps1` to create `app\build\outputs\apk\debug\app-debug.apk` without ADB or phone-data modifications.
