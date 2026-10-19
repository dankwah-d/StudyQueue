CREATE TABLE concepts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,

    CONSTRAINT concepts_name_not_blank
        CHECK (length(trim(name)) > 0)
);

INSERT INTO concepts (name)
VALUES ('Processes');

ALTER TABLE questions
    ADD COLUMN concept_id BIGINT REFERENCES concepts(id);

CREATE INDEX questions_concept_id_idx ON questions(concept_id);

UPDATE questions
SET concept_id = (
    SELECT id FROM concepts WHERE name = 'Processes'
)
WHERE prompt = 'Which statement best describes a process?';