# StudyQueue checkpoint
Updated: 2026-10-20 (America/New_York)

Finished:
- Questions expose concepts; answer submission evaluates and records attempts.
- Global and concept statistics include counts and rounded accuracy.
- Concept state derives from all attempts: NOT_STARTED, PRACTICING, MASTERED. Mastery requires at least 5 attempts and 80% unrounded accuracy; it can regress.
- Adaptive selection prioritizes eligible unstarted concepts, weaker practicing concepts, then mastered concepts. Ties use concept attempts, question attempts, then question ID. Unassigned questions remain a fallback.
- V4 expands the bank to six questions: two each for Processes, Threads, and CPU Scheduling.
- V5 requires Processes mastery before Threads or CPU Scheduling becomes eligible. Every direct prerequisite must be mastered. Statistics and selection share eligibility SQL.
- The practice UI loads recommendations, refreshes statistics after answers, and handles loading, empty, retry, and failure states.
- Statistics expose direct prerequisite names, counts, accuracy, and state in one database snapshot. The UI explains unmet prerequisites and updates those explanations after answers.
- A read-only SVG concept graph draws prerequisite arrows and labels state and availability, refreshing with statistics. It includes accessible descriptions, horizontal scrolling, and empty/invalid-graph/failure states.
- Latest slice: redesigned the practice workspace with navigation, live progress summaries, practice/map columns on wide screens, full-width statistics, collapsible mastery rules, clearer option selection and feedback, and responsive/mobile styling in studyqueue.css. No backend or migration changes.

Verified:
- 19 Maven tests pass against PostgreSQL, including aggregation, mastery boundaries, recommendation ordering, multiple prerequisites, unlocking and relocking, missing prerequisite questions, and HTTP response details.
- 13 Node frontend checks pass, including recommendation requests, empty states, retry, statistics failures, state/eligibility refresh, blocker explanations, graph arrows/layers, multiple prerequisites, disconnected roots, and cycle/missing-concept handling.
- Maven resource processing passed; live HTTP checks returned 200 for the updated page and concept-graph.js. The backend is unchanged in the graph slice; the 19 Maven tests passed in the preceding slice.
- UI redesign: 13 frontend checks pass, including overview values updating after answers. HTML IDs, anchor targets, encoding, and CSS structure checked; Maven resources and live page/stylesheet HTTP checks passed. Browser visual review remains outstanding.
- Live API returns prerequisite details: Processes has 9 attempts, 6 correct, and 66.7% accuracy; Threads and CPU Scheduling are locked by Processes. Existing counts were preserved, with no live answers submitted for verification.
- git diff --check passes.

Remaining:
- Browser connection is unavailable. Visual layout, narrow-screen behavior, and real-browser practice checks remain unverified.
- Duplicate Flyway dependency warnings remain.
- Graph editing, database cycle prevention, learner accounts, and study sessions are not implemented. The graph view detects cycles for presentation. Eligibility uses direct prerequisite mastery.

Next: Complete browser smoke checks; then add concept-focused practice from the graph while preserving prerequisite eligibility.
First action: Open http://localhost:8081 and verify graph arrows, labels, recommendations, blocker explanations, updates after answers, page reload, and narrow-screen layout.

Verification commands (from the StudyQueue repository):
- `.\mvnw.cmd test` (requires the local PostgreSQL configuration)
- `node --test tests/concept-stats.test.cjs` (scripted DOM checks; no browser dependencies)

Related docs: stats-api.md, question-selection.md, question-bank.md, concept-graph.md, practice-ui.md.
