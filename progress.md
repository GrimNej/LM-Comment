# LM-Comment progress

- Checkpoint date: 2026-07-16
- Branch: `rebuild/lm-comment-hackathon`
- Latest completed implementation commit: `8873b0e`
- Latest deployment commit: `c32ca14`
- Current phase: H7 - hardening and rehearsal
- Active implementation blocker: none
- Physical-device acceptance: pending owner phone connection
- Demo rehearsal count: 0 / 5
- Resume from: finish release hardening and connect the owner's phone for physical acceptance

## Phase checklist

- [x] H0 Repository/build - local quality, prebuild, and signed build proven
- [x] H1 Bubble - implementation and compile/emulator smoke gate complete; final phone interaction pending
- [x] H2 One-frame capture - implementation and unit gate complete; final phone MediaProjection acceptance pending
- [x] H3 Crop/OCR - implementation and automated/API 36 offline gate complete; physical portrait/landscape acceptance pending
- [x] H4 Relay/Groq - private native configuration, strict client, live Groq route, and APK secret gate complete
- [x] H5 Results/copy - native generation, editing, regeneration, clipboard, and same-session recapture complete
- [x] H6 Product shell/polish - branded shell, complete routes, safe diagnostics, themes, and accessibility pass complete
- [ ] H7 Hardening/rehearsal - automated/packaging gates in progress; physical-phone matrix and five rehearsals pending owner phone

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
- Private synchronous native demo configuration with release HTTPS validation and token-safe status.
- Strict golden-fixture JSON codec plus bounded, cancellable, no-redirect native HTTPS client.
- Expo allowlists only the public relay URL and temporary demo token into judge builds; existing native settings are preserved.
- Live Kotlin-to-production relay canary returns the exact requested count without printing content.
- Commit: `7ca8213` (`feat(relay): add real Groq generation route`).

### H5 results, editing, regeneration, and copy

- Editable reviewed text, five tones, optional 500-character instruction, and one-to-three result count.
- Cancellable native relay generation with duplicate-request and stale-result guards plus stable error mapping.
- Selectable result cards, explicit editing, regenerate, clipboard copy with haptic feedback, and no auto-posting.
- New Capture safely clears sensitive state and waits for two measured frames before CaptureCloak can be shown again.
- Standalone manual-entry sessions return the user to an available bubble instead of opening an unusable capture path.
- Rotation preserves active generation/results while Close clears in-memory text, bitmaps, OCR, and network work.
- Commit: `f31ceac` (`feat(results): add edit regenerate and copy`).

### H6 product shell and polish

- Branded graphite, violet, and cyan product system with new adaptive icon, monochrome icon, splash, favicon, shared tokens, and reusable controls.
- Polished Home, Setup, Demo, Diagnostics, and Settings routes with light/dark themes and complete navigation.
- Three synthetic judge fixtures enter the real secure native Manual Context, generation, editing, and explicit-copy workflow.
- Safe native diagnostics expose only fixed metadata, an allowlisted stable error code, relay hostname/health, permission state, and debug-only resource counters.
- Settings keep credentials in Android private preferences, protect advanced configuration behind a deliberate unlock, and preserve hidden relay overrides when ordinary writing defaults change.
- Release URL policy rejects local hosts; debug HTTP is allowed only for an explicit local-development allowlist and never becomes a persisted capability.
- Relay health has a four-second overall deadline and disconnects promptly on cancellation.
- Light/dark screens and 200% Android font scale were visually inspected on API 36; controls remain readable, scrollable, and at least 48 dp.
- Commit: `8873b0e` (`feat(shell): add judge-ready product experience`).

## Remaining work in mandatory order

1. H7: finish release-only backend/container hardening and final static/build/package scans.
2. H7 owner gate: install on the priority phone, run portrait/landscape MediaProjection workflows, and complete five consecutive rehearsals.

## Evidence at this checkpoint

- `pnpm quality`: PASS after H6 - scope, naming, expanded native secret scan, lint, typecheck, 18 relay tests, and relay production build.
- Native `:lm-comment-android:testDebugUnitTest`: PASS - 84 tests (83 pass, one opt-in live skip), including H6 URL-policy, settings-preservation, bounded health, diagnostics, and stable-error regressions.
- API 36 x86_64 instrumentation: PASS - private config persistence/safe status plus bundled offline OCR and secure direct-manual behavior (3 tests).
- Arm64 debug APK: PASS - 102,691,686 bytes (97.93 MiB); APK Signature Scheme v2 verified.
- Exact local Groq key and generic `gsk_` pattern scan of every decompressed APK entry: PASS.
- H3 evidence: `artifacts/evidence/h3-crop-ocr-20260715.md`.
- H4 evidence: `artifacts/evidence/h4-native-relay-20260715.md`.
- H5 evidence: `artifacts/evidence/h5-results-workflow-20260716.md`.
- H6 evidence: `artifacts/evidence/h6-product-shell-20260716.md`.
- Relay/VPS evidence: `artifacts/evidence/relay-deployment-20260715.md`.
- Public HTTPS health, invalid-token sanitization, live Groq structured generation, relay/Caddy log scan, and blocked direct port: PASS.
- Physical phone install/capture/crop/OCR evidence: PENDING - no ADB phone connected.
- Fresh H6 x86_64 release APK: PASS - 54,335,309 bytes, SHA-256 `196FD6CCF30A1791241086B3ED8A11CD1BF2A1F61EF932F6BE0F749523469847`, APK Signature Scheme v2 verified, installed on API 36.

## Known limitations

- A Google ML Kit Task cannot be force-cancelled. On coroutine cancellation, bitmap cleanup safely waits for the provider Task to reach a terminal state so pixels cannot be recycled while ML Kit may still read them.
- Physical-phone crop interaction and full MediaProjection-to-OCR repetition remain part of the H7 owner-device gate.
- The workstation currently runs Node 24.14.0 while the repository pins Node 22.13.x; all quality gates pass, and production uses the pinned Node 22.13.1 runtime.

## Inputs needed from the owner

- No more backend credentials, DNS, hosting, or product decisions are needed now.
- When requested for final testing, connect the priority Android phone with USB debugging enabled and provide its model plus Android version.
- Final release signing can use a judge-only generated keystore unless the owner wants to supply a permanent keystore; no decision is needed until H7.
