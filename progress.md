# LM-Comment progress

- Checkpoint date: 2026-07-16
- Branch: `rebuild/lm-comment-hackathon`
- Latest completed implementation commit: `1d3c780`
- Latest deployment commit: `d0910d5`
- Current phase: H7 - hardening and rehearsal
- Active implementation blocker: none
- Physical-device acceptance: pending reconnection of the owner's Nothing A001
- Demo rehearsal count: 0 / 5
- Resume from: reconnect the Nothing A001, install the new signed APK, and verify the consent-dialog and visual regressions before the remaining acceptance matrix

## Phase checklist

- [x] H0 Repository/build - local quality, prebuild, and signed build proven
- [x] H1 Bubble - implementation and compile/emulator smoke gate complete; final phone interaction pending
- [x] H2 One-frame capture - implementation and unit gate complete; final phone MediaProjection acceptance pending
- [x] H3 Crop/OCR - implementation and automated/API 36 offline gate complete; physical portrait/landscape acceptance pending
- [x] H4 Relay/Groq - private native configuration, strict client, live Groq route, and APK secret gate complete
- [x] H5 Results/copy - native generation, editing, regeneration, clipboard, and same-session recapture complete
- [x] H6 Product shell/polish - branded shell, complete routes, safe diagnostics, themes, and accessibility pass complete
- [ ] H7 Hardening/rehearsal - automated/packaging gates in progress; physical-phone matrix and five rehearsals pending owner phone

## Visual and capture regression checkpoint

- Commit: `1d3c780` (`fix(android): harden capture and refresh product design`).
- Bubble behavior: all four lens corners now face inward; geometry is unit-tested; safe bounds include system insets; failed overlay removal is never acknowledged as hidden; activity-launch failures clear the workflow session; accessibility activation now triggers the same click path as touch.
- Capture behavior: MediaProjection waits for focus/resume recovery and two committed CaptureCloak frames, then discards one bounded raw compositor buffer before converting the accepted frame. Consent result data remains memory-only and is cleared on every terminal path.
- Product design: the Expo shell and native workflow now share paper, ink, signal-lime, and terracotta tokens; the crop editor, bubble, launcher icon, splash, monochrome icon, and native header use the same four-corner lens mark. Purple/cyan gradients, glass effects, oversized pills, stacked promotional labels, all-caps product copy, and em dashes were removed.
- Repository presentation: `README.md` now includes the generated banner, app icon, badges, architecture, privacy boundaries, build instructions, evidence links, and MIT license link. The generated banner is `docs/assets/lm-comment-banner.png`.
- Copy enforcement: `scripts/check-copy-style.mjs` is part of `pnpm check` and rejects em dashes across tracked product/source documentation plus canned promotional phrases and quoted all-caps labels in product copy. Rules and research are recorded in `docs/COPY_STYLE.md`.
- Files changed: `README.md`; `CHECKPOINT_HANDOFF.md`; the implementation blueprint punctuation; `apps/mobile/app.config.ts`; `apps/mobile/assets/brand/*`; `apps/mobile/assets/images/*`; all six Expo route files; `apps/mobile/src/ui/{tokens,components}.tsx`; demo fixtures/scripts/checklist; `docs/{COPY_STYLE,DEMO_RUNBOOK}.md`; `docs/assets/lm-comment-banner.png`; native bubble, capture, crop, and workflow Kotlin sources; four new native unit-test files; `package.json`; and `scripts/{check-copy-style,generate-brand-assets}.mjs`.
- Exact automated tests: `pnpm quality` passed; clean `pnpm mobile:prebuild` passed; 16 native suites passed with 98 tests, 0 failures, 0 errors, and 1 intentional live-test skip; API 36 instrumentation passed 3 / 3; scoped release lint passed; x86_64 release clean-installed and launched; Home, Samples, and the secure native manual workflow passed UI-hierarchy smoke checks without taking screenshots.
- Final artifact: `artifacts/release/LM-Comment-0.1.0-hackathon-arm64.apk`, 53,163,818 bytes, SHA-256 `3BC5DBCC9409460167AA85E0622E2A1E673E4C66EC4FCB418DCA3DA81B95018D`, arm64-v8a only, v2/v3 signed with certificate SHA-256 `9570D71820DFCA41BA25C8717CEACF2B77A3C867227056F992DD6CB12E080731`, and 16 KiB aligned.
- APK evidence: package/name/version/SDK/ABI, service separation, `noHistory=false`, manifest restrictions, and provider-boundary scans passed. The APK contains zero provider-secret hits, zero `.env` entries, and zero screenshot-like image entries.
- Evidence paths: `docs/TEST_EVIDENCE.md` and ignored local report `artifacts/evidence/h7-visual-capture-regression-20260716.md`.
- Next phase: H7 physical verification on the Nothing A001, followed by the remaining stress matrix and five rehearsals.
- Known limitation: automated checks cannot prove what the owner sees in the MediaProjection crop. Physical confirmation that the consent dialog is absent, the bubble artwork is correct, and the redesigned screens look right remains pending because the phone is not currently connected.

