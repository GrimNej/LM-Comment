# Verified versions

Verified on 2026-07-15 against official documentation and package registries.

| Surface | Frozen choice |
|---|---|
| Expo | SDK 57.0.6 |
| React Native | 0.86.0 |
| React | 19.2.3 |
| Android compile / target SDK | 36 / 36 |
| Android minimum SDK | 26 |
| Node | 22.13.1 |
| pnpm | 10.34.5 |
| Java | Android Studio JBR 21 |
| Bundled ML Kit Latin OCR | `com.google.mlkit:text-recognition:16.0.1` |
| Groq default model | `openai/gpt-oss-120b` |

`llama-3.3-70b-versatile`, named in the original blueprint, was announced for
deprecation on 2026-06-17 and shuts down for free/developer access on 2026-08-16.
The replacement is kept in relay configuration and never in the APK.

References:

- https://docs.expo.dev/versions/latest/
- https://developer.android.com/media/grow/media-projection
- https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- https://console.groq.com/docs/deprecations
- https://console.groq.com/docs/models
- https://console.groq.com/docs/model/openai/gpt-oss-120b
