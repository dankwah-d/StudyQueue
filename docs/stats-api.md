# Statistics API

## Global attempt statistics

`GET /api/stats` returns totals across all recorded attempts:

```json
{
  "totalAttempts": 3,
  "correctAnswers": 2,
  "accuracyPercent": 66.7
}
```

## Attempt statistics by concept

`GET /api/stats/concepts` returns one entry per concept, ordered by concept name:

```json
[
  {
    "conceptId": 1,
    "conceptName": "Processes",
    "totalAttempts": 3,
    "correctAnswers": 2,
    "accuracyPercent": 66.7,
    "state": "PRACTICING",
    "eligible": true,
    "prerequisites": []
  }
]
```

Accuracy is the percentage of correct attempts, rounded to one decimal place.
Concepts with no attempts are included with zero counts and `0.0` accuracy.
Attempts for questions not associated with a concept remain part of the global
statistics but are not assigned to a concept.

Concept state is derived from all recorded attempts when statistics are requested:

- `NOT_STARTED`: no attempts.
- `MASTERED`: at least 5 attempts and at least 80% correct.
- `PRACTICING`: all other cases.

The mastery rule uses unrounded accuracy. State is not stored separately and may
return to `PRACTICING` after an incorrect answer. These are initial project rules,
not a validated measure of learning. There are no learner accounts yet, so state
and statistics reflect all attempts in this installation.

`eligible` is true when every direct prerequisite is mastered. Concepts without
prerequisites are eligible immediately. Eligibility is independent of the concept's
own state, so a concept can retain historical mastery while currently locked.
The practice table displays eligibility as Available or Locked.

`prerequisites` lists every direct prerequisite, ordered by concept name, with
`conceptId`, `conceptName`, `totalAttempts`, `accuracyPercent`, and `state`. Root
concepts return an empty array. Counts, eligibility, and prerequisite states are
read in one database snapshot. The UI names unmet prerequisites with their attempt
counts and accuracy; when all are mastered it lists their names as mastered.
Prerequisites are not recursively expanded in this response.

## Practice page

The practice page displays concept statistics below the question card. It loads
statistics on page load and refreshes them after each successful answer submission.
An empty concept list shows an empty state. If statistics cannot be loaded, the
page offers a retry button and question practice remains available. Counts include
all recorded attempts, rather than only the current practice session.
