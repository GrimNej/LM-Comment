# LM-Comment progress

- Checkpoint date: 2026-07-15
- Branch: `rebuild/lm-comment-hackathon`
- Latest deployment commit: `c32ca14`
- Current phase: H3 - Crop, bundled OCR, and text review (foundation only)
- Active blocker: physical-device/API 36 acceptance evidence is not available
- Demo rehearsal count: 0 / 5
- Resume from: wire bundled ML Kit OCR and crop/review states into the native workflow

## Phase checklist

- [x] H0 Repository/build - local quality, prebuild, signed debug/release build proven
- [x] H1 Bubble - implementation and compile gate complete; phone behavior pending
- [x] H2 One-frame capture - implementation/unit gate complete; API 36 + phone acceptance pending
- [ ] H3 Crop/OCR - crop editor/geometry foundation and tests complete; ML Kit/workflow wiring pending
- [ ] H4 Relay/Groq - local and remote relay are green; native relay client remains
- [ ] H5 Results/copy - not implemented
- [ ] H6 Product shell/polish - only the Expo shell exists; full visual product pass pending
- [ ] H7 Hardening/rehearsal - not started

## Completed implementation

### H0

- Expo SDK 57 / React Native workspace, native Expo module, Fastify relay, contracts, scripts, docs.
- Android target/compile SDK 36, min SDK 26, arm64 build helper, clean-prebuild support.
- Model configuration updated from the retiring Llama model to fixed `openai/gpt-oss-120b`.

### H1

- Idempotent special-use foreground bubble service.
- Custom Context Lens bubble, drag, snap, safe bounds, persisted position, reset, notification Open/Stop.
- Session IDs, hide/restore acknowledgement, and 45-minute hard stop.

### H2

- Transparent capture-cloak activity and explicit secure/opaque transition before frame publication.
- Fresh MediaProjection consent per tap, one foreground capture service, one virtual display, one accepted frame.
- Two-million-pixel hard allocation bound, resize handling, terminal error retention, timeouts, blank/protected-frame error.
- Scoped projection/display/reader/image/bitmap cleanup and debug counters.
- Rotation does not recreate the workflow activity (`orientation|screenSize`).

### H3 foundation

- Normalized crop coordinates, pixel mapping, letterbox/orientation transforms, non-crossing handles.
- Memory-only crop bitmap factory and polished native Compose four-handle crop editor.
- OCR interface and conservative line-ending normalization.
- Crop/transform/normalizer unit tests.

### H4 relay and Oracle VPS deployment

- `GET /healthz` and authenticated `POST /v1/generate`.
- Fixed model `openai/gpt-oss-120b`; no model failover.
- Strict validation and Groq JSON-schema structured output.
- One format-repair attempt, rate/daily caps, deadlines, kill switch, sanitized logging, stable errors.
- Public endpoint: `https://lmcomment-api.grimnej.com`.
- Live Groq canary: PASS with a valid structured response.
- App-owned Node 22.13.1 runtime and immutable production-only release under `/opt/lm-comment`.
- Sandboxed DynamicUser service: 192 MiB hard RAM limit, 50% CPU quota, no writable app state.
- Root-only environment at `/etc/lm-comment/relay.env` (`0600`); provider key is absent from source/mobile files.
- Relay listens only on Docker bridge `172.18.0.1:8787`; direct public port 8787 is blocked.
- Existing Caddy received one validated hostname route through live reload; Captionman stayed HTTP 200 and no existing container restarted.
- Deployment build cache removed; complete installed footprint is about 138 MiB.

## Remaining work in order

1. Add bundled `com.google.mlkit:text-recognition:16.0.1`, implement the ML Kit engine, and wire crop -> OCR -> editable review/manual fallback.
2. Add native relay client and finish review -> generating -> editable result cards -> regenerate/copy/new capture.
3. Persist/validate demo configuration in native private preferences and expose safe diagnostics/counters.
4. Build the polished dark/light Expo routes: Home, Setup, Demo, Diagnostics, Settings.
5. Run API 36 emulator and priority-phone capture/OCR/result repetition gates.
6. Produce the final signed judge APK and run five full rehearsals.

## Evidence at this checkpoint

- `pnpm quality`: PASS at the prior checkpoint.
- Scope, naming, secret scans, lint, and TypeScript typecheck: PASS.
- Relay Vitest locally and on Linux: PASS - 3 files / 18 tests.
- Relay TypeScript production build locally and on Linux: PASS.
- Native `:lm-comment-android:testDebugUnitTest`: PASS after H2/H3 changes.
- Current post-H2/H3-foundation arm64 debug APK: PASS (97,627,629 bytes); APK Signature Scheme v2 verified.
- Public HTTPS health: PASS from the VPS and an external workstation.
- Missing/invalid demo token: PASS - sanitized `401 UNAUTHORIZED`.
- Live Groq generation: PASS - HTTP 200, exact one-option schema, content not displayed or persisted.
- Relay/Caddy secret and content log scan: PASS.
- Direct public port 8787: BLOCKED as designed.
- VPS deployment evidence: `artifacts/evidence/relay-deployment-20260715.md`.
- Physical install/capture evidence: PENDING - no ADB phone connected.
- API 36 emulator repetition gate: PENDING.

## Inputs needed from the owner

- No more backend credentials or hosting decisions are needed.
- Connect the priority Android phone with USB debugging enabled for the H1/H2/H3 acceptance run.
- Provide the phone model and Android version before the final device matrix.
