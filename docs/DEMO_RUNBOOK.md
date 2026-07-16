# Demo-day runbook

This runbook is for the final direct-installed Android APK. The authoritative spoken flow is in [`demo/presentation-script.md`](../demo/presentation-script.md), and all demo content must come from [`demo/fixtures.json`](../demo/fixtures.json) or equivalent synthetic text.

## Before leaving

- Keep the final signed APK on both the phone and laptop; record its SHA-256 and signing certificate.
- Verify `https://lmcomment-api.grimnej.com/healthz`, the active demo token, the fixed relay model, and the Groq spending limit.
- Keep the presentation script, synthetic target content, USB cable, charger/power bank, and mobile hotspot ready.
- Record the priority phone externally with a second camera and store the fallback video offline on both presentation devices. The secure workflow may appear black to Android's built-in recorder. This owner action is not complete until the real file has been reviewed.
- Do not put the Groq key, `.env`, demo token, or private content in slides, video, screenshots, or logs.

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
