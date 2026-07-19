# ImpactForge submission plan

Status checked on 2026-07-19 against the official ImpactForge overview, rules, schedule, updates, gallery, and discussions.

## Deadline and eligibility

- Submission deadline: July 24, 2026 at 12:30 PM Nepal time.
- Judging: July 24 at 9:45 PM through July 30 at 5:45 AM Nepal time.
- Winners announced: July 30 at 9:45 PM Nepal time.
- Team size: one to five people.
- The registration card limits participation to students who are above the legal age of majority. Companies and professional organizations are excluded.
- Standard excluded locations shown by Devpost: Brazil, Crimea, Cuba, Iran, North Korea, Quebec, and Russia.

Confirmed by the owner on 2026-07-19: current student, above the legal age of majority, and solo submission. Confirm the country of residence shown in the Devpost account before the final submission.

## What ImpactForge asks for

1. Project name.
2. Project story covering the problem, solution, features, tools, and intended users.
3. At least one visual proof item such as a screenshot, demo video, or file.
4. A project link, optional but encouraged.
5. A short impact statement.
6. Team members and contributions.

The standard Devpost form also asks for a tagline, thumbnail, built-with tags, project story, links, image gallery, video link, teammates, and agreement to the rules.

## Recommended public repository

Create one new public GitHub repository named `LM-Comment`. Do not submit an older project repository and do not make judges hunt for a special branch. Push the current tested history directly to the new repository's `main` branch.

Published repository: `https://github.com/GrimNej/LM-Comment`. The tested history is published as `main`. Its first commit is dated July 15, 2026, after ImpactForge opened on June 19.

The repository was created empty and published with:

```powershell
git remote add origin https://github.com/GrimNej/LM-Comment.git
git push -u origin HEAD:main
```

Then create a GitHub Release named `ImpactForge 2026 submission`, attach the signed APK as a release asset, and publish its SHA-256 in the release notes. Do not commit the APK, `.env`, signing key, signing password, or local evidence directory.

Use these Devpost links:

- Project or code repository: `https://github.com/GrimNej/LM-Comment`
- Try it out: `https://github.com/GrimNej/LM-Comment/releases/tag/impactforge-2026`
- Video demo: a public or unlisted YouTube or Vimeo URL that plays without a login and permits embedding

## Recommended submission copy

Project name: `LM-Comment`

Tagline:

> Turn text on any Android screen into private, editable reply options without auto-posting.

Impact statement:

> Writing a thoughtful reply can be difficult when tone, language confidence, or cognitive load gets in the way. LM-Comment lets people work from text already on their Android screen, review local text recognition, and shape several reply options without surrendering control of the final message. It reduces communication friction while keeping screenshots on-device and requiring an explicit edit or copy instead of posting automatically.

Built-with tags:

- Android
- Kotlin
- Jetpack Compose
- Expo
- React Native
- TypeScript
- Fastify
- Groq
- ML Kit Text Recognition
- Oracle Cloud
- Caddy

## Story structure

Use short sections in this order:

1. The problem: copying context between apps is slow, tone is difficult, and people should not have to hand control of posting to an assistant.
2. The solution: a compact Android lens captures one user-approved frame, crops locally, recognizes text on-device, and produces editable reply choices.
3. User agency: the user reviews OCR, chooses tone and count, edits a result, and copies it. LM-Comment never posts.
4. Privacy boundary: screenshots remain in memory on the phone. Only reviewed text and generation choices reach the relay. The Groq key remains server-side.
5. Technical build: native Kotlin and Compose for the secure workflow, Expo for the product shell, bundled ML Kit OCR, and a bounded Fastify relay on an isolated Oracle VPS.
6. Challenges: Android MediaProjection timing, overlay lifecycle, rotation, bounded bitmap ownership, and provider-secret isolation.
7. Evidence: signed APK, Android 16 runtime checks, 106 native JVM tests, four device tests, release lint, provider-boundary scans, and a public health endpoint.
8. Next step: broaden device testing and accessibility research without adding auto-posting or screenshot storage.

## Judging strategy

- Build Quality, 30%: lead with the working signed APK, real phone demo, test evidence, strict resource cleanup, and deployed relay.
- Real-World Impact, 25%: explain the communication barrier and show how the workflow helps without removing user agency.
- Creativity and Approach, 20%: emphasize the cross-app lens, local crop and OCR, and deliberate edit/copy boundary.
- User Experience, 15%: show the full flow without opening technical screens. Keep every tap visible and intentional.
- Clarity, 10%: use the submission thumbnail, concise story, two-minute video, architecture diagram, public repository, and direct APK release link.

The strongest award positioning is The Forge Award first and Community Champion second. The privacy-aware overlay workflow also supports the Disruptor Award.

## Final submission checklist

- [x] Confirm student status, legal age, and solo entry.
- [ ] Confirm the Devpost account's country of residence is eligible.
- [ ] Join ImpactForge on the correct Devpost account.
- [ ] Create a draft submission before recording the final video.
- [x] Create the public GitHub repository and push this history as `main`.
- [x] Publish the `impactforge-2026` GitHub Release with the signed ARM64 APK and checksum.
- [ ] Upload `docs/assets/impactforge-thumbnail.png` as the Devpost thumbnail.
- [ ] Record the script in `demo/presentation-script.md` using only synthetic content.
- [ ] Upload the video to YouTube or Vimeo and test playback while logged out.
- [ ] Paste the story, impact statement, built-with tags, repository URL, release URL, and video URL.
- [ ] Add your name and exact contribution. Add teammates only if they actually contributed.
- [ ] Preview the published submission and test every link from a private browser window.
- [ ] Submit before the deadline, then recheck that Devpost shows the project as submitted.

## Official references

- ImpactForge overview: https://impactforge.devpost.com/
- ImpactForge rules: https://impactforge.devpost.com/rules
- ImpactForge schedule: https://impactforge.devpost.com/details/dates
- Devpost submission steps: https://help.devpost.com/article/126-know-your-submission-steps
- Devpost video upload: https://help.devpost.com/article/85-uploading-a-demo-video
- Devpost video guidance: https://help.devpost.com/article/84-video-making-best-practices
