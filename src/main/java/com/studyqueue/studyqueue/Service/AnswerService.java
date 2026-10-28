package com.studyqueue.studyqueue.Service;

import com.studyqueue.studyqueue.Controller.QuestionController.AnswerResult;
import com.studyqueue.studyqueue.Question;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class AnswerService {
    private final JdbcTemplate jdbc;

    public AnswerService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public AnswerResult submit(Question question, String optionId, UUID submissionId) {
        // Legacy clients may omit the key; intentional new attempts get distinct keys.
        UUID key = submissionId == null ? UUID.randomUUID() : submissionId;
        jdbc.update("""
                INSERT INTO attempts(question_id, selected_option_id, correct, submission_id,
                                     evaluated_correct_option_id, evaluated_explanation)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (submission_id) DO NOTHING
                """, question.getId(), optionId, question.getCorrectOptionId().equals(optionId),
                key, question.getCorrectOptionId(), question.getExplanation());
        // A separate statement sees the committed winner after a concurrent conflict.
        return jdbc.queryForObject("""
                SELECT question_id, selected_option_id, correct,
                       evaluated_correct_option_id, evaluated_explanation
                FROM attempts WHERE submission_id = ?
                """, (row, index) -> {
            if (row.getLong("question_id") != question.getId()
                    || !optionId.equals(row.getString("selected_option_id"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Submission identifier already used for another answer");
            }
            return new AnswerResult(row.getBoolean("correct"),
                    row.getString("evaluated_correct_option_id"),
                    row.getString("evaluated_explanation"));
        }, key);
    }
}
