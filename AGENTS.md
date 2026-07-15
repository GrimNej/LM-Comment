# AGENTS.md

Build LM-Comment according to `LM_COMMENT_FINAL_HACKATHON_IMPLEMENTATION_BLUEPRINT.md`
and `hackathon-release-contract.yaml`.

## Priority

Finish the real judge workflow before secondary features.

## Mandatory sequence

H0 → H1 → H2 → H3 → H4 → H5 → H6 → H7. Do not start a later phase while an
earlier phase is red.

## Non-negotiable rules

- Never persist or upload a screenshot.
- Never place the Groq key in mobile code, build configuration, APK, logs, or fixtures.
- Do not add accounts, databases, direct provider mode, streaming, or Focus Session.
- Only the compact bubble is a system overlay; the workflow is a normal activity.
- `CaptureWorkflowActivity` explicitly uses `noHistory=false`.
- Keep `FLAG_SECURE` off during CaptureCloak and add it before sensitive UI is shown.
- Use separate `specialUse` bubble and `mediaProjection` capture services.
- Use a one-shot, full-display MediaProjection with allocation bounded in advance.
- Close capture resources on every terminal path; never use AccessibilityService.
- Never auto-post, and never hide the bubble with an unmeasured fixed delay.
- Do not claim completion without a build and evidence.
- Preserve user changes and never use destructive Git commands without approval.

## Completion report

Record files changed, behavior completed, exact tests, evidence path, commit hash,
next phase, and known limitations in `progress.md`.
