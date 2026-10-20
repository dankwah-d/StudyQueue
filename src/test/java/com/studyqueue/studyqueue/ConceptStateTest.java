package com.studyqueue.studyqueue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConceptStateTest {
    @ParameterizedTest
    @CsvSource({
            "0, 0, NOT_STARTED",
            "1, 0, PRACTICING",
            "4, 4, PRACTICING",
            "5, 3, PRACTICING",
            "5, 4, MASTERED",
            "5, 5, MASTERED",
            "6, 4, PRACTICING",
            "2500, 1999, PRACTICING"
    })
    void derivesStateFromUnroundedAttemptCounts(long total, long correct, ConceptState expected) {
        assertEquals(expected, ConceptState.fromAttempts(total, correct));
    }
}
