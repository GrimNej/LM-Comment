# Post-hackathon roadmap

The hackathon branch stays intentionally narrow: direct-installed Android APK, temporary demo token, one fixed Groq-backed relay, memory-only screenshots, local OCR, and explicit copy. The items below start only after the judge workflow and five-rehearsal gate are complete.

## Phase 1: decide whether to productize

- Review Google Play and overlay/MediaProjection policy feasibility.
- Write a public privacy policy and user-facing data-flow explanation.
- Decide whether public use needs accounts or can remain account-free.
- Replace the temporary demo gate with production authentication and abuse controls.
- Add AI-output reporting and operational incident handling.
- Expand privacy, security, and device-compatibility testing.

## Phase 2: broaden the product safely

- Add Android Keystore-backed credential handling if direct bring-your-own-key mode is approved.
- Evaluate multiple providers only after a stable provider abstraction exists.
- Add encrypted writing profiles and an explicit retention model.
- Expand OCR language support, including Devanagari, and test a broader OEM/device matrix.
- Add privacy-preserving production metrics and performance tracing.

## Phase 3: research, not promises

- Application-window capture on supported Android versions.
- Session-based Focus Mode only if policy and lifecycle behavior are acceptable.
- Profile synchronization and draft history with clear deletion controls.
- Streaming, model routing, and additional response controls.

No deferred item should be added to the hackathon release merely to make the feature list look larger. Each phase needs a new threat model, scope contract, success metrics, and release gate.
