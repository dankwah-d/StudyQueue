package com.studyqueue.studyqueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class QuestionSelectionTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WebApplicationContext context;
    private MockMvc http;

    @BeforeEach
    void isolateSelectionFixtures() {
        // Transaction-local tables shadow real data and disappear on rollback.
        for (String table : new String[]{"concepts", "questions", "attempts", "concept_prerequisites"}) {
            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL) ON COMMIT DROP");
        }
        http = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void returnsNoContentWhenThereAreNoQuestions() throws Exception {
        http.perform(get("/api/questions/next")).andExpect(status().isNoContent());
    }

    @Test
    void prioritizesUnstartedThenWeakestPracticingThenMastered() throws Exception {
        question(null);
        Long first = question(concept("First"));
        Long second = question(concept("Second"));
        concept("No questions");
        expectNext(first);
        attempt(first, false);
        expectNext(second);
        for (int i = 0; i < 5; i++) attempt(second, true);
        expectNext(first);
        for (int i = 0; i < 4; i++) attempt(first, true);
        expectNext(first); // Both mastered; 80% is weaker than 100%.
        Long weak = question(concept("Weak"));
        attempt(weak, false);
        attempt(weak, false);
        expectNext(weak);
        attempt(first, false);
        expectNext(weak); // Both practicing; 0% is weaker than 4/6.
        http.perform(get("/api/questions/next"))
                .andExpect(jsonPath("$.concept.name").value("Weak"))
                .andExpect(jsonPath("$.correctOptionId").doesNotExist());
    }

    @Test
    void breaksTiesByAttemptsThenIdAndFallsBackToUnassignedQuestions() throws Exception {
        Long fallback = question(null);
        expectNext(fallback);
        Long concept = concept("Practice");
        Long first = question(concept);
        Long second = question(concept);
        expectNext(first);
        attempt(first, false);
        expectNext(second);
        attempt(second, false);
        expectNext(first);
        Long lessPracticed = question(concept("Less practiced"));
        attempt(lessPracticed, false);
        expectNext(lessPracticed);
    }

    private Long concept(String name) {
        return jdbc.queryForObject("INSERT INTO concepts(name) VALUES (?) RETURNING id", Long.class, name);
    }

    @Test
    void requiresEveryPrerequisiteAndRelocksWhenMasteryDrops() throws Exception {
        Long dependentConcept = concept("Dependent");
        Long dependent = question(dependentConcept);
        Long firstConcept = concept("Prerequisite A");
        Long first = question(firstConcept);
        Long secondConcept = concept("Prerequisite B");
        Long second = question(secondConcept);
        jdbc.update("INSERT INTO concept_prerequisites VALUES (?, ?), (?, ?)",
                dependentConcept, firstConcept, dependentConcept, secondConcept);
        expectNext(first);
        expectEligibility("Dependent", false);
        expectPrerequisiteStates("NOT_STARTED", "NOT_STARTED");
        http.perform(get("/api/stats/concepts"))
                .andExpect(jsonPath("$[?(@.conceptName == 'Prerequisite A')].prerequisites[*].conceptId").isEmpty())
                .andExpect(jsonPath("$[?(@.conceptName == 'Dependent')].prerequisites[*].conceptName",
                        contains("Prerequisite A", "Prerequisite B")));
        for (int i = 0; i < 4; i++) submit(first, "A");
        submit(first, "B");
        expectNext(second);
        expectEligibility("Dependent", false);
        expectPrerequisiteStates("MASTERED", "NOT_STARTED");
        for (int i = 0; i < 5; i++) submit(second, "A");
        expectEligibility("Dependent", true);
        expectPrerequisiteStates("MASTERED", "MASTERED");
        expectNext(dependent);
        submit(first, "B");
        expectEligibility("Dependent", false);
        expectPrerequisiteStates("PRACTICING", "MASTERED");
        expectNext(first);
    }

    @Test
    void missingPrerequisiteQuestionsLeaveDependentsLockedAndAllowUnassignedFallback() throws Exception {
        Long dependentConcept = concept("Dependent");
        question(dependentConcept);
        Long prerequisite = concept("No questions");
        jdbc.update("INSERT INTO concept_prerequisites VALUES (?, ?)", dependentConcept, prerequisite);
        expectEligibility("Dependent", false);
        expectEligibility("No questions", true);
        http.perform(get("/api/questions/next")).andExpect(status().isNoContent());
        expectNext(question(null));
    }

    private void submit(Long question, String option) throws Exception {
        http.perform(post("/api/questions/{id}/answer", question)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":\"" + option + "\"}"))
                .andExpect(status().isOk());
    }

    private void expectEligibility(String name, boolean eligible) throws Exception {
        http.perform(get("/api/stats/concepts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.conceptName == '" + name + "')].eligible", contains(eligible)));
    }

    private void expectPrerequisiteStates(String first, String second) throws Exception {
        http.perform(get("/api/stats/concepts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.conceptName == 'Dependent')].prerequisites[*].state", contains(first, second)));
    }

    private Long question(Long concept) {
        return jdbc.queryForObject("""
                INSERT INTO questions(prompt, options, correct_option_id, concept_id)
                VALUES ('Selection test', '{"A":"Yes","B":"No"}'::jsonb, 'A', ?) RETURNING id
                """, Long.class, concept);
    }

    private void attempt(Long question, boolean correct) {
        jdbc.update("INSERT INTO attempts(question_id, selected_option_id, correct) VALUES (?, ?, ?)",
                question, correct ? "A" : "B", correct);
    }

    private void expectNext(Long id) throws Exception {
        http.perform(get("/api/questions/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }
}
