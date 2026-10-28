package com.studyqueue.studyqueue.Repository;

import com.studyqueue.studyqueue.Service.Attempt;
import com.studyqueue.studyqueue.ConceptState;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    interface GlobalStats {
        long getTotalAttempts();
        long getCorrectAnswers();
    }

    @Query(value = """
            SELECT COUNT(*) AS "totalAttempts",
                   COUNT(*) FILTER (WHERE correct) AS "correctAnswers"
            FROM attempts
            """, nativeQuery = true)
    GlobalStats findGlobalStats();

    @Query(value = ConceptStatsSql.COUNTS + """
            SELECT c.id AS "conceptId",
                   c.name AS "conceptName",
                   c.total AS "totalAttempts",
                   c.correct AS "correctAnswers",
            """ + ConceptStatsSql.ELIGIBLE + """
                   AS "eligible"
            FROM concept_stats c
            ORDER BY c.name
            """, nativeQuery = true)
    List<ConceptAttemptStatsProjection> selectStatsByConcept(
            @Param("minimumAttempts") long minimumAttempts,
            @Param("masteryAccuracy") double masteryAccuracy);

    default List<ConceptAttemptStatsProjection> findStatsByConcept() {
        return selectStatsByConcept(ConceptState.MINIMUM_MASTERY_ATTEMPTS, ConceptState.MASTERY_ACCURACY);
    }

    @Query(value = """
            SELECT p.concept_id AS "conceptId", p.prerequisite_id AS "prerequisiteId"
            FROM concept_prerequisites p
            JOIN concepts c ON c.id = p.prerequisite_id
            ORDER BY p.concept_id, c.name, c.id
            """, nativeQuery = true)
    List<ConceptPrerequisiteProjection> findPrerequisites();
}
