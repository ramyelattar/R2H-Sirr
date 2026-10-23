# Codex / Next implementation phase (NO APK BUILD)

You are extending the existing M4 Android + Node project. Preserve all functional work from M3 and M4; do not replace architecture or rewrite UI. Do not clone InteractiveWish assets/content or call its private APIs. Content must be original Arabic, for consensual adults 18+ only. Do not use cloud AI.

## Owner directive

**NO Android build now.** Do not invoke Gradle, `assembleDebug`, `bundle`, emulator, ADB installation, or package an APK until the owner explicitly authorizes it. Source editing, Node unit tests, content generation checks and isolated Kotlin-core compilation are allowed for verification.

## Immediate tasks

1. Audit `RemoteScreen.kt`, `RemoteCrypto.kt`, `RemoteSessionStorage.kt`, and `backend/{logic,media,server}.mjs` for Kotlin/Compose compatibility and security logic without building. Do not claim UI tested.
2. Implement independently scoped source-only improvements: recoverable encrypted uploads/downloads (chunking, quotas, cancellation); encrypted remote end-card photo if approved by privacy constraints; clean server-side revocation paths. Avoid introducing remote storage of decrypted content.
3. Validate browser's canvas scratch reveal with a real browser only when UI capture is available. Do not claim this test passed from syntax check alone.
4. Preserve the canonical v2 content files and generator. Run `python tools/generate_content_v2.py --check` after changes; do not run legacy M3 generator output.
5. Add backend unit tests for replays, simultaneous first downloads, expiry cleanup, and reconnection logic; document evidence and remaining risks.
6. Do not mark the stage COMPLETE/RELEASE until a later owner-authorized device acceptance run.

## Commands allowed in this no-build phase

```bash
python tools/generate_content_v2.py --check
(cd backend && node --test test/*.test.mjs)
node --check backend/public/app.js
# Standalone Kotlin-core compilation for pure logic is allowed if needed; no Gradle.
```

## Delivery

Report changed files, exact source-level outcomes, test results, unverified Android/real-device behavior, and the next development checkpoint. Do not change licensing policies or proprietary third-party materials.
