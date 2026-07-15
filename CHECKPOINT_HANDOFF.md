# LM-Comment checkpoint handoff — 2026-07-15

This is a deliberately clean stop point created under the owner's 30-minute cutoff.

## Git checkpoint

```text
c732127 chore(repo): establish lm-comment hackathon workspace
da518c6 feat(bubble): add draggable overlay vertical slice
63c76a7 feat(capture): add secure one-shot projection workflow
b377d76 feat(relay): add fixed-model Groq generation route
e33e07c feat(workflow): add tested crop editor foundation
```

The authoritative task ledger is [`progress.md`](progress.md).

## Resume command

```powershell
pnpm install --frozen-lockfile
pnpm quality
```

Then resume H3 at the bundled OCR engine and workflow integration. Do not mark H2 or H3 accepted until the emulator/phone repetition gates in the blueprint are recorded.

The latest checkpoint APK is generated at:

```text
apps/mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

It builds and verifies, but it is a developer checkpoint—not the judge APK. Crop/OCR/results are not wired yet.

## Credential setup

Copy `.env.example` to `.env` and set:

```dotenv
GROQ_API_KEY=...
DEMO_TOKEN=...
```

Keep `.env` local. The mobile app must never contain the Groq provider key; only the short-lived demo token may be configured into the judge build.

## Fast verification

```powershell
pnpm check
pnpm lint
pnpm typecheck
pnpm test
pnpm relay:build
$env:LM_COMMENT_ANDROID_ARCH='arm64-v8a'
pnpm mobile:android:debug
```
