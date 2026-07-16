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

Complete this table after the last code/configuration change. A failed or unrun gate must remain `PENDING` or `FAIL`.

| Gate | Command/check | Final result |
|---|---|---|
| Workspace quality | `pnpm quality` | PENDING H7 FREEZE |
| Native unit suite | from `apps/mobile/android`: `.\gradlew.bat :lm-comment-android:testDebugUnitTest` | PENDING H7 FREEZE |
| Android lint | from `apps/mobile/android`: `.\gradlew.bat :app:lintRelease` | PENDING H7 FREEZE |
| Debug APK | `pnpm mobile:android:debug` | PENDING H7 FREEZE |
| Arm64 release APK | set `LM_COMMENT_ANDROID_ARCH=arm64-v8a`; `pnpm mobile:android:release` | PENDING H7 FREEZE |
| Relay container | build `apps/relay/Dockerfile` and record image digest | PENDING H7 FREEZE |
| Relay health | public `/healthz` plus authenticated synthetic canary | PENDING H7 FREEZE |
| APK inspection | package/name/version, certificate, manifests, provider-key scan, install | PENDING H7 FREEZE |
| Screenshot/log safety | no screenshot files; no request/OCR content in logs | PENDING H7 FREEZE |

## Owner phone gate

No physical phone was connected at this checkpoint. The following are not satisfied by emulator evidence and must remain pending until the owner performs and records them on the priority phone.

| Test | Required | Status |
|---|---:|---|
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

After every physical workflow, verify the debug diagnostics counters return to zero and that no projection/capture notification, duplicate bubble, or screenshot file remains. Record the phone model, Android version, APK hash, dates, failures, and retest outcome without including captured text, tokens, or screenshots of sensitive UI.
