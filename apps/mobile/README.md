# LM-Comment Android app

This package is the Expo 57 / React Native shell for LM-Comment. The bubble, one-shot MediaProjection capture, crop, bundled on-device OCR, relay generation, editing, and copy workflow live in the linked Kotlin module at [`../../modules/lm-comment-android`](../../modules/lm-comment-android).

LM-Comment is Android-only for the hackathon and cannot run in Expo Go because it uses custom native services and an activity.

## Prerequisites

- Node 22.13.x and pnpm 10.34.5.
- Android Studio JBR 21.
- Android SDK/compile target 36 and NDK 27.1.12297006.
- An Android device or current Pixel emulator with API 36.

Run all commands from the repository root. On Windows, `pnpm doctor` locates the expected Java and Android setup.

## Demo configuration

Copy the root `.env.example` to `.env` and set only the temporary mobile demo values needed for a judge build:

```dotenv
EXPO_PUBLIC_RELAY_BASE_URL=https://your-relay.example
EXPO_PUBLIC_DEMO_TOKEN=your-temporary-demo-token
```

The Groq provider key is relay-only. Never prefix it with `EXPO_PUBLIC_`, add it to mobile configuration, or include it in an APK. The demo token is a temporary rate-limit gate rather than a provider credential; rotate it after the event.

## Generate and build Android

```powershell
pnpm install --frozen-lockfile
pnpm doctor
pnpm quality
pnpm mobile:prebuild
$env:LM_COMMENT_ANDROID_ARCH='x86_64' # Pixel emulator
pnpm mobile:android:debug
pnpm mobile:android:release
```

For the owner's typical 64-bit Android phone, use:

```powershell
$env:LM_COMMENT_ANDROID_ARCH='arm64-v8a'
pnpm mobile:android:release
```

The generated native project is under `apps/mobile/android/` and is ignored by Git. APKs are emitted under:

- `apps/mobile/android/app/build/outputs/apk/debug/app-debug.apk`
- `apps/mobile/android/app/build/outputs/apk/release/app-release.apk`

Verify the final release certificate and APK hash before distributing it. The direct-install hackathon build does not require an Android App Bundle or Play Store submission.

## Install on a connected device

Enable Developer options and USB debugging, connect the phone, then verify that ADB sees exactly the intended device:

```powershell
adb devices
adb install -r apps/mobile/android/app/build/outputs/apk/release/app-release.apk
```

On first launch, complete **Setup**: allow notifications, grant **Display over other apps**, and confirm relay health. Android asks for screen-capture consent each time the bubble starts a capture; LM-Comment does not request AccessibilityService or broad storage access.

## Development

For a custom development client with Metro:

```powershell
pnpm --filter @lm-comment/mobile start
```

Use `pnpm mobile:prebuild` again after changing Expo plugins or Android configuration. Keep durable native implementation changes in `modules/lm-comment-android`, not in the generated Android directory.

See the [root README](../../README.md), [demo runbook](../../docs/DEMO_RUNBOOK.md), and [test evidence](../../docs/TEST_EVIDENCE.md) for the complete release workflow.
