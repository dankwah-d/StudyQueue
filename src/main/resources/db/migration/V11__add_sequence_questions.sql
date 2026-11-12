ALTER TABLE questions DROP CONSTRAINT questions_supported_type;
ALTER TABLE questions DROP CONSTRAINT questions_correct_option_exists;
ALTER TABLE questions ALTER COLUMN correct_option_id DROP NOT NULL;
ALTER TABLE questions ADD COLUMN correct_order JSONB;

-- Validate complete permutations, including unique IDs and no missing/unknown steps.
CREATE FUNCTION valid_question_order(choices JSONB, ordering JSONB) RETURNS BOOLEAN
LANGUAGE SQL IMMUTABLE AS $$
    SELECT CASE WHEN jsonb_typeof(ordering) = 'array' THEN
        jsonb_array_length(ordering) >= 2
        AND jsonb_array_length(ordering) = (SELECT count(*) FROM jsonb_object_keys(choices))
        AND NOT EXISTS (SELECT 1 FROM jsonb_array_elements(ordering) item
                        WHERE jsonb_typeof(item) <> 'string' OR NOT choices ? (item #>> '{}'))
        AND (SELECT count(DISTINCT item) FROM jsonb_array_elements(ordering) item) = jsonb_array_length(ordering)
    ELSE FALSE END
$$;

ALTER TABLE questions ADD CONSTRAINT questions_supported_type
    CHECK (question_type IN ('MCQ_SINGLE', 'SEQUENCE'));
ALTER TABLE questions ADD CONSTRAINT questions_answer_valid CHECK (
    (question_type = 'MCQ_SINGLE' AND correct_option_id IS NOT NULL
        AND options ? correct_option_id AND correct_order IS NULL)
    OR (question_type = 'SEQUENCE' AND correct_option_id IS NULL
        AND correct_order IS NOT NULL AND valid_question_order(options, correct_order)
        AND display_option_count IS NULL)
);

ALTER TABLE attempts ALTER COLUMN selected_option_id DROP NOT NULL;
ALTER TABLE attempts ADD COLUMN selected_order JSONB;
ALTER TABLE attempts ADD COLUMN evaluated_correct_order JSONB;
ALTER TABLE attempts ADD CONSTRAINT attempts_answer_present CHECK (
    (selected_option_id IS NOT NULL AND selected_order IS NULL)
    OR (selected_option_id IS NULL AND selected_order IS NOT NULL AND jsonb_typeof(selected_order) = 'array')
);

INSERT INTO questions (prompt, question_type, options, correct_order, explanation, concept_id)
SELECT 'A process is ready, is dispatched, requests a blocking disk read, completes the read, and is dispatched again. Arrange the resulting states from first to last.',
       'SEQUENCE',
       '{"p":"Ready before first dispatch","q":"Running before the disk request","r":"Blocked waiting for the disk read","s":"Ready after the disk read completes","t":"Running after the second dispatch"}'::jsonb,
       '["p","q","r","s","t"]'::jsonb,
       'The process moves from ready to running when dispatched, blocks for the disk read, returns to ready when the read completes, and runs again after the next dispatch.',
       id FROM concepts WHERE name = 'Processes';
