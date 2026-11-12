# Sequence questions

`questionType` is `MCQ_SINGLE` or `SEQUENCE` in question responses. For a sequence,
`options` maps stable step IDs to text; all steps are displayed in shuffled order.
The correct sequence is excluded from question responses.

Submit a complete permutation using `POST /api/questions/{id}/answer`:

```json
{"orderedOptionIds":["p","q","r","s","t"],"submissionId":"89f235a5-8919-45c3-a2d0-6abec47d969c"}
```

Omit `optionId` for sequences. Missing, repeated, null, or unknown step IDs are
rejected with 400 without recording an attempt. MCQs continue to use `optionId`.
Sequence results include `correct`, `correctOrder`, and `explanation`.
Only an exact match is correct; there is no partial credit. Both question types
contribute to the same accuracy, mastery, and eligibility calculations.

Attempts retain the submitted order and the evaluated correct order. Reusing the
same submission UUID and order returns the original result without another attempt;
changing the order for that UUID returns 409. The UI locks selection after an
ambiguous failure and retries the original payload. Clearing history removes
attempts for both types.

In seed data, set `question_type = 'SEQUENCE'`, `correct_option_id = NULL`, and
`correct_order` to a JSON array containing every option ID exactly once. Do not
set `display_option_count`: sequence questions always present every step.
