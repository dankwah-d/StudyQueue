# Practice session delivery

The practice UI fetches `GET /api/questions/eligible` before each new presentation
in Randomized and Question ID modes. Recommended mode fetches
`GET /api/questions/recommended`, a ranked eligible pool with the same public fields.
This returns an array of public question DTOs using the existing direct-prerequisite
mastery rule. Empty pools return `200 []`. Correct-answer identifiers, explanations,
and answer-pool configuration are omitted before submission.

A page lifetime starts one practice session. The browser shuffles eligible IDs,
then consumes that stable queue without repeats. Exhaustion shows Session complete;
it never silently starts another cycle. New session resets only the local queue and
answer counter, preserving persisted attempts. Reloading also starts a new session.
Navigation among practice, map, and statistics keeps the current question and feedback.

Choose a concept and question order, then press New session to apply the settings.
Only eligible questions in that concept enter its queue. Locked concepts are labeled
and disabled. Randomized is the default; Question ID (ascending) is an explicit,
deterministic review order, not a curriculum or prerequisite traversal. Recommended
uses the ranking below and reranks unseen questions on each delivery, including
newly unlocked questions. Seen IDs remain excluded until New session. Changing
the order selector takes effect only after a successful New session request.

In Randomized and Question ID modes, each delivery removes relocked/deleted
questions and appends newly eligible unseen
IDs after the remaining queue (shuffled or sorted within the additions). Unlocking
never reshuffles pending questions. A failed fetch does not advance the queue.
The progress count is answered questions out of answered/presented plus currently
pending questions; its denominator can change as eligibility changes. Exhausted
sessions may check for newly unlocked questions without repeating seen questions.
Sessions are local to each page; attempts and mastery retain installation-wide scope.

Answer options shuffle independently of question ordering and always submit their
original identifiers. A pending answer disables option changes and session restart;
an ambiguous failure retains its UUID for retry. Feedback stays until Continue or
an explicit New session. A successful answer refreshes concept statistics; the next
question fetch recomputes prerequisite eligibility from the persisted attempts.

Prerequisite edges already exist in `concept_prerequisites`; no new map is needed.
Both pool delivery and the legacy recommendation use `ConceptStatsSql.ELIGIBLE`.
This is prerequisite-aware eligibility rather than a new question type or a graph
rebuild. Direct prerequisites must be mastered; ancestor eligibility is not recursive.
The existing graph, blocker explanations, and mastery rules remain unchanged.

## Shared recommendation ranking

`GET /api/questions/next` returns one question using the same public fields as
`GET /api/questions`: `id`, `prompt`, `options`, and nullable `concept` (`id`,
`name`). Correct answers and explanations are only returned after submission.
When no eligible questions exist, the endpoint returns HTTP 204 with no body.

Before ranking, the strategy excludes concepts whose direct prerequisites are not
all mastered. Concepts without prerequisites and unassigned questions are eligible.
Selection and statistics share the same eligibility SQL and mastery thresholds.

Both `/recommended` and `/next` use one shared ranking query. The strategy orders
eligible questions by:

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


## Prerequisite order (topological)

Select Prerequisite order (topological), then New session. The browser fetches
`GET /api/questions/topological` for every presentation. The backend uses Kahn's
algorithm over all concepts and prerequisite edges, including concepts without
questions. Edges run from prerequisite to dependent. When multiple concepts are
ready, the lowest concept ID wins; questions within each concept use ascending
question ID. Unassigned questions come last. This produces a deterministic valid
topological order, rather than an authored curriculum or an accuracy ranking.

The complete graph is sorted before filtering questions with the existing direct
prerequisite mastery rule. Topological position does not unlock a concept. Reads
of the graph and eligibility share a repeatable-read transaction. Empty pools
return `200 []`; a cycle anywhere in the graph returns HTTP 409 with no partial
pool. The UI explains graph conflicts and offers retry or another session mode.

On each delivery, unseen eligible questions follow the current server order,
including newly unlocked questions. Relocked questions are removed and seen IDs
remain excluded until New session. Concept filters and completion work as in the
other modes. No database migration or graph-editing endpoint was added.
