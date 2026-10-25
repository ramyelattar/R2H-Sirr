# R2H-Sirr — M9 Stabilization Report

**Date:** 2026-10-25  
**Branch:** `feature/m9-stabilization`  
**Base:** M8 `feature/m8-egyptian-experience` at `1208a95d562aabfba16324079b52a1f9b4455322`  
**Scope:** Direct source changes on GitHub; **NO Android build, NO APK installation, NO device run, NO branch merge.**

## Verdict

**SOURCE HANDOFF — READY FOR THE USER'S FIRST LOCAL BUILD, NOT DEVICE-ACCEPTED.** M9 has been updated on GitHub and packaged with a local build/install helper. Per the owner's instruction, GitHub Actions is **not an acceptance gate**; the next gate is a single local Android build and in-place device update. Neither Kotlin/Compose compilation nor device behavior is yet verified, so do not mark production-ready.

## Implemented changes and evidence

| Area | Source evidence | Status | Qualification |
| --- | --- | --- | --- |
| WebSocket encrypted payload limit | `backend/server.mjs` | IMPLEMENTED | Bounded 64 KiB frame (previously 8 KiB); requires integration and realistic network test |
| Success/duplicate acknowledgements | `backend/server.mjs`, `backend/logic.mjs` | IMPLEMENTED | ACK carries `eventId`; bounded FIFO dedup replaces wholesale cache clearing |
| Android remote retry/queue | `RemoteScreen.kt` | IMPLEMENTED | Stable `eventId`, in-memory bounded queue, ACK removal, retry budget and failure indication; not durable across process death |
| Private-message draft preservation | `RemoteScreen.kt` | IMPLEMENTED | Draft is cleared only when successfully queued, not when enqueue fails; ACK is still required to prove server acceptance |
| Local game restore | `LocalSessionStore.kt`, `ExperienceScreen.kt` | IMPLEMENTED | Full deck, actor order and scores serialized into Android-Keystore-backed `LocalVault`, validated on restore |
| Local reply preservation | `ExperienceScreen.kt`, `LocalSessionStore.kt` | IMPLEMENTED | Replies encrypted per round, explicit save and automatic save before completing/skipping; no remote share is claimed |
| Activity navigation restoration | `MainActivity.kt` | IMPLEMENTED | Route uses `rememberSaveable`; remote login/active transfer restoration still needs device acceptance |
| Quiz answer state | `QuizScreen.kt` | IMPLEMENTED | Numeric indices and progress use Compose `rememberSaveable`; these particular small state values live in the Android saved-state Bundle rather than the encrypted vault |
| Dice face consistency | `DiceScreen.kt` | IMPLEMENTED | Freeze result faces at roll, prevent configuration changes during rolling, mark rolling before starting effect |
| Scratch performance | `ScratchScreenV2.kt` | IMPLEMENTED | Observable nested append-only stroke lists remove per-pointer growing list copies; actual frame rate unmeasured |
| Chunk upload resume integrity | `RemoteChunkTransfer.kt` | IMPLEMENTED | SHA-256 fingerprint (including MIME prefix), checks source has not changed before completion, discards stale same-size pending uploads |
| Encrypted chunk download memory | `RemoteChunkTransfer.kt` | IMPLEMENTED | Allocate final plaintext buffer once, copy/decrypt one chunk at a time and clear transient decoded chunks; up to 48 MiB plaintext output still held in RAM |
| New Node regression tests | `backend/test/m9-reliability.test.mjs` | ADDED, NOT RUN | Contains large encrypted WS frame, first/duplicate ACK, and bounded dedup tests |
| Source guard and CI (optional, not required for user handoff) | `tools/check_m8_ui_source.py`, `.github/workflows/m8-source-tests.yml` | UPDATED, NOT RUN | Source check now matches O(1) scratch update and validates M9 presence; CI includes M9 branch |

All Android paths above are relative to `app/src/main/java/com/r2h/sirr/`.

### Additional handoff corrections (2026-10-25)

- `ExperienceScreen.kt`: local round transitions are now saved **synchronously before** exposing the new state; storage failures do not silently advance the round. The end-of-session view displays the previous encrypted replies and allows a new round to delete them.
- `Security.kt`: encrypted private records now use Android `AtomicFile` to avoid truncating existing game data when a write fails, and temporary plaintext byte buffers are zeroed where practical.
- `app/build.gradle.kts`: preview identity is now `versionCode = 4` and `versionName = 0.4.0-m9-preview` (same application ID `com.r2h.sirr`).
- `tools/Build-Install-M9.ps1`: optional Windows PowerShell local helper that builds `:app:assembleDebug` and uses **`adb install -r`**, never uninstall/clear data; stops on Gradle or ADB errors. Requires **Gradle 8.13** available locally because the source repository does not include a Gradle wrapper JAR. No local build or installation was run here.

