package com.studyqueue.studyqueue.Service;

import com.studyqueue.studyqueue.Controller.QuestionController.AnswerResult;
import com.studyqueue.studyqueue.Question;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AnswerService {
    private final JdbcTemplate jdbc;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public AnswerService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public AnswerResult submit(Question question, String optionId, UUID submissionId) {
        return record(question, optionId, null, submissionId);
    }

    @Transactional
    public AnswerResult submitSequence(Question question, List<String> order, UUID submissionId) {
        return record(question, null, List.copyOf(order), submissionId);
    }

    private AnswerResult record(Question question, String optionId, List<String> order, UUID submissionId) {
        // Legacy clients may omit the key; intentional new attempts get distinct keys.
        UUID key = submissionId == null ? UUID.randomUUID() : submissionId;
        jdbc.update("""
                INSERT INTO attempts(question_id, selected_option_id, correct, submission_id,
                                     evaluated_correct_option_id, evaluated_explanation, selected_order, evaluated_correct_order)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
                ON CONFLICT (submission_id) DO NOTHING
                """, question.getId(), optionId, order == null ? question.getCorrectOptionId().equals(optionId)
                        : question.getCorrectOrder().equals(order),
                key, question.getCorrectOptionId(), question.getExplanation(),
                order == null ? null : JSON.writeValueAsString(order),
                question.getCorrectOrder() == null ? null : JSON.writeValueAsString(question.getCorrectOrder()));
        // A separate statement sees the committed winner after a concurrent conflict.
        return jdbc.queryForObject("""
                SELECT question_id, selected_option_id, correct,
                       evaluated_correct_option_id, evaluated_explanation, selected_order, evaluated_correct_order
                FROM attempts WHERE submission_id = ?
                """, (row, index) -> {
            if (row.getLong("question_id") != question.getId()
                    || !java.util.Objects.equals(optionId, row.getString("selected_option_id"))
                    || !java.util.Objects.equals(order, readOrder(row.getString("selected_order")))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Submission identifier already used for another answer");
            }
            return new AnswerResult(row.getBoolean("correct"),
                    row.getString("evaluated_correct_option_id"),
                    row.getString("evaluated_explanation"), readOrder(row.getString("evaluated_correct_order")));
        }, key);
    }

    private static List<String> readOrder(String json) {
        return json == null ? null : java.util.Arrays.asList(JSON.readValue(json, String[].class));
    }
}
