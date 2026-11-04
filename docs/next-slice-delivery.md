# Practice session slice delivery

## A. Implemented

Extended the existing browser frontend: explicit session exhaustion, New session,
answered/available progress with a native progress bar, concept selection, randomized
or ascending question-ID order, responsive settings, and locked concept labels.
Reused existing shuffle, answer feedback, Next question, statistics, and prerequisite
eligibility. Answer inputs now lock immediately during submission. Ambiguous retries
also block session restart until confirmed. No IDs, schema, data, deployment, or
backend validation behavior changed. No commit or push was performed by this agent. During verification, repository HEAD
changed externally to `f5e7b2c` and included the frontend/test edits; only the
documentation changes remained uncommitted at final inspection.

## B. Files changed

- `src/main/resources/static/index.html`: settings, progress, explicit completion,
  concept filtering, restart, and submission locking.
- `src/main/resources/static/practice-session.js`: stop on exhaustion, track total,
  and support explicit ascending order; retain eligibility reconciliation.
- `src/main/resources/static/studyqueue.css`: responsive settings and progress styling.
- `tests/practice-session.test.cjs`: exhaustion, fresh random sessions, explicit order.
- `tests/concept-stats.test.cjs`: completion, preserved feedback/statistics, restart,
  concept selection, duplicate submission; update former implicit-cycle assumptions.
- `docs/question-selection.md`: current session contract and state connections.
- `docs/next-slice-delivery.md`: audit, verification, demo, and engineering receipt.

## C. Audit and verification (2026-10-29)

Audited QuestionController, StatsController, Question/Concept/Attempt entities,
QuestionRepository, AttemptRepository and projections/shared SQL, AnswerService,
AnswerOptions, ConceptState, HTML/CSS/session/graph scripts, migrations V1?V7, and
the existing Java/Node tests. Questions are read from PostgreSQL through public DTOs.
Correct IDs and explanations are withheld until evaluation. AnswerService compares
stable option IDs, persists attempts transactionally, and deduplicates submission
UUID retries. Flyway already supplies concepts, prerequisites, and distractor pools.

Baseline: `node --test tests/concept-stats.test.cjs tests/practice-session.test.cjs`
passed 23 tests; `.\mvnw.cmd --batch-mode --no-transfer-progress test` passed 26.
Baseline log: `target/baseline-tests.log` (untracked build output).

Final: same Node command passed 26 tests. `.\mvnw.cmd --batch-mode
--no-transfer-progress verify` passed 26 Java tests and produced the runnable JAR.
Log: `target/slice-verify.log`. `git diff --check` passed.
Java checks exercise real dedicated PostgreSQL on 5434, question retrieval/eligibility,
correct and incorrect evaluation, persisted attempt counts, idempotent retries,
statistics, mastery transitions, and answer pool correctness. Node checks exercise
randomized order, no premature repeats, stable option IDs, explanation retention,
completion/restart, concept filtering, and deterministic order.

Live packaged app smoke check on 8082 with the isolated test datasource: root HTML,
practice-session.js and CSS returned HTTP 200; the page contained New session;
/api/questions/eligible returned two seeded questions; /api/stats returned zero
attempts. These were read-only checks. PowerShell initially failed its legacy HTML
parser; retry with -UseBasicParsing succeeded.

Browser visual/interaction check NOT completed: in-app browser host unavailable;
Windows Firefox automation stopped because browser URL policy enforcement is
unsupported. DOM-stub tests and HTTP checks do not establish visual correctness or
mobile layout correctness. No remaining automated test failures.

## D. Manual demo

From the repository root, with Java 21 and Docker Desktop running, ensure `.env`
exists with the values documented in `.env.example` (keep existing values):

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Open http://localhost:8081. Select an available concept and Randomized, then New
session. Choose an option, Check answer, inspect feedback and explanation. Double
clicking Check answer must save one attempt. Leave the explanation visible; visit
Progress and return to confirm it remains. Press Next question and finish the pool.
Confirm Session complete, no automatic repeats, and accurate progress. Press New
session to practice again and verify recorded statistics remain. Random order can
coincidentally match across sessions, particularly with only two available questions.
Choose Question ID (ascending) plus a concept and New session for deterministic
review order. Answer options still shuffle independently. Narrow the window to a
mobile width and confirm controls wrap and choices remain readable.

