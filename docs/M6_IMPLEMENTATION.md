# M6 — Encrypted media transport and shared card attachments (source-only)

Branch: feature/m6-media-cards, based on M4 main. No Android build or APK was produced.

## Added

- Backward-compatible encrypted transfers up to 48 MiB including MIME prefix; old single-request route retained for files under 8 MiB.
- 512 KiB AES-256-GCM chunks, independent random 12-byte IVs and AAD binding each chunk to room ID, upload ID, index and media kind. Relay stores ciphertext/metadata, not media plaintext.
- Upload creation, status query for received chunks, idempotent retry, cancellation and finalization.
- Browser resume while the selected File is available in the open page; Android resume while the transport instance and source URI survive. Not restart-persistent.
- Limits: max 48 MiB/file, 64 MiB per room, 128 MiB globally, 4 active uploads per room, 40 globally. RAM storage with 15-minute TTL (or earlier room expiry).
- Encrypted version-2 end-cards with optional image and audio references for consenting pair sessions. Attached files remain separately available until expiry; first-download removal applies only to the encrypted card, not to its attachments.
- Arabic controls for upload/resume/cancel, progress and card attachment viewing in Android and browser.

## API additions

POST /api/rooms/:room/uploads
GET /api/rooms/:room/uploads/:upload
PUT /api/rooms/:room/uploads/:upload/chunks/:index
DELETE /api/rooms/:room/uploads/:upload
POST /api/rooms/:room/uploads/:upload/complete
GET /api/rooms/:room/media/:media/manifest
GET /api/rooms/:room/media/:media/chunks/:index

All require bearer room authorization. Creation requires the active actor's turn; status/update/cancel require ownership of the upload. Completed chunk media appear in room state with transfer: chunked.

## Verification (local source, not Android)

- Node tests: 18/18 passing (permissions, deduplication, partial transfers, AES-GCM roundtrip, quotas, expiry).
- Node syntax checks: backend server, chunk store, browser guest and transfer library.
- Python content consistency check: 170 challenges and 140 questions.
- GitHub key source files compared with local blob hashes.

## Unverified / remaining

- Android Compose/OkHttp source is not built, type-checked against Android dependencies, or device-tested. No APK, emulator or Gradle run.
- No live HTTP/WSS integration or two-device test. The environment did not have the ws npm dependency; no hosted server was deployed.
- Room and uploads remain volatile in server RAM; server restart discards them.
- Browser restart loses selected File; Android process death may invalidate URI/transfer state.
- Android large media downloads still assemble the full payload in memory and require file-streamed preview optimization before release.
- Interoperability between Android AES-GCM and WebCrypto remains unverified in a real device session.
- Server still sees normal text messages and quiz selections. The preceding M5 privacy work is NOT completed.
- Card first-download is not proof of viewing. Attachment files themselves are not single-use and expire separately.
- More advanced animations, accessibility and polished scratch effects remain.
- M6 must stay a draft development branch; no release-ready claim.

No build should be performed until the owner explicitly authorizes it.
