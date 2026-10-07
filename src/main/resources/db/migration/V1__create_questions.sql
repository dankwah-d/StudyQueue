CREATE TABLE questions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prompt TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL DEFAULT 'MCQ_SINGLE',
    options JSONB NOT NULL,
    correct_option_id VARCHAR(20) NOT NULL,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT questions_prompt_not_blank
        CHECK (length(trim(prompt)) > 0),

    CONSTRAINT questions_supported_type
        CHECK (question_type = 'MCQ_SINGLE'),

    CONSTRAINT questions_options_object
        CHECK (jsonb_typeof(options) = 'object'),

    CONSTRAINT questions_correct_option_exists
        CHECK (options ? correct_option_id)
);

INSERT INTO questions (
    prompt,
    options,
    correct_option_id,
    explanation
)
VALUES (
    'Which statement best describes a process?',
    '{
        "A": "A program stored on disk",
        "B": "A program in execution",
        "C": "A physical CPU core",
        "D": "A file containing instructions"
    }'::jsonb,
    'B',
    'A process is a program in execution, including its execution state and resources.'
);