## Tests and actual observations

- **Gradle / APK / device:** Not run by instruction.
- **Node `npm test`:** Not executed in this environment; newly added test source must be exercised.
- **Python `python tools/check_m8_ui_source.py`:** Not executed in this environment.
- **Content parity `python tools/generate_content_v2.py --check`:** Not executed in this environment.
- **GitHub Actions:** Workflow runs on M9 were created but completed with **`startup_failure`**, with no test jobs executing. Examples: [M9 run 40118069078](https://github.com/ramyelattar/R2H-Sirr/actions/runs/40118069078), [M9 run 40117986930](https://github.com/ramyelattar/R2H-Sirr/actions/runs/40117986930). Root cause not established. **CI is NOT green.**
- The source changes were committed and pushed to the dedicated M9 branch; no claim of passing test or compiler verification.

## Remaining risks / open work

1. **P0 — Compilation:** Build when authorized with AGP/Kotlin configuration, resolve actual errors, and verify lifecycle/saved-state behavior on Android.
2. **Optional — GitHub Actions:** Not part of the owner's acceptance process. Ignore its startup failures for this handoff, without claiming CI success.
3. **P0 — Remote correctness:** Execute Node E2E WS tests, then Android↔Web two-client handshake, configure/ACK/reconnect, member permissions, consent, stale commands, expiry, and encrypted media scenarios.
4. **P1 — Pending commands:** The retry queue is process-memory-only; failed commands may expire without durable delivery. Preserve user intent and use explicit confirmation for irreversible actions.
5. **P1 — Privacy:** Quiz answer indices use system saved-instance-state; decrypted media preview/cache lifecycle and byte-array plaintext retention must be verified on device. Encryption protocol is not independently audited.
6. **P1 — Transfer robustness:** Check changed ContentResolver streams, failure cancellation, room expiry, maximum 48 MiB media pressure, media file integrity and interrupted upload behavior.
7. **P1 — Performance:** Benchmark prolonged scratch gestures, 20-second dice spins, large media reads and slow/rotated screens.
8. **P2 — Product UX:** Independent visual review, accessibility, content editorial quality, navigation and Egyptian colloquial text need real screenshots and user testing. **The previous 65/100 score has not become 90/100 by this source work.**
9. **P2 — Repeatable tooling:** Confirm compatible Gradle wrapper provisioning and dependency locking without introducing binary assets in an unreviewed source-only commit.

## User-operated local build and device-acceptance checklist

- [ ] Node backend tests pass, including `m9-reliability.test.mjs`.
- [ ] Python content and source guards pass.
- [ ] Android debug Kotlin/Compose compilation succeeds.
- [ ] No crashes or data loss on rotation, process recreation, background/foreground and leaving/re-entering each game.
- [ ] Android and browser send large encrypted configuration and process first/deduplicated ACKs.
- [ ] Draft text and private encrypted local replies are handled correctly, including error cases.
- [ ] Resumed same-size mutated file is rejected; download output is byte-for-byte correct.
- [ ] Consent, room expiration, and local privacy wipe are accepted independently.
- [ ] Real device UX and Egyptian-language review is performed; score documented rather than assumed.

**Release status: NOT READY / PARTIAL.** No merge to `main` is recommended before the gates above pass.

## Windows handoff (run when ready to build)

From PowerShell in `E:\Projects\R2H-Android\R2H-Sirr`:

```powershell
git fetch origin feature/m9-stabilization
git switch feature/m9-stabilization
git pull --ff-only origin feature/m9-stabilization
powershell -ExecutionPolicy Bypass -File .\tools\Build-Install-M9.ps1
```

This avoids forcing or resetting branches; if local modifications obstruct checkout or pull, stop and preserve them instead of discarding work. Keep one authorized phone connected via ADB. **Existing installed app data is not deliberately removed**; an unrelated signing key can still make Android refuse an update.

After installing, exercise first-run navigation, couple/party round continuation, encrypted reply save and recap, 25-question quiz with rotation, 20-second dice, 3-layer scratch, remote 2-device encrypted session, big file resume and privacy clear. Share device results to determine whether the next step is a fix or visual polish. The prior subjective 65/100 is not upgraded without new device evidence.
