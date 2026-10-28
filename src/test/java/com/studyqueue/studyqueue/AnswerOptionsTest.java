package com.studyqueue.studyqueue;

import com.studyqueue.studyqueue.Service.AnswerOptions;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class AnswerOptionsTest {
    @Test
    void largerPoolsVaryDistractorsAlwaysIncludeCorrectAndPreserveIds() {
        Map<String, String> pool = new LinkedHashMap<>();
        for (int i = 0; i < 9; i++) pool.put("choice-" + i, "Answer " + i);
        var presentations = new HashSet<Map<String, String>>();
        var orders = new HashSet<java.util.List<String>>();
        for (int seed = 0; seed < 40; seed++) {
            var choices = AnswerOptions.present(pool, "choice-7", 4, new Random(seed));
            assertEquals(4, choices.size());
            assertEquals("Answer 7", choices.get("choice-7"));
            choices.forEach((id, text) -> assertEquals(pool.get(id), text));
            presentations.add(choices);
            orders.add(java.util.List.copyOf(choices.keySet()));
        }
        assertTrue(presentations.size() > 1);
        assertTrue(orders.size() > 1);
        assertEquals(9, pool.size());
    }

    @Test
    void configuredCountsSupportMoreThanFourAndClampToPoolSize() {
        var pool = Map.of("A", "one", "B", "two", "C", "three", "D", "four", "E", "five", "F", "six");
        assertEquals(5, AnswerOptions.present(pool, "F", 5, new Random(2)).size());
        assertEquals(pool, AnswerOptions.present(pool, "F", 10, new Random(2)));
        assertTrue(AnswerOptions.present(pool, "F", 2, new Random(2)).containsKey("F"));
    }

    @Test
    void existingSmallQuestionsRetainEveryOption() {
        for (int size = 1; size <= 7; size++) {
            Map<String, String> pool = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) pool.put("" + i, "Answer " + i);
            assertEquals(pool, AnswerOptions.present(pool, "0", null, new Random(5)));
        }
    }
}
