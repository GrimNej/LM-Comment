# Product copy style

LM-Comment uses direct, conversational product language. Text should help someone act without making the interface sound like a pitch deck.

## Rules

- Use sentence case for headings, labels, buttons, and status text.
- Lead with the action or outcome.
- Prefer one clear sentence to a label, headline, and subtitle that repeat the same idea.
- Use familiar words and active voice.
- Name the real object: bubble, capture, crop, text, relay, result.
- State privacy boundaries precisely. Say that the captured frame stays in memory, the screenshot is never sent, and the relay receives reviewed text plus generation choices.
- Use contractions when they sound natural.
- Do not use decorative all-caps labels.
- Do not use em dashes.
- Avoid promotional filler such as "judge-ready," "private by design," "real workflow," and "seamless."
- Keep stable error codes in code. Translate them into short, useful messages in the interface.

## Check

Run the repeatable copy audit with:

```bash
pnpm check:copy
```

The check covers the mobile shell, native workflow text, demo fixtures, and root README.

## Research used

- [Google capitalization guidance](https://developers.google.com/style/capitalization) recommends sentence case and warns against unnecessary capitalization.
- [Google heading guidance](https://developers.google.com/style/headings) favors descriptive sentence-case headings with simple punctuation.
- [Microsoft's product voice guidance](https://learn.microsoft.com/en-us/style-guide/top-10-tips-style-voice) recommends crisp minimalism, fewer words, and language that sounds natural when read aloud.
- [Microsoft's concise sentence guidance](https://learn.microsoft.com/en-us/style-guide/word-choice/use-simple-words-concise-sentences) recommends precise verbs and removing words that add no substance.
- [Wikipedia's overview of AI-generated writing signals](https://en.wikipedia.org/wiki/AI-generated_content_on_Wikipedia) records excessive em dash use and promotional wording among common review signals.
