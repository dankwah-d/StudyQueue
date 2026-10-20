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
