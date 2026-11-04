package com.studyqueue.studyqueue.Service;

import com.studyqueue.studyqueue.Question;
import com.studyqueue.studyqueue.Repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class TopologicalQuestions {
    private final JdbcTemplate jdbc;
    private final QuestionRepository questions;

    public TopologicalQuestions(JdbcTemplate jdbc, QuestionRepository questions) {
        this.jdbc = jdbc;
        this.questions = questions;
    }

    // Sort the entire graph, including concepts with no questions, before eligibility filtering.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<Question> findEligible() {
        Map<Long, Integer> remaining = new HashMap<>();
        Map<Long, List<Long>> children = new HashMap<>();
        for (Long id : jdbc.queryForList("SELECT id FROM concepts", Long.class)) {
            remaining.put(id, 0);
            children.put(id, new ArrayList<>());
        }
        jdbc.query("SELECT concept_id, prerequisite_id FROM concept_prerequisites", row -> {
            long child = row.getLong("concept_id");
            long parent = row.getLong("prerequisite_id");
            if (!remaining.containsKey(child) || !remaining.containsKey(parent)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A prerequisite concept is missing");
            }
            remaining.compute(child, (id, count) -> count + 1);
            children.get(parent).add(child);
        });
        PriorityQueue<Long> ready = new PriorityQueue<>();
        remaining.forEach((id, count) -> { if (count == 0) ready.add(id); });
        Map<Long, Integer> rank = new HashMap<>();
        while (!ready.isEmpty()) {
            Long id = ready.remove();
            rank.put(id, rank.size());
            for (Long child : children.get(id)) {
                if (remaining.compute(child, (key, count) -> count - 1) == 0) ready.add(child);
            }
        }
        if (rank.size() != remaining.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prerequisite relationships contain a cycle");
        }
        return questions.findEligibleQuestions().stream()
                .sorted(Comparator.comparingInt((Question q) -> q.getConcept() == null
                        ? Integer.MAX_VALUE : rank.get(q.getConcept().getId()))
                        .thenComparing(Question::getId))
                .toList();
    }
}
