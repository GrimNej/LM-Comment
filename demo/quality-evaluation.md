# Model quality evaluation

Dataset: [`quality-set.json`](./quality-set.json)

Status: **PENDING HUMAN REVIEW**. The dataset and structural validator are complete; no score is claimed until the frozen relay/model has been run and a person has reviewed every output.

## Scoring rubric

Score each dimension from 1 (poor) to 5 (excellent):

1. Relevance to the supplied context.
2. Naturalness for a real reply/comment.
3. Match to the requested tone.
4. Specificity grounded in the supplied facts.
5. No invented facts.
6. No filler or meta commentary.
7. Exact requested option count with no normalized duplicates.

Acceptance target: every scenario returns the exact requested count, no scenario invents a fact, and the mean of dimensions 1-6 is at least 4.0 with no dimension below 3 for any judge fixture.

## Review record

| Field | Result |
| --- | --- |
| Frozen model | `openai/gpt-oss-120b` |
| Scenarios reviewed | PENDING / 30 |
| Reviewer | PENDING |
| Review date | PENDING |
| Mean score | PENDING |
| Exact-count failures | PENDING |
| Duplicate failures | PENDING |
| Invention failures | PENDING |

Do not paste generated replies into logs or diagnostics. Keep the completed score sheet offline with the hackathon evidence package.
