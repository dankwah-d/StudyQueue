CREATE TABLE attempts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id),
    selected_option_id VARCHAR(20) NOT NULL,
    correct BOOLEAN NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT attempts_option_not_blank
        CHECK (length(trim(selected_option_id)) > 0)
);

CREATE INDEX attempts_question_id_idx ON attempts(question_id);