package com.studyqueue.studyqueue;

import com.studyqueue.studyqueue.Controller.QuestionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class QuestionBankTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private QuestionController controller;

    private static final List<String> NEW_CONCEPTS = List.of(
            "Interprocess Communication", "Synchronization", "Deadlock", "Memory Management");
    private static final List<String> ALL_CONCEPTS = List.of(
            "Processes", "Threads", "CPU Scheduling", "Interprocess Communication",
            "Synchronization", "Deadlock", "Memory Management");

    @Test
    void expandedSeedsHaveUsableChoicesExplanationsAndCorrectGrading() {
        assertEquals(133L, jdbc.queryForObject("SELECT count(*) FROM questions", Long.class));
        for (String concept : ALL_CONCEPTS) {
            var rows = jdbc.queryForList("""
                    SELECT q.id, q.correct_option_id, q.explanation,
                           (SELECT count(*) FROM jsonb_each_text(q.options)) AS choice_count,
                           (SELECT count(DISTINCT value) FROM jsonb_each_text(q.options)
                            WHERE length(trim(value)) > 0) AS distinct_choices
                    FROM questions q JOIN concepts c ON c.id = q.concept_id WHERE c.name = ? AND q.question_type = 'MCQ_SINGLE'
                    """, concept);
            assertEquals(16, rows.size(), concept);
            for (var row : rows) {
                assertEquals(4L, ((Number) row.get("choice_count")).longValue());
                assertEquals(4L, ((Number) row.get("distinct_choices")).longValue());
                assertFalse(((String) row.get("explanation")).isBlank());
                long id = ((Number) row.get("id")).longValue();
                String answer = (String) row.get("correct_option_id");
                var correct = controller.answer(id, new QuestionController.AnswerRequest(answer, UUID.randomUUID()));
                assertTrue(correct.correct());
                assertEquals(row.get("explanation"), correct.explanation());
                String wrong = answer.equals("A") ? "B" : "A";
                var incorrect = controller.answer(id, new QuestionController.AnswerRequest(wrong, UUID.randomUUID()));
                assertFalse(incorrect.correct());
                assertEquals(answer, incorrect.correctOptionId());
            }
        }
        assertEquals(0L, jdbc.queryForObject("""
                SELECT count(*) FROM (SELECT prompt FROM questions GROUP BY prompt HAVING count(*) > 1) duplicates
                """, Long.class));
    }

    @Test
    void newConceptsUnlockAlongTheirPrerequisitePaths() {
        // Rollback restores test data; no demo database is used.
        jdbc.update("DELETE FROM attempts");
        assertEquals(Set.of("Processes"), eligibleConcepts());
        master("Processes");
        assertEquals(Set.of("Processes", "Threads", "CPU Scheduling",
                "Interprocess Communication", "Memory Management"), eligibleConcepts());
        master("Threads");
        assertTrue(eligibleConcepts().contains("Synchronization"));
        assertFalse(eligibleConcepts().contains("Deadlock"));
        master("Synchronization");
        assertTrue(eligibleConcepts().containsAll(NEW_CONCEPTS));
    }

    private Set<String> eligibleConcepts() {
        return controller.getEligibleQuestions().stream().map(q -> q.concept().name()).collect(Collectors.toSet());
    }

    private void master(String concept) {
        long id = jdbc.queryForObject("""
                SELECT min(q.id) FROM questions q JOIN concepts c ON c.id = q.concept_id WHERE c.name = ?
                """, Long.class, concept);
        String answer = jdbc.queryForObject("SELECT correct_option_id FROM questions WHERE id = ?", String.class, id);
        for (int i = 0; i < 5; i++) {
            controller.answer(id, new QuestionController.AnswerRequest(answer, UUID.randomUUID()));
        }
    }
}
