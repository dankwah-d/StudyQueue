CREATE TABLE concept_prerequisites (
    concept_id BIGINT NOT NULL REFERENCES concepts(id),
    prerequisite_id BIGINT NOT NULL REFERENCES concepts(id),
    PRIMARY KEY (concept_id, prerequisite_id),
    CONSTRAINT concept_prerequisites_not_self CHECK (concept_id <> prerequisite_id)
);

CREATE INDEX concept_prerequisites_prerequisite_id_idx
    ON concept_prerequisites(prerequisite_id);

INSERT INTO concept_prerequisites (concept_id, prerequisite_id)
SELECT dependent.id, prerequisite.id
FROM concepts dependent
CROSS JOIN concepts prerequisite
WHERE dependent.name IN ('Threads', 'CPU Scheduling')
  AND prerequisite.name = 'Processes';
