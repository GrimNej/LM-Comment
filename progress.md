# LM-Comment progress

- Checkpoint date: 2026-07-15
- Branch: `rebuild/lm-comment-hackathon`
- Latest completed implementation commit: `d06e61c`
- Latest deployment commit: `c32ca14`
- Current phase: H4 - native demo configuration and relay client
- Active implementation blocker: none
- Physical-device acceptance: pending owner phone connection
- Demo rehearsal count: 0 / 5
- Resume from: implement the strict native relay contract and private demo configuration store

## Phase checklist

- [x] H0 Repository/build - local quality, prebuild, and signed build proven
- [x] H1 Bubble - implementation and compile/emulator smoke gate complete; final phone interaction pending
- [x] H2 One-frame capture - implementation and unit gate complete; final phone MediaProjection acceptance pending
- [x] H3 Crop/OCR - implementation and automated/API 36 offline gate complete; physical portrait/landscape acceptance pending
- [ ] H4 Relay/Groq - local and remote relay are green; native client/configuration remains
- [ ] H5 Results/copy - not implemented
- [ ] H6 Product shell/polish - foundation exists; full product pass pending
- [ ] H7 Hardening/rehearsal - not started

## Completed implementation

### H0

- Expo SDK 57 / React Native workspace, native Expo module, Fastify relay, contracts, scripts, and docs.
- Android target/compile SDK 36, min SDK 26, arm64 build helper, clean-prebuild support.
- Fixed Groq model updated from the retiring Llama model to `openai/gpt-oss-120b`.

### H1

- Idempotent `specialUse` foreground bubble service.
- Custom Context Lens bubble, drag, snap, safe bounds, persisted position, reset, notification Open/Stop.
- Session IDs, synchronous hide/restore acknowledgement, and 45-minute hard stop.
- Bubble-owned direct-manual session prevents overlapping capture/manual sessions and restores exactly once.

### H2

- Transparent capture-cloak activity and explicit secure/opaque transition before frame publication.
- Fresh MediaProjection consent per tap, dedicated one-shot foreground capture service, one virtual display, one accepted full-display frame.
- Two-million-pixel hard allocation bound, resize handling, stable terminal errors, timeouts, and blank/protected-frame detection.
- Scoped projection/display/reader/image/bitmap cleanup and debug counters.
- Rotation does not recreate the workflow activity (`orientation|screenSize`); process restoration closes safely instead of exposing or hanging a stale workflow.

### H3

- Normalized crop coordinates, pixel mapping, letterbox/orientation transforms, non-crossing handles, Reset and Full screen.
- Polished adaptive native Compose crop/review/manual UI with 48 dp targets, landscape scrolling, IME handling, and accessible crop alternatives.
- Bundled `com.google.mlkit:text-recognition:16.0.1`; OCR works from memory without first-run model delivery.
- Editable review plus exact empty OCR state, Back to crop, Use full screen, Type text, retry, and direct manual entry.
- Caller-owned bitmap contract, cancellation-safe ML Kit Task borrowing, identity-tracked crop cleanup, and opaque Closing state before teardown.
- Ten repeated crop/OCR lifecycle cycles release every temporary crop and return `activeWorkflowBitmap` to zero.
- Commit: `d06e61c` (`feat(workflow): add crop and bundled OCR`).

### H4 relay and Oracle VPS deployment

- `GET /healthz` and authenticated `POST /v1/generate`.
- Fixed model `openai/gpt-oss-120b`; no model failover.
- Strict validation and Groq JSON-schema structured output.
- One format-repair attempt, rate/daily caps, deadlines, kill switch, sanitized logging, stable errors.
- Public endpoint: `https://lmcomment-api.grimnej.com`.
- Live Groq canary: PASS with a valid structured response.
- App-owned Node 22.13.1 runtime and immutable production-only release under `/opt/lm-comment`.
- Sandboxed DynamicUser service: 192 MiB hard RAM limit, 50% CPU quota, and no writable app state.
- Root-only environment at `/etc/lm-comment/relay.env` (`0600`); provider key is absent from source/mobile files.
- Relay listens only on Docker bridge `172.18.0.1:8787`; direct public port 8787 is blocked.
- Existing Caddy received one validated hostname route through live reload; Captionman stayed HTTP 200 and no existing container restarted.
- Deployment build cache removed; complete installed footprint is about 138 MiB.

## Remaining work in mandatory order

1. H4: add native private demo configuration validation/store, strict JSON codec, bounded cancellable relay client, fixture tests, and live mobile call.
2. H5: finish review -> generating -> editable result cards -> regenerate/copy/new capture, with duplicate/cancellation guards and no auto-post.
3. H6: finish the polished Expo Home, Setup, Demo, Diagnostics, and Settings routes in dark/light themes.
4. H7: run final secret/security/static gates, API 36 full-flow repetition, priority-phone portrait/landscape workflows, release signing, and five rehearsals.

## Evidence at this checkpoint

- `pnpm quality`: PASS after H3 - scope, naming, secret scan, lint, typecheck, 18 relay tests, and relay production build.
- Native `:lm-comment-android:testDebugUnitTest`: PASS - 30 tests.
- API 36 x86_64 instrumentation with airplane mode: PASS - bundled offline OCR and secure direct-manual/Back behavior (2 tests).
- Arm64 debug APK: PASS - 100,621,268 bytes (95.96 MiB); APK Signature Scheme v2 verified.
- Exact local Groq key and generic `gsk_` pattern scan of every decompressed APK entry: PASS.
- H3 evidence: `artifacts/evidence/h3-crop-ocr-20260715.md`.
- Relay/VPS evidence: `artifacts/evidence/relay-deployment-20260715.md`.
- Public HTTPS health, invalid-token sanitization, live Groq structured generation, relay/Caddy log scan, and blocked direct port: PASS.
- Physical phone install/capture/crop/OCR evidence: PENDING - no ADB phone connected.

## Known limitations

- A Google ML Kit Task cannot be force-cancelled. On coroutine cancellation, bitmap cleanup safely waits for the provider Task to reach a terminal state so pixels cannot be recycled while ML Kit may still read them.
- Physical-phone crop interaction and full MediaProjection-to-OCR repetition remain part of the H7 owner-device gate.
- The workstation currently runs Node 24.14.0 while the repository pins Node 22.13.x; all quality gates pass, and production uses the pinned Node 22.13.1 runtime.

## Inputs needed from the owner

- No more backend credentials, DNS, hosting, or product decisions are needed now.
- When requested for final testing, connect the priority Android phone with USB debugging enabled and provide its model plus Android version.
- Final release signing can use a judge-only generated keystore unless the owner wants to supply a permanent keystore; no decision is needed until H7.