Fresh seeds offer only two Processes questions. Mastery requires five attempts at
80% accuracy, so start additional sessions to unlock Threads and CPU Scheduling.
On subsequent Next question requests, newly eligible questions append to the queue.
Restarting or reloading resets local session progress, never recorded attempts.

## E. Engineering receipt

The existing app already implemented most of this directive. The bounded change
replaces silent cyclic practice with explicit sessions and exposes settings/progress.
No new framework, API, migration, prerequisite graph, or adaptive architecture was
needed. Explicit order means question-ID order; the database has no authored
curriculum sequence. Eligibility refresh can change the progress denominator.
Newly eligible IDs append after pending IDs; ordered additions sort among themselves.
A single-question pool completes after one presentation. Sessions and pending retry
payloads remain page-local and are lost on reload. Persisted statistics are shared
installation-wide, not scoped to individual learners.

## F. Recommended next slice

Existing loop: answer -> persisted attempt -> aggregate concept state -> direct
prerequisite eligibility -> next pool. The legacy /api/questions/next already ranks
eligible concepts by state, accuracy and attempt counts, but random session practice
does not use that ranking. Add an explicit recommended-order mode that returns a
ranked eligible pool using shared backend ranking rules, while retaining the browser
seen-ID queue and completion contract. Test ranking changes after an answer,
unlock/relock behavior, and no repeats. Keep mastery thresholds unchanged. Defer
individual learner accounts, durable sessions, recursive prerequisite traversal,
and authored learning sequences to separately scoped work.


## G. Recommended-order continuation (2026-11-04)

Implemented the recommended next slice in the main StudyQueue checkout. Select
Recommended (adapts after answers), then New session. The new read-only
`/api/questions/recommended` endpoint returns the eligible pool using the same
ranking SQL as `/next`. The browser reranks unseen questions on each delivery,
filters by the active concept, removes relocked questions, and never repeats seen
IDs until restart. Randomized and ascending-ID modes retain their stable queues.
No migrations, mastery thresholds, persisted data, or Railway files changed.

Validation: 28 Node checks passed; Maven verify passed all 27 Java tests and built
`target/studyqueue-0.0.1-SNAPSHOT.jar`. The integration test checks ranked pool
ordering against `/next`, answer-driven ranking changes, prerequisite unlock and
relock, and withheld solutions. Frontend checks cover changing rankings, seen-ID
exclusion, exhaustion, relock/unlock, and order settings applying only on restart.
`git diff --check` passed. Log: `target/recommended-verify.log`.

Docker Desktop was unavailable. Verification instead used the existing isolated
PostgreSQL 18 test instance on port 5434; the development database was not used.
Browser visual and real-browser interaction checks remain outstanding. No commit,
push, or deployment was performed.


## H. Topological practice order (2026-11-04)

Added Prerequisite order (topological) and `/api/questions/topological`. The
TopologicalQuestions service runs Kahn's algorithm over all concepts and edges,
including concepts with no questions, before applying the existing eligibility
query. Lowest ready concept ID breaks ties; questions use ascending ID within a
concept; unassigned questions come last. The whole read uses a repeatable-read
transaction. Cycles return 409 and the frontend explains the conflict. No partial
order is delivered. The session honors server order on each delivery and excludes
seen IDs; settings still apply on New session.

Maven verify passed 30 Java tests and packaged the executable JAR. All 31 Node
checks passed. Coverage includes prerequisite-before-dependent ordering with
reverse-created IDs, multiple prerequisites, disconnected nodes, empty graphs,
cycles among questionless concepts, unlock/relock, stable question ordering,
no repeats, and UI conflict recovery. An initial test assertion used a matcher
that disallowed an empty expected list; corrected it to assert `[]` explicitly.
Log: `target/topological-verify.log`. `git diff --check` passed. The isolated
PostgreSQL test server on 5434 was used, then stopped. Browser visual review
remains outstanding. No migration, commit, push, or deployment.
