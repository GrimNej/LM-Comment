# Test evidence

Status date: 2026-07-16. This file separates recorded evidence from owner-only acceptance work. [`progress.md`](../progress.md) is the checkpoint authority; raw local reports live under `artifacts/evidence/` and are intentionally ignored by Git because they can contain device metadata.

## Recorded H6 baseline

These are the latest completed-phase results recorded before H7 packaging changes. Run the final H7 commands again after the release tree is frozen.

| Gate | Recorded result | Evidence |
|---|---|---|
| `pnpm quality` | PASS: scope, naming, secret scan, lint, typecheck, 18 relay tests, relay build | `progress.md` |
| Native debug unit tests | PASS: 84 total; 83 passed and one opt-in live test skipped | `progress.md` |
| API 36 x86_64 instrumentation | PASS: three tests covering private config, bundled offline OCR, and secure manual workflow | `progress.md` |
| API 36 release smoke | PASS: installed, branded shell inspected, real fixture generated, result edited and copied | `artifacts/evidence/h6-product-shell-20260716.md` |
| Release APK signature | PASS: APK Signature Scheme v2 verified for the recorded H6 APK | `progress.md` |
| APK provider-key scan | PASS: exact local key and generic `gsk_` scan of decompressed entries | `progress.md` |
| Public relay | PASS: HTTPS health, invalid-token sanitization, structured Groq generation, and log scan | `artifacts/evidence/relay-deployment-20260715.md` |

Recorded H6 x86_64 release APK: 54,335,309 bytes, SHA-256 `196FD6CCF30A1791241086B3ED8A11CD1BF2A1F61EF932F6BE0F749523469847`. This is baseline evidence, not the final H7 distribution hash.

Supporting reports:

- `artifacts/evidence/h3-crop-ocr-20260715.md`
- `artifacts/evidence/h4-native-relay-20260715.md`
- `artifacts/evidence/h5-results-workflow-20260716.md`
- `artifacts/evidence/h6-product-shell-20260716.md`
- `artifacts/evidence/relay-deployment-20260715.md`

## Final automated release record

The H7 source and relay hardening gates below were rerun after visual/capture regression commit `1d3c780`. The relay deployment remains at `d0910d5` because no relay code changed. The refreshed arm64 distribution artifact is frozen and inspected. A failed or unrun gate remains `PENDING` or `FAIL`.

| Gate | Command/check | Final result |
|---|---|---|
| Workspace quality | `pnpm quality` | PASS: scope, naming, repository copy style, secret and 30-case quality-set validators; lint; typecheck; 19 relay tests; relay build |
| Native unit suite | from `apps/mobile/android`: `.\gradlew.bat :lm-comment-android:testDebugUnitTest` | PASS: 98 total across 16 suites, zero failures, zero errors, one intentional opt-in live-test skip |
| Clean native generation | `pnpm mobile:prebuild` | PASS: clean prebuild; API-33-only splash attribute removed by the durable Expo config plugin |
| API 36 instrumentation | x86_64 connected Android tests | PASS: 3 / 3 |
| Android lint | from `apps/mobile/android`: `.\gradlew.bat :app:lintRelease -PreactNativeArchitectures=x86_64 -x :react-native-worklets:lintAnalyzeRelease -x :react-native-reanimated:lintAnalyzeRelease` | PASS: app and LM-Comment module release lint; only the two named upstream analyzers were excluded after they crashed internally |
| Debug APK | x86_64 `pnpm mobile:android:debug` | PASS |
| Arm64 release APK | set `LM_COMMENT_ANDROID_ARCH=arm64-v8a`; `pnpm mobile:android:release` | PASS: 53,163,818-byte signed artifact; SHA-256 `3BC5DBCC9409460167AA85E0622E2A1E673E4C66EC4FCB418DCA3DA81B95018D` |
| Relay container | build `apps/relay/Dockerfile` and record image digest | PASS: `sha256:bffd7ac6e0762a19fe3ac65b2439a7d745e3dd3bebfb5f128fb66830f82201c1`; 57,563,877 bytes; Node 22.13.1; non-root UID 1000; healthy ephemeral run |
| Relay health | public `/healthz` plus authenticated synthetic canary | PASS: public HTTPS health and content-free authenticated Groq canary; VPS `current` points to `d0910d5` and the service is active |
| VPS isolation and safety | current release, free storage, source/log secret scans | PASS: immutable 23 MiB release, approximately 20 GB free, unrelated services preserved, final source/log scans clean |
| APK inspection | package/name/version, certificate, manifests, provider-key scan, install | PARTIAL: package `com.grimnej.lmcomment`, name `LM-Comment`, version `0.1.0`, arm64-only ABI, v2/v3 dedicated release signature, 16 KiB alignment, manifest and every-entry provider scan pass; final arm64 install requires the owner phone |
| Screenshot/log safety | no screenshot persistence; no request/OCR content in logs | PARTIAL: source policy, APK entry scan, and deployed relay log scans pass; owner-phone post-workflow residue check remains pending |
| Runtime route smoke | API 36 x86_64 release install plus UI hierarchy | PASS: Home, Samples, and secure native manual workflow launched with the new copy and controls; no screenshot was taken or stored |

