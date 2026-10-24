# M7.1 — Remote session customization and game feedback

Status: source implemented, **not Android-built** and **not cross-device accepted**.

## Contract

On `POST /api/rooms`, the host may supply:

```json
{
  "nickname": "اسم مستعار",
  "mode": "couple",
  "level": "SOFT",
  "settings": {
    "rounds": 10,
    "variety": false,
    "avoidRepeat": true,
    "customPrompts": []
  }
}
```

The room creator can also send a WebSocket action `{ "type":"configure", "eventId":"...", "settings":{...} }` **while WAITING only**. The server validates `rounds` from `[6,10,12,18]`, Boolean flags, and up to 30 unique Arabic custom prompts (6–200 Unicode code points each; control/bidi override characters rejected). It controls the authoritative turn count and prompt selection. Only the host can configure.

`publicState()` exposes `settings.rounds`, `settings.variety`, `settings.avoidRepeat`, and **a count** of custom prompts. The complete custom deck remains server-side; only individual prompts selected for active rounds are sent in the shared `prompt` field. **This is not end-to-end encryption**: the relay can read custom prompts and participants see them as they appear. All session settings exist in RAM and disappear on server restart.

`variety=true`: challenge choices include the selected intensity plus the immediately gentler level; SOFT has no gentler level. Custom prompts, if provided, are prioritized approximately every other round. `avoidRepeat=true` prevents repeats within a room while candidates remain. It does **not** implement cross-session history or eliminate repeats between rooms.

Default behavior remains 10 couple rounds / 12 party rounds. Clients can therefore continue without supplying `settings`, and the original M7 content version 3 remains unchanged. Older clients will not present M7.1 host controls but still receive the normal round count and prompts.

## Browser and Android UI

- Host can configure while the room is WAITING; other participants see the settings summary (not the custom deck).
- Select number of rounds, intensity mix, room-local non-repeat rule and custom challenge text.
- A synced round progress indicator appears as rounds advance.
- Browser challenge transition uses an opt-in CSS fade/slide respecting `prefers-reduced-motion: reduce`.
- Android challenge transition uses Compose `Crossfade` and optional haptic cues. The toggle enables/disables effects.
- Server remains authoritative; neither client selects the next prompt, starts without consent or changes settings once active.
- Remote text replies, quiz answers and custom prompts remain readable by the relay. This milestone does not complete M5 security.

## Source-only verification

- `node --test backend/test/*.test.mjs` — **25 passed / 0 failed** including five M7.1 tests for host permission, invalid settings, custom rounds, prompt mixing and duplicate commands.
- `python tools/generate_content_v2.py --check` — PASS; canonical content version 3 unchanged.
- `node --check` for browser/backend JS — PASS.
- Independent `kotlinc` pure-core compilation and `ContentSmokeTestKt` / `M7SmokeTestKt` — PASS.
- Android Compose/OkHttp files **not typechecked with Android SDK**, Gradle not invoked, APK not built.
- A browser smoke test was attempted; browser navigation was blocked by the execution environment (`ERR_BLOCKED_BY_ADMINISTRATOR`). **Browser visual behavior remains unverified.**
- No HTTPS/WSS integration with two actual devices, no security audit, and no production deployment.

## Next acceptance criteria

1. Authorize Android compilation later, then fix any Kotlin/Compose compatibility errors.
2. Deploy a private HTTPS/WSS relay and join from Android plus guest browser; verify config, consent, rounds, reconnection and 18-round completion.
3. Test user-device reduced-motion and haptic preferences, small-screen UI, RTL TalkBack labels, and screen rotation.
4. Complete M5 E2EE chat/quiz work before using remote games with genuinely sensitive replies.