## Completed implementation

### H0

- Expo SDK 57 / React Native workspace, native Expo module, Fastify relay, contracts, scripts, and docs.
- Android target/compile SDK 36, min SDK 26, arm64 build helper, clean-prebuild support.
- Fixed Groq model updated from the retiring Llama model to `openai/gpt-oss-120b`.

### H1

- Idempotent `specialUse` foreground bubble service.
- Custom Context Lens bubble with four tested inward-facing corner marks, drag, snap, inset-safe bounds, persisted position, reset, notification Open/Stop, and accessible click activation.
- Session IDs, synchronous hide/restore acknowledgement, and 45-minute hard stop.
- Bubble-owned direct-manual session prevents overlapping capture/manual sessions and restores exactly once.
- Capture proceeds only after a verified overlay removal; failed activity launches clear the active session and restore or stop the bubble cleanly.

### H2

- Transparent capture-cloak activity and explicit secure/opaque transition before frame publication.
- Fresh MediaProjection consent per tap, dedicated one-shot foreground capture service, one virtual display, one accepted full-display frame.
- Post-consent capture waits for resumed focus plus two committed transparent-cloak frames and skips one raw compositor buffer, preventing the Android consent surface from becoming the accepted crop frame.
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

- Editorial paper, ink, signal-lime, and terracotta product system with a unified four-corner lens across the bubble, native workflow, adaptive icon, monochrome icon, splash, favicon, shared tokens, and reusable controls.
- Polished Home, Setup, Demo, Diagnostics, and Settings routes with light/dark themes and complete navigation.
- Three synthetic judge fixtures enter the real secure native Manual Context, generation, editing, and explicit-copy workflow.
- Safe native diagnostics expose only fixed metadata, an allowlisted stable error code, relay hostname/health, permission state, and debug-only resource counters.
- Settings keep credentials in Android private preferences, protect advanced configuration behind a deliberate unlock, and preserve hidden relay overrides when ordinary writing defaults change.
- Release URL policy rejects local hosts; debug HTTP is allowed only for an explicit local-development allowlist and never becomes a persisted capability.
- Relay health has a four-second overall deadline and disconnects promptly on cancellation.
- Light/dark screens and 200% Android font scale were visually inspected on API 36; controls remain readable, scrollable, and at least 48 dp.
- Professional root README with generated banner, logo, badges, architecture, privacy boundaries, build instructions, evidence links, and license.
- Sentence-case, action-first product copy plus a repeatable repository copy-style check; no em dashes remain in tracked source or documentation.
- Commits: `8873b0e` and redesign/fix commit `1d3c780`.

### H7 release hardening and package freeze

