# LM-Comment

LM-Comment is a screen-aware Android writing assistant built for a focused
hackathon demonstration. A user starts a floating bubble, approves one screen
capture, crops the relevant words, reviews on-device OCR, chooses a tone, and
copies an editable response generated through a small Groq-backed relay.

## What is deliberately different

- The screenshot stays in Android memory and is never uploaded.
- Bundled ML Kit OCR runs locally, including on a fresh offline installation.
- Only text explicitly reviewed by the user reaches the relay.
- The workflow never posts automatically and does not use an accessibility service.
- The provider key exists only on the relay host.

## Workspace

- `apps/mobile` — Expo 57 / React Native product shell.
- `modules/lm-comment-android` — native bubble, one-shot capture, crop, OCR,
  generation, and result workflow.
- `apps/relay` — Fastify relay with the server-side Groq integration.
- `contracts` — shared JSON fixtures and transport schemas.

## Local prerequisites

- Node 22.13.1 and pnpm 10.34.5.
- Android Studio JBR 21, Android SDK 36, and NDK 27.1.12297006.
- Docker for relay image verification.

On Windows, set `JAVA_HOME` to Android Studio's `jbr` directory. The repository
doctor reports the detected setup without changing it:

```bash
pnpm doctor
```

## Build

```bash
pnpm install --frozen-lockfile
pnpm check
pnpm lint
pnpm typecheck
pnpm test
pnpm mobile:prebuild
pnpm mobile:android:debug
pnpm mobile:android:release
pnpm relay:build
```

For a faster physical-phone build during development:

```powershell
$env:LM_COMMENT_ANDROID_ARCH='arm64-v8a'
pnpm mobile:android:debug
```

Generated APKs are written under
`apps/mobile/android/app/build/outputs/apk/`. Android projects and binaries are
generated artifacts and are intentionally ignored by Git.

## Relay

Copy `.env.example` to `.env`, set a short-lived `DEMO_TOKEN` and a server-side
`GROQ_API_KEY`, then run:

```bash
pnpm relay:dev
```

The selected model is `openai/gpt-oss-120b`. It replaces the blueprint's
deprecated Llama model and remains relay configuration—not mobile code.

## Scope authority

The frozen scope is defined by `hackathon-release-contract.yaml`. The complete
implementation authority is `LM_COMMENT_FINAL_HACKATHON_IMPLEMENTATION_BLUEPRINT.md`.
