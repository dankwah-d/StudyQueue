# Practice session delivery

The practice UI now fetches `GET /api/questions/eligible` before each new presentation.
This returns an array of public question DTOs using the existing direct-prerequisite
mastery rule. Empty pools return `200 []`. Correct-answer identifiers, explanations,
and answer-pool configuration are omitted before submission.

A page lifetime is one practice session. The browser shuffles eligible IDs once,
then consumes that stable queue without repeats until the currently eligible pool
is exhausted. A new cycle reshuffles; it avoids an immediate repeat when another
question is available. Reloading starts a new session. Navigation among practice,
map, and statistics keeps the current session.

Each refresh removes relocked/deleted questions and appends shuffled, newly eligible
unseen IDs after the remaining queue. Unlocking never reshuffles pending questions.
A failed fetch does not advance the queue. With a single eligible question, repetition
after exhaustion is necessary. Sessions are local to each page; attempts and mastery
retain their existing installation-wide scope.

Prerequisite edges already exist in `concept_prerequisites`; no new map is needed.
Both pool delivery and the legacy recommendation use `ConceptStatsSql.ELIGIBLE`.
This is prerequisite-aware eligibility rather than a new question type or a graph
rebuild. Direct prerequisites must be mastered; ancestor eligibility is not recursive.
The existing graph, blocker explanations, and mastery rules remain unchanged.

## Legacy recommendation endpoint

`GET /api/questions/next` returns one question using the same public fields as
`GET /api/questions`: `id`, `prompt`, `options`, and nullable `concept` (`id`,
`name`). Correct answers and explanations are only returned after submission.
When no eligible questions exist, the endpoint returns HTTP 204 with no body.

Before ranking, the strategy excludes concepts whose direct prerequisites are not
all mastered. Concepts without prerequisites and unassigned questions are eligible.
Selection and statistics share the same eligibility SQL and mastery thresholds.

The initial strategy orders eligible questions by:

1. Concept state: not started, practicing, mastered, then unassigned questions.
2. Lowest unrounded concept accuracy within the same state.
3. Fewest concept attempts.
4. Fewest attempts on the individual question.
5. Lowest question ID for a deterministic tie break.

Concepts with no questions are naturally excluded. Mastered concepts remain
available for review when no unstarted or practicing concepts have questions.
Unassigned questions are available when there are no eligible questions linked to concepts.
The mastery thresholds are shared with `ConceptState` (5 attempts and 80%).

The legacy endpoint remains available to existing API clients. The practice page uses
the session pool described above. A failed request offers a retry; an empty
question bank offers Check for questions. The existing list endpoint is retained.

Selection uses all recorded attempts in the installation. It does not yet model
individual learner accounts or study sessions. A question
may repeat when it remains the highest priority, especially with only one seeded
question. GET requests do not record attempts or reserve questions.

V5 adds `concept_prerequisites` and seeds Processes as a prerequisite for Threads
and CPU Scheduling. All direct prerequisites must satisfy mastery (at least 5
attempts and at least 80% unrounded accuracy); falling below mastery relocks their
dependents. Existing attempts are retained. Eligibility filters recommendations;
the list and answer endpoints remain available for reviewing existing questions.

The table rejects self edges and duplicates and enforces valid concept IDs. There
is no graph-editing API or cycle detection yet; future graph migrations must keep
the graph acyclic. Eligibility checks direct prerequisite mastery, not recursive
eligibility of ancestors. A prerequisite with no questions cannot be mastered.

Verification:

- `.\mvnw.cmd test`: PostgreSQL and HTTP checks cover empty banks, concept ordering,
  state changes, fewer-attempt tie breaks, unassigned fallback, and answer hiding.
  Selection fixtures use transaction-local temporary tables, rolled back after tests.
- `node --test tests/concept-stats.test.cjs`: frontend checks include recommendation
  requests after answers, retry recovery, and empty-bank behavior.