- Relay provider work is bounded to 20 seconds and the Fastify request deadline is 25 seconds so stable timeout responses win before infrastructure timeouts.
- Multi-stage, production-only Docker image pins Node 22.13.1 and pnpm 10.34.5, runs as the unprivileged `node` user, and includes a health check.
- Clean Expo prebuild preserves API compatibility through the durable safe-splash config plugin; its generated Android result was verified.
- Expanded scope, forbidden-route, AccessibilityService, direct-provider, persistence, database, streaming, and APK secret checks protect the release contract.
- Thirty synthetic quality scenarios cover all five tones; the structural validator passes. Human model-output scoring remains pending and is not claimed.
- Judge fixtures, presentation script, rehearsal checklist, demo runbook, test-evidence record, and post-hackathon roadmap are present.
- Content-free live canary verifies response structure and count without printing generated content.
- Oracle deployment now points atomically to immutable release `d0910d5`; its installed release is about 23 MiB, the host retains about 20 GB free, and unrelated services were left untouched.
- The refreshed phone distribution is frozen as a 53,163,818-byte arm64-only APK with a dedicated 3,072-bit hackathon release certificate, verified v2/v3 signatures, 16 KiB alignment, and SHA-256 `3BC5DBCC9409460167AA85E0622E2A1E673E4C66EC4FCB418DCA3DA81B95018D`.
- Final APK inspection confirms the required package/name/version, separate bubble and capture services, `noHistory=false`, no debuggable/cleartext/Accessibility/dev-launcher manifest surface, no screenshot-like image entries, and no provider key or direct Groq endpoint.
- H7 files changed: relay/container (`.dockerignore`, `apps/relay/Dockerfile`, `apps/relay/src/app.ts`, relay tests and canary); mobile/native configuration (`apps/mobile/app.config.ts`, safe-splash plugin, Settings, demo-configuration validator and tests); release gates/data (`package.json`, scope and quality validators, quality set/evaluation); and judge documentation (`apps/mobile/README.md`, demo fixtures/script/checklist, `docs/DEMO_RUNBOOK.md`, `docs/POST_HACKATHON_ROADMAP.md`, `docs/TEST_EVIDENCE.md`, and this checkpoint).
- Commits: `291914e` (`chore(release): harden hackathon delivery`), `e7eb66a` (`docs(demo): add judge runbook and rehearsal kit`), and `d0910d5` (`chore(relay): keep live canary content-free`).

## Remaining work in mandatory order

1. H7 owner gate: install the frozen APK on the priority phone and run the complete physical-device matrix, including 20 capture-to-copy workflows and cancellation/recovery paths.
2. H7 rehearsal gate: complete five consecutive judge rehearsals.
3. H7 owner controls: confirm the Groq dashboard spending limit and record/review the external fallback video.

## Evidence at this checkpoint

