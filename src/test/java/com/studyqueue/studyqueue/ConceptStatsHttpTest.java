package com.studyqueue.studyqueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ConceptStatsHttpTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Transactional
    void savedAnswersUpdateConceptStatisticsThroughHttp() throws Exception {
        Long conceptId = jdbc.queryForObject(
                "INSERT INTO concepts(name) VALUES ('HTTP stats test') RETURNING id", Long.class);
        Long questionId = jdbc.queryForObject("""
                INSERT INTO questions(prompt, options, correct_option_id, concept_id)
                VALUES ('HTTP stats question', '{"A":"Yes","B":"No"}'::jsonb, 'A', ?)
                RETURNING id
                """, Long.class, conceptId);
        MockMvc http = MockMvcBuilders.webAppContextSetup(context).build();
        String concept = "$[?(@.conceptName == 'HTTP stats test')]";

        http.perform(get("/api/stats/concepts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(concept + ".state", contains("NOT_STARTED")))
                .andExpect(jsonPath(concept + ".totalAttempts", contains(0)))
                .andExpect(jsonPath(concept + ".accuracyPercent", contains(0.0)));

        http.perform(post("/api/questions/{id}/answer", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));
        http.perform(post("/api/questions/{id}/answer", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":\"B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));

        for (int read = 0; read < 2; read++) {
            http.perform(get("/api/stats/concepts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(concept + ".totalAttempts", contains(2)))
                    .andExpect(jsonPath(concept + ".correctAnswers", contains(1)))
                    .andExpect(jsonPath(concept + ".accuracyPercent", contains(50.0)));
        }

        for (int answer = 0; answer < 3; answer++) {
            http.perform(post("/api/questions/{id}/answer", questionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionId\":\"A\"}"))
                    .andExpect(status().isOk());
        }
        http.perform(get("/api/stats/concepts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(concept + ".state", contains("MASTERED")));
        http.perform(post("/api/questions/{id}/answer", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":\"B\"}"))
                .andExpect(status().isOk());
        http.perform(get("/api/stats/concepts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(concept + ".state", contains("PRACTICING")));
    }
}
