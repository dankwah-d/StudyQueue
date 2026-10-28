# Introductory Operating Systems question bank

The initial bank contains six single-answer multiple-choice questions:

| Concept | Topics |
| --- | --- |
| Processes | A process as a program in execution; process control blocks |
| Threads | Shared address space versus per-thread execution state; concurrency on one core |
| CPU Scheduling | Round-robin time quanta; turnaround time |

V1 seeds the original process question. V3 links it to Processes. V4 adds two
concepts and five questions, bringing each concept to two questions. V4 preserves
existing questions and attempts; it does not require IDs to have particular values.

This is introductory practice content, not coverage of the entire university
course. V5 makes Processes a prerequisite for Threads and CPU Scheduling. These
dependent concepts become eligible for adaptive selection after Processes is mastered.

After migration, `GET /api/questions` exposes the questions without correct answers
or explanations. `GET /api/stats/concepts` includes Threads and CPU Scheduling with
zero counts until they receive attempts. `GET /api/questions/next` prioritizes these
unstarted concepts after their prerequisite has been mastered. Until then it
recommends Processes practice.


## Optional answer pools

`questions.options` remains the JSON object mapping stable answer IDs to text.
V7 adds nullable `display_option_count`. Existing rows keep NULL and display every
choice, including questions with more than four options. To enable a distractor pool,
store additional plausible wrong answers in `options` and set `display_option_count`
to the desired number of displayed choices (at least 2). No new distractors or seed
questions are invented by this change.

For a pool with eight choices and `display_option_count = 4`, delivery samples three
incorrect choices without replacement and includes the correct choice. A count above
the pool size displays the entire pool. Choice IDs and texts remain unchanged; the
browser independently shuffles the returned entries for each new presentation,
including numeric-looking IDs that JSON object enumeration might otherwise reorder.
Retries retain the current choices. Grading compares the submitted stable option ID
with the persisted correct option ID, and returns the original explanation.

The existing answer endpoint still accepts any valid option ID in the stored pool;
it does not authenticate which subset a client was shown. Presentation-scoped answer
validation would require a server-issued presentation token and is outside this slice.