- `pnpm quality`: PASS after H7 source hardening - scope, naming, secrets, 30-case quality-set validation, lint, typecheck, 19 relay tests, and relay production build.
- Native `:lm-comment-android:testDebugUnitTest`: PASS - 98 tests across 16 suites, zero failures, zero errors, and one intentional opt-in live skip.
- Clean `pnpm mobile:prebuild`: PASS; the durable Expo config plugin removes the incompatible API-33-only splash attribute from generated base styles.
- API 36 x86_64 instrumentation: PASS - 3 / 3 tests covering private configuration, bundled offline OCR, and secure direct-manual behavior.
- H7 x86_64 debug APK build: PASS.
- H7 x86_64 release APK clean install and launch on the API 36 emulator: PASS.
- Final refreshed arm64 release build: PASS - 53,163,818 bytes, SHA-256 `3BC5DBCC9409460167AA85E0622E2A1E673E4C66EC4FCB418DCA3DA81B95018D`, `arm64-v8a` only.
- Dedicated release signing: PASS - APK Signature Schemes v2/v3, 3,072-bit RSA certificate SHA-256 `9570D71820DFCA41BA25C8717CEACF2B77A3C867227056F992DD6CB12E080731`, and 16 KiB zip alignment verified.
- Final APK inspection: PASS - package/name/version/SDK/launcher, service separation, `noHistory=false`, forbidden manifest surfaces, screenshot-like entry names, exact Groq key, generic `gsk_`, `GROQ_API_KEY`, and direct Groq endpoint checks.
- Release lint: PASS for the app and first-party native module with `react-native-worklets` and `react-native-reanimated` release analyzers explicitly excluded after those upstream analyzer tasks crashed internally; no first-party lint finding is hidden by this exception.
- Final relay container: PASS - digest `sha256:bffd7ac6e0762a19fe3ac65b2439a7d745e3dd3bebfb5f128fb66830f82201c1`, 57,563,877 bytes, non-root UID 1000, Node 22.13.1, and healthy ephemeral run.
- Oracle VPS release `d0910d5`: PASS - service active, public HTTPS health good, authenticated content-free Groq canary good, about 20 GB free, and final source/log secret scans clean.
- Arm64 debug APK: PASS - 102,691,686 bytes (97.93 MiB); APK Signature Scheme v2 verified.
- Exact local Groq key and generic `gsk_` pattern scan of every decompressed APK entry: PASS.
- H3 evidence: `artifacts/evidence/h3-crop-ocr-20260715.md`.
- H4 evidence: `artifacts/evidence/h4-native-relay-20260715.md`.
- H5 evidence: `artifacts/evidence/h5-results-workflow-20260716.md`.
- H6 evidence: `artifacts/evidence/h6-product-shell-20260716.md`.
- H7 evidence: `artifacts/evidence/h7-release-checkpoint-20260716.md`.
- H7 tracked automated-release evidence: `docs/TEST_EVIDENCE.md`.
- Relay/VPS evidence: `artifacts/evidence/relay-deployment-20260715.md`.
- Public HTTPS health, invalid-token sanitization, live Groq structured generation, relay/Caddy log scan, and blocked direct port: PASS.
- Physical phone install/capture/crop/OCR evidence: PENDING - the Nothing A001 is not currently visible to ADB.
- Fresh H6 x86_64 release APK: PASS - 54,335,309 bytes, SHA-256 `196FD6CCF30A1791241086B3ED8A11CD1BF2A1F61EF932F6BE0F749523469847`, APK Signature Scheme v2 verified, installed on API 36.
- Final refreshed arm64 release APK, signature/hash, manifest/ABI and provider-boundary inspection: PASS. Clean arm64 install remains PENDING on the owner's phone because the available emulator is x86_64.
- H7 owner-phone stress matrix, five rehearsals, external fallback video, and Groq dashboard spending-limit confirmation: PENDING.

## Known limitations

- A Google ML Kit Task cannot be force-cancelled. On coroutine cancellation, bitmap cleanup safely waits for the provider Task to reach a terminal state so pixels cannot be recycled while ML Kit may still read them.
- Physical-phone crop interaction and full MediaProjection-to-OCR repetition remain part of the H7 owner-device gate.
- Android release lint currently requires excluding the crashing upstream `react-native-worklets` and `react-native-reanimated` analyzers; the app and first-party module lint successfully.
- Structural validation covers all 30 quality scenarios, but human review of live model output is still pending.
- The workstation currently runs Node 24.14.0 while the repository pins Node 22.13.x; all recorded workspace quality gates pass, and production uses the pinned Node 22.13.1 runtime.

## Inputs needed from the owner

- No more backend credentials, DNS, hosting, or product decisions are needed now.
- For final testing, connect the priority Android phone with USB debugging enabled and provide its model plus Android version.
- Confirm that a spending limit is set in the Groq dashboard; do not share the provider key again.
- Record the fallback demo externally because secure workflow screens may be black in built-in screen recording, then run and record five consecutive judge rehearsals.
- A judge-only generated signing keystore is sufficient for this hackathon build unless the owner explicitly wants to preserve a permanent Play Store identity.
