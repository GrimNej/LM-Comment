# Judge rehearsal checklist

Complete this only after the final APK and relay configuration are frozen. Any code, APK, token, model, or relay change that affects the workflow resets the five-run count.

## One-time owner setup

- [ ] Install the final signed APK on the priority phone.
- [ ] Confirm the phone model and Android version in the record below.
- [ ] Grant notifications and **Display over other apps** access.
- [ ] Confirm Home reports `Ready to start` and Diagnostics reports healthy relay status without exposing the demo token.
- [ ] Confirm the current demo token is active and the Groq spending limit is configured.
- [ ] Save the APK, presentation script, synthetic fixture, and owner-recorded fallback video offline.
- [ ] Record a short fallback video of the priority phone with a second camera; the secure workflow may appear black to Android's built-in recorder. Use only synthetic content and do not expose credentials.

## Before each rehearsal

- [ ] Use the same synthetic target content and presentation script.
- [ ] Enable Do Not Disturb and disable battery saver.
- [ ] Confirm network or hotspot availability.
- [ ] Clear the clipboard and close unrelated apps.
- [ ] Start from Home with exactly one bubble.
- [ ] Do not use ADB, a terminal, Settings changes, app restarts, or backend changes.

## Required flow

- [ ] Start and drag the bubble.
- [ ] Open synthetic content, tap the bubble, and approve capture.
- [ ] Confirm no LM-Comment surface appears in the captured frame.
- [ ] Crop, run OCR, review/edit text, and choose a tone.
- [ ] Generate three options through the real relay.
- [ ] Select and edit one option, save it, and explicitly copy it.
- [ ] Close; confirm exactly one bubble returns; stop the bubble.
- [ ] Confirm no capture notification or projection remains.

## Five consecutive rehearsals

| Run | Date/time | Result | Duration | Notes |
|---:|---|---|---:|---|
| 1 |  | PENDING |  |  |
| 2 |  | PENDING |  |  |
| 3 |  | PENDING |  |  |
| 4 |  | PENDING |  |  |
| 5 |  | PENDING |  |  |

A run is a failure if it needs terminal use, ADB, app restart, redeployment, a token/configuration edit, cache clearing, or an explanation for a broken feature.

## Physical acceptance record

- Phone model: `PENDING OWNER`
- Android version: `PENDING OWNER`
- APK SHA-256: `PENDING FINAL APK`
- Signing certificate SHA-256: `PENDING FINAL APK`
- Externally recorded fallback video filename: `PENDING OWNER RECORDING`
- Completed by/date: `PENDING`

In addition to the five judge rehearsals, the blueprint's H7 gate requires the priority-phone matrix: 20 complete workflows, 20 consent cancellations, 10 bubble start/stop cycles, rotations, lifecycle checks, failure cases, and post-workflow resource checks. Record those results in `docs/TEST_EVIDENCE.md`; do not infer them from emulator tests.
