INSERT INTO concepts (name)
VALUES ('Threads'), ('CPU Scheduling')
ON CONFLICT (name) DO NOTHING;

INSERT INTO questions (prompt, options, correct_option_id, explanation, concept_id)
SELECT seed.prompt, seed.options::jsonb, seed.correct_option_id, seed.explanation, c.id
FROM (VALUES
    (
        'What does a process control block (PCB) store?',
        '{"A":"Only the executable file on disk","B":"The source code of every program","C":"Information such as process state, program counter, and CPU registers","D":"Only the contents of the CPU cache"}',
        'C',
        'A PCB records information needed to manage and resume a process, including its state, program counter, and saved CPU registers.',
        'Processes'
    ),
    (
        'Which resource is normally shared by threads in the same process?',
        '{"A":"The process address space","B":"A single shared program counter","C":"A single shared register set","D":"A single shared execution stack"}',
        'A',
        'Threads in one process share its address space and resources such as open files. Each thread has its own program counter, registers, and stack.',
        'Threads'
    ),
    (
        'On a single CPU core, how can multiple runnable threads make progress?',
        '{"A":"Every thread executes an instruction at exactly the same instant","B":"Each thread requires its own physical CPU core","C":"Threads must share one program counter","D":"The scheduler interleaves their execution over time"}',
        'D',
        'The scheduler can switch between runnable threads, allowing concurrent progress through interleaving. This does not require simultaneous execution on multiple cores.',
        'Threads'
    ),
    (
        'What characterizes round-robin CPU scheduling?',
        '{"A":"The shortest job always runs to completion first","B":"Each ready process receives a time quantum in turn","C":"The highest-priority process can never be preempted","D":"Processes are selected only when they perform disk I/O"}',
        'B',
        'Round-robin gives ready processes a time quantum in turn. A process that uses its quantum without finishing is preempted and normally placed at the back of the ready queue.',
        'CPU Scheduling'
    ),
    (
        'In CPU scheduling, what is turnaround time?',
        '{"A":"The time from process arrival to completion","B":"Only the time spent executing on the CPU","C":"Only the time from arrival to the first response","D":"The duration of a single context switch"}',
        'A',
        'Turnaround time is completion time minus arrival time. It includes CPU execution and time spent waiting, including I/O waits.',
        'CPU Scheduling'
    )
) AS seed(prompt, options, correct_option_id, explanation, concept_name)
JOIN concepts c ON c.name = seed.concept_name;