The lint result is intentionally scoped precisely: the application and first-party native module passed. `react-native-worklets` and `react-native-reanimated` release analyzers were excluded because their upstream Android lint tasks crashed (`react-native-reanimated` reported a missing `KaModule`), not because of an application lint finding.

Final artifact details:

- Path: `artifacts/release/LM-Comment-0.1.0-hackathon-arm64.apk` (local release artifact; intentionally ignored by Git).
- Certificate: `CN=LM-Comment Hackathon Release, OU=Hackathon, O=Grimnej, C=NP`; SHA-256 `9570D71820DFCA41BA25C8717CEACF2B77A3C867227056F992DD6CB12E080731`.
- APK Signature Schemes v2 and v3 verify; `zipalign -c -P 16 -v 4` passes.
- The exact local Groq key, generic `gsk_`, `GROQ_API_KEY`, and `api.groq.com` each have zero matches across every decompressed APK entry.
- The APK contains zero `.env` entries and zero screenshot-like image entries.
- The x86_64 release variant clean-installed and launched on the API 36 emulator. Home, Samples, and the secure native manual workflow passed content-only UI-hierarchy smoke checks. The frozen arm64 artifact cannot run on that x86_64 emulator and therefore remains pending clean installation on the owner's arm64 phone.

## Owner phone gate

The owner's Nothing A001 was not visible to ADB at this checkpoint. The following are not satisfied by emulator evidence and must remain pending until the owner performs and records them on that phone.

| Test | Required | Status |
|---|---:|---|
| Consent dialog absent from accepted crop | 1 | PENDING OWNER PHONE |
| Four bubble corners face inward | 1 | PENDING OWNER PHONE |
| Redesigned shell and native workflow visual review | 1 | PENDING OWNER PHONE |
| Complete capture-to-copy workflows | 20 | PENDING OWNER PHONE |
| Capture-consent cancellations | 20 | PENDING OWNER PHONE |
| Bubble start/stop | 10 | PENDING OWNER PHONE |
| Rotations | 5 | PENDING OWNER PHONE |
| Background/foreground cycles | 5 | PENDING OWNER PHONE |
| Screen lock/unlock cycles | 3 | PENDING OWNER PHONE |
| Permission revocation and force-stop recovery | 1 each | PENDING OWNER PHONE |
| Offline/token/backend/OCR/long-text/blank-frame failures | matrix | PENDING OWNER PHONE |
| Five consecutive judge rehearsals | 5 | 0 / 5 |
| Offline fallback video reviewed | 1 | PENDING OWNER RECORDING |

Additional owner-controlled release checks:

- Groq dashboard spending limit: **PENDING OWNER CONFIRMATION**.
- Human review of all 30 quality-set scenarios: **PENDING**; the structural validator passes, but model-output quality has not been claimed.

After every physical workflow, verify the debug diagnostics counters return to zero and that no projection/capture notification, duplicate bubble, or screenshot file remains. Record the phone model, Android version, APK hash, dates, failures, and retest outcome without including captured text, tokens, or screenshots of sensitive UI.
