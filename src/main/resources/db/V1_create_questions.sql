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
