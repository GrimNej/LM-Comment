# LM-Comment progress

- Checkpoint date: 2026-07-15
- Branch: `rebuild/lm-comment-hackathon`
- Checkpoint commit before this ledger: `e33e07c`
- Current phase: H3 — Crop, bundled OCR, and text review (foundation only)
- Active blocker: credentials and physical-device evidence are not available
- Demo rehearsal count: 0 / 5
- Resume from: wire bundled ML Kit OCR and crop/review states into the native workflow

## Phase checklist

- [x] H0 Repository/build — local quality, prebuild, signed debug/release build proven
- [x] H1 Bubble — implementation and compile gate complete; phone behavior pending
- [x] H2 One-frame capture — implementation/unit gate complete; API 36 + phone acceptance pending
- [ ] H3 Crop/OCR — crop editor/geometry foundation and tests complete; ML Kit/workflow wiring pending
- [ ] H4 Relay/Groq — local implementation and 18 tests complete; live canary/deployment pending
- [ ] H5 Results/copy — not implemented
- [ ] H6 Product shell/polish — only the Expo shell exists; full visual product pass pending
- [ ] H7 Hardening/rehearsal — not started

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

### H4 local relay

- `GET /healthz` and authenticated `POST /v1/generate`.
- Fixed model `openai/gpt-oss-120b`; no model failover.
- Strict request/response validation and Groq JSON-schema structured output.
- One format-repair attempt, rate/daily caps, deadlines, kill switch, sanitized logging, stable errors.
- 18 relay tests passing locally.

## Remaining work in order

1. Add bundled `com.google.mlkit:text-recognition:16.0.1`, implement the ML Kit engine, and wire crop → OCR → editable review/manual fallback.
2. Add native relay client and finish review → generating → editable result cards → regenerate/copy/new capture.
3. Persist/validate demo configuration in native private preferences and expose safe diagnostics/counters.
4. Build the polished dark/light Expo routes: Home, Setup, Demo, Diagnostics, Settings.
5. Add relay Docker/deployment packaging, deploy to HTTPS, and run a real Groq canary.
6. Run API 36 emulator and priority-phone capture/OCR/result repetition gates.
7. Produce the final signed judge APK and run five full rehearsals.

## Evidence at this checkpoint

- `pnpm quality`: PASS in 16 seconds (host Node 24.14 emitted the expected warning because the repository pins Node 22.13.x).
- Scope, naming, and secret scans: PASS.
- Lint and TypeScript typecheck: PASS.
- Relay Vitest: PASS — 3 files / 18 tests.
- Relay TypeScript production build: PASS.
- Native `:lm-comment-android:testDebugUnitTest`: PASS after H2/H3 changes.
- H0 arm64 debug APK: PASS.
- H0 signed arm64 release APK: PASS and signature verified.
- Current post-H2/H3-foundation arm64 debug APK: PASS (97,627,629 bytes); APK Signature Scheme v2 verified.
- Physical install/capture evidence: PENDING — no ADB phone was connected.
- API 36 system-image test: PENDING — download was stopped to honor the checkpoint deadline.
- Live Groq canary: PENDING — no `GROQ_API_KEY` or `DEMO_TOKEN` supplied.

## Inputs needed from the owner

- Put `GROQ_API_KEY` and a temporary `DEMO_TOKEN` in a local `.env` copied from `.env.example` (never commit it).
- Choose a public HTTPS relay target (the blueprint recommends Railway) and provide access, or approve a local-only demo.
- Connect the priority Android phone with USB debugging enabled for the H1/H2/H3 acceptance run.
