package com.studyqueue.studyqueue;

import com.studyqueue.studyqueue.Repository.AttemptRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
class StudyqueueApplicationTests {

	@Autowired
	private AttemptRepository attemptRepository;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void contextLoads() {
	}

	@Test
	void loadsAttemptStatisticsGroupedByConcept() {
		Assertions.assertTrue(attemptRepository.findStatsByConcept().stream()
				.anyMatch(stats -> "Processes".equals(stats.getConceptName())));
	}

	@Test
	@Transactional
	void aggregatesAttemptsAcrossQuestionsWithoutMixingConcepts() {
		Long first = jdbc.queryForObject("INSERT INTO concepts(name) VALUES ('Stats test A') RETURNING id", Long.class);
		Long second = jdbc.queryForObject("INSERT INTO concepts(name) VALUES ('Stats test B') RETURNING id", Long.class);
		Long empty = jdbc.queryForObject("INSERT INTO concepts(name) VALUES ('Stats test C') RETURNING id", Long.class);
		Long unanswered = jdbc.queryForObject("INSERT INTO concepts(name) VALUES ('Stats test D') RETURNING id", Long.class);
		Long q1 = insertQuestion(first);
		Long q2 = insertQuestion(first);
		Long q3 = insertQuestion(second);
		insertQuestion(unanswered);
		Long unassigned = insertQuestion(null);
		jdbc.update("INSERT INTO attempts(question_id, selected_option_id, correct) VALUES (?, 'A', TRUE), (?, 'B', FALSE), (?, 'A', TRUE), (?, 'B', FALSE), (?, 'A', TRUE)",
				q1, q1, q2, q3, unassigned);

		var stats = attemptRepository.findStatsByConcept();
		var a = stats.stream().filter(s -> s.getConceptId().equals(first)).findFirst().orElseThrow();
		var b = stats.stream().filter(s -> s.getConceptId().equals(second)).findFirst().orElseThrow();
		Assertions.assertEquals(3, a.getTotalAttempts());
		Assertions.assertEquals(2, a.getCorrectAnswers());
		Assertions.assertEquals(1, b.getTotalAttempts());
		Assertions.assertEquals(0, b.getCorrectAnswers());
		for (Long id : java.util.List.of(empty, unanswered)) {
			var zero = stats.stream().filter(s -> s.getConceptId().equals(id)).findFirst().orElseThrow();
			Assertions.assertEquals(0, zero.getTotalAttempts());
			Assertions.assertEquals(0, zero.getCorrectAnswers());
		}
	}

	private Long insertQuestion(Long conceptId) {
		return jdbc.queryForObject("INSERT INTO questions(prompt, options, correct_option_id, concept_id) VALUES ('Stats test question', '{\"A\":\"Yes\",\"B\":\"No\"}'::jsonb, 'A', ?) RETURNING id", Long.class, conceptId);
	}

}
