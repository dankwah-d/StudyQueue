package com.studyqueue.studyqueue.Repository;

import com.studyqueue.studyqueue.Question;
import com.studyqueue.studyqueue.ConceptState;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Override
    @EntityGraph(attributePaths = "concept")
    List<Question> findAll();

    @Override
    @EntityGraph(attributePaths = "concept")
    Optional<Question> findById(Long id);

    @Query(value = ConceptStatsSql.COUNTS + """
            SELECT q.* FROM questions q
            LEFT JOIN concept_stats c ON c.id = q.concept_id
            WHERE
            """ + ConceptStatsSql.ELIGIBLE + " ORDER BY q.id", nativeQuery = true)
    List<Question> selectEligibleQuestions(@Param("minimumAttempts") long minimumAttempts,
                                         @Param("masteryAccuracy") double masteryAccuracy);

    default List<Question> findEligibleQuestions() {
        return selectEligibleQuestions(ConceptState.MINIMUM_MASTERY_ATTEMPTS, ConceptState.MASTERY_ACCURACY);
    }

    @Query(value = ConceptStatsSql.COUNTS + """
            , question_stats AS (
                SELECT question_id, COUNT(*) AS total FROM attempts GROUP BY question_id
            )
            SELECT q.id FROM questions q
            LEFT JOIN concept_stats c ON c.id = q.concept_id
            LEFT JOIN question_stats a ON a.question_id = q.id
            WHERE
            """ + ConceptStatsSql.ELIGIBLE + """
            ORDER BY CASE
                WHEN c.id IS NULL THEN 3
                WHEN c.total = 0 THEN 0
                WHEN c.total >= :minimumAttempts AND c.correct::numeric / c.total >= :masteryAccuracy THEN 2
                ELSE 1 END,
                COALESCE(c.correct::numeric / NULLIF(c.total, 0), 0),
                COALESCE(c.total, 0), COALESCE(a.total, 0), q.id
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> selectNextQuestionId(@Param("minimumAttempts") long minimumAttempts,
                                      @Param("masteryAccuracy") double masteryAccuracy);

    default Optional<Long> findNextQuestionId() {
        return selectNextQuestionId(ConceptState.MINIMUM_MASTERY_ATTEMPTS, ConceptState.MASTERY_ACCURACY);
    }
}
