# Introductory Operating Systems question bank

The bank contains 133 questions after V12: 112 single-answer multiple-choice
questions and 21 sequence questions. Each of the seven concepts has 16 MCQs and
three ordering questions, for 19 questions per concept.

Sequence questions show shuffled steps. Select each step from first to last,
use **Undo last step** to revise, then submit the complete order. Grading requires
an exact match; incorrect answers show the correct order and explanation.
V11 adds a process-state sequence question and the schema support for this type.
V12 adds 20 ordering questions, preserving existing questions and attempts.

| Ordering topic | Scenarios |
| --- | --- |
| Processes | Process states, child fork/exec lifecycle, system-call flow |
| Threads | Worker join, FIFO worker pool, barrier arrivals |
| CPU Scheduling | FCFS completion, shortest-job-first, round-robin slices |
| Interprocess Communication | Length-prefixed receive, pipe EOF, request-response |
| Synchronization | Mutex-protected increment, bounded-buffer producer, condition-variable consumer |
| Deadlock | Lock ordering, safe completion sequence, detection and recovery |
| Memory Management | TLB-miss translation, demand paging, LRU eviction preference |

Prompts specify scheduling, protocol, and resource assumptions so each sequence
has one correct order. V12 runs on the next application startup. Integration tests
check coverage and correct/incorrect grading for all 21 sequence questions.

| Concept (MCQ count) | MCQ topics |
| --- | --- |
| Processes (16) | Process state, PCBs, context switching, address spaces, UNIX fork/exec/wait, zombies, PIDs, privilege modes, timer preemption |
| Threads (16) | Shared resources, stacks, concurrency, parallelism, races, joins, mapping models, thread-local storage, worker pools, lifetime, serial speedup limits |
| CPU Scheduling (16) | Round-robin and SJF traces, turnaround/response/waiting time, throughput, convoy effect, quantum overhead, aging, SRTF, priority inversion and inheritance |
| Interprocess Communication (16) | Isolation, shared memory, messages, pipes and EOF, sockets, stream framing, partial reads, nonblocking operations, backpressure, retry deduplication |
| Synchronization (16) | Critical sections, locking protocols, semaphores, condition predicates, bounded buffers, spinlocks, barriers, reader-writer locks, atomics, fairness |
| Deadlock (16) | Necessary conditions, lock ordering, try-lock, starvation, livelock, resource graphs, safe states, resource calculations, detection, avoidance, recovery |
| Memory Management (16) | Page tables, address calculations, TLBs, faults, fragmentation, dirty pages, thrashing, copy-on-write, protection, LRU, multi-level tables, locality |

V1 seeds the original process question. V3 links it to Processes. V4 adds two
concepts and five questions, bringing each concept to two questions. V4 preserves
existing questions and attempts; it does not require IDs to have particular values.

V8 adds six questions per existing concept, mixing definitions, scenarios, and
small calculations. Every new question has four choices and an explanation.
Existing question IDs and attempt records remain intact. Prerequisites and mastery
rules are unchanged. The migration runs on the next application startup.

V9 adds four concepts with eight questions each. Each question has four stable
answer IDs, one correct answer, and an explanation. The new prerequisite edges are:

- Processes -> Interprocess Communication
- Processes -> Memory Management
- Threads -> Synchronization
- Synchronization -> Deadlock

Existing edges remain. New concepts appear in the map and statistics automatically;
their questions become eligible when their direct prerequisites are mastered.
Both migrations are additive and run on application startup; they do not reset progress.

V10 doubles the bank from 56 to 112 by adding eight questions per concept.
The additions mix distinct scenarios, calculations, and definitions, each with four
choices and an explanation. No existing question, prerequisite, or attempt is
changed. V10 runs on the next application startup. Integration checks validate
all 112 seeded questions, both correct and incorrect grading, and prerequisite unlocking.

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
