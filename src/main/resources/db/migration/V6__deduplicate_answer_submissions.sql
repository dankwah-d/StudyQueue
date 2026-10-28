ALTER TABLE attempts
    ADD COLUMN submission_id UUID UNIQUE,
    ADD COLUMN evaluated_correct_option_id VARCHAR(20),
    ADD COLUMN evaluated_explanation TEXT;
