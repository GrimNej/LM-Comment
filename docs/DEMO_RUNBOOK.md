# Demo-day runbook

This runbook is for the final direct-installed Android APK. The authoritative spoken flow is in [`demo/presentation-script.md`](../demo/presentation-script.md), and all demo content must come from [`demo/fixtures.json`](../demo/fixtures.json) or equivalent synthetic text.

## Before leaving

- Keep the final signed APK on both the phone and laptop; record its SHA-256 and signing certificate.
- Verify `https://lmcomment-api.grimnej.com/healthz`, the active demo token, the fixed relay model, and the Groq spending limit.
- Keep the presentation script, synthetic target content, USB cable, charger/power bank, and mobile hotspot ready.
- Record the protected app through the visible Android emulator window on Windows, or record the priority phone externally with a second camera. Android's built-in recorder intentionally shows the secure workflow as black. Store the reviewed fallback video offline on both presentation devices.
- Do not put the Groq key, `.env`, demo token, or private content in slides, video, screenshots, or logs.

## Recording the secure workflow on Windows

1. Start the `LMComment_API36` emulator and install the x86_64 release APK.
2. Keep all target content synthetic. Do not show Settings, terminals, credentials, notifications, or private accounts.
3. Record the **Android Emulator - LMComment_API36:5554** window with OBS or Windows Game Bar. Do not use Android's recorder or `adb screenrecord`.
4. Run the same bubble, consent, crop, OCR, generation, edit, and copy workflow shown in the final phone build.
5. Review the exported video before uploading it. Confirm the crop and result screens are visible, no secrets appear, and the video stays under two minutes.

This is the normal protected build. `FLAG_SECURE` remains active after frame acquisition, captured pixels remain memory-only, and no privacy-disabled APK is used.

## Thirty minutes before judging

1. Restart the phone, disable battery saver, and enable Do Not Disturb.
2. Confirm free storage, notification permission, and **Display over other apps** access.
3. Open Home and confirm `Ready to start`; use Diagnostics to confirm relay health.
4. Run one real synthetic fixture generation and one complete capture/OCR flow.
5. Close the workflow, stop the bubble, clear the clipboard, and close unrelated apps.
6. Keep the relay warm with a health check; do not redeploy or change settings.

## Immediately before presenting

1. Put the prepared synthetic post on screen.
2. Open LM-Comment Home, confirm `Ready to start`, and start exactly one bubble.
3. Keep the fallback fixture and offline video one gesture away.
4. Do not update the APK, backend, model, token, or phone OS.

## Recovery order

1. Retry capture once.
2. Use **Try with text** and a bundled synthetic fixture; this still uses the real relay.
3. Switch once to the prepared hotspot.
4. Play the reviewed offline fallback video.
5. Explain the architecture using non-sensitive evidence only.

Never debug with ADB or terminal commands in front of judges.

## After the demo

Stop the bubble, confirm its notification is gone, clear the clipboard, and close LM-Comment. Rotate or revoke the temporary demo token after the event if the APK will be shared.
