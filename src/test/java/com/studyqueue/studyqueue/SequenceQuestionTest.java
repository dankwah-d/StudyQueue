package com.studyqueue.studyqueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SequenceQuestionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;

    @Test
    void sequenceIsPrivateUntilGradedAndRetriesPreserveTheOriginalResult() throws Exception {
        var http = MockMvcBuilders.webAppContextSetup(context).build();
        long id = originalSequenceId();
        http.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.questionType == 'SEQUENCE')].correctOrder").isEmpty());
        String key = UUID.randomUUID().toString();
        String body = "{\"orderedOptionIds\":[\"p\",\"q\",\"r\",\"s\",\"t\"],\"submissionId\":\"" + key + "\"}";
        long before = jdbc.queryForObject("SELECT count(*) FROM attempts WHERE question_id=?", Long.class, id);
        for (int retry = 0; retry < 2; retry++) {
            http.perform(post("/api/questions/{id}/answer", id).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(true))
                    .andExpect(jsonPath("$.correctOrder[0]").value("p"))
                    .andExpect(jsonPath("$.correctOrder[4]").value("t"));
        }
        assertEquals(before + 1, jdbc.queryForObject("SELECT count(*) FROM attempts WHERE question_id=?", Long.class, id));
        http.perform(post("/api/questions/{id}/answer", id).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("[\"p\",\"q\"", "[\"q\",\"p\"")))
                .andExpect(status().isConflict());
        http.perform(post("/api/questions/{id}/answer", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderedOptionIds\":[\"q\",\"p\",\"r\",\"s\",\"t\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(false));
        assertEquals(before + 2, jdbc.queryForObject("SELECT count(*) FROM attempts WHERE question_id=?", Long.class, id));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM attempts WHERE question_id=? AND correct AND selected_order IS NOT NULL", Long.class, id));
    }

    @Test
    void malformedOrdersAreRejectedWithoutRecordingAttempts() throws Exception {
        var http = MockMvcBuilders.webAppContextSetup(context).build();
        long id = originalSequenceId();
        long before = jdbc.queryForObject("SELECT count(*) FROM attempts", Long.class);
        for (String body : new String[]{"{}", "{\"optionId\":\"p\"}",
                "{\"orderedOptionIds\":[]}", "{\"orderedOptionIds\":[\"p\",\"q\"]}",
                "{\"orderedOptionIds\":[\"p\",\"p\",\"r\",\"s\",\"t\"]}",
                "{\"orderedOptionIds\":[\"p\",\"q\",\"r\",\"s\",\"unknown\"]}",
                "{\"orderedOptionIds\":[\"p\",\"q\",\"r\",\"s\",null]}"}) {
            http.perform(post("/api/questions/{id}/answer", id).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM attempts", Long.class));
    }

    private long originalSequenceId() {
        return jdbc.queryForObject("SELECT min(id) FROM questions WHERE question_type='SEQUENCE'", Long.class);
    }

    @Test
    void everyTopicHasThreeValidSequencesWithExactMatchGrading() throws Exception {
        var http = MockMvcBuilders.webAppContextSetup(context).build();
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        var coverage = jdbc.queryForList("""
                SELECT c.name, count(*) AS total FROM questions q JOIN concepts c ON c.id=q.concept_id
                WHERE q.question_type='SEQUENCE' GROUP BY c.name
                """);
        assertEquals(7, coverage.size());
        for (var topic : coverage) assertEquals(3L, ((Number) topic.get("total")).longValue(), topic.get("name").toString());
        var rows = jdbc.queryForList("""
                SELECT id, correct_order::text AS ordering, explanation,
                    (SELECT count(DISTINCT value) FROM jsonb_each_text(options) WHERE length(trim(value))>0) AS distinct_steps,
                    jsonb_array_length(correct_order) AS step_count
                FROM questions WHERE question_type='SEQUENCE'
                """);
        assertEquals(21, rows.size());
        for (var row : rows) {
            long id = ((Number) row.get("id")).longValue();
            assertEquals(((Number) row.get("step_count")).longValue(), ((Number) row.get("distinct_steps")).longValue());
            assertFalse(row.get("explanation").toString().isBlank());
            var order = new java.util.ArrayList<>(java.util.Arrays.asList(json.readValue(row.get("ordering").toString(), String[].class)));
            http.perform(post("/api/questions/{id}/answer", id).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(java.util.Map.of("orderedOptionIds", order))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(true))
                    .andExpect(jsonPath("$.explanation").value(row.get("explanation")));
            java.util.Collections.swap(order, 0, 1);
            http.perform(post("/api/questions/{id}/answer", id).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(java.util.Map.of("orderedOptionIds", order))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(false));
        }
    }
}
