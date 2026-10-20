package com.studyqueue.studyqueue.Repository;

final class ConceptStatsSql {
    private ConceptStatsSql() {}

    static final String COUNTS = """
            WITH concept_stats AS (
                SELECT c.id, c.name, COUNT(a.id) AS total,
                       COUNT(a.id) FILTER (WHERE a.correct) AS correct
                FROM concepts c
                LEFT JOIN questions q ON q.concept_id = c.id
                LEFT JOIN attempts a ON a.question_id = q.id
                GROUP BY c.id, c.name
            )
            """;

    // The outer query uses c as the candidate concept alias.
    static final String ELIGIBLE = """
            NOT EXISTS (
                SELECT 1 FROM concept_prerequisites p
                JOIN concept_stats prerequisite ON prerequisite.id = p.prerequisite_id
                WHERE p.concept_id = c.id
                  AND (prerequisite.total < :minimumAttempts
                       OR prerequisite.correct::numeric / NULLIF(prerequisite.total, 0) < :masteryAccuracy)
            )
            """;
}
