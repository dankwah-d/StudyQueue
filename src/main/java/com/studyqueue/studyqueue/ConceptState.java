package com.studyqueue.studyqueue;

public enum ConceptState {
    NOT_STARTED,
    PRACTICING,
    MASTERED;

    public static final long MINIMUM_MASTERY_ATTEMPTS = 5;
    public static final double MASTERY_ACCURACY = 0.8;

    public static ConceptState fromAttempts(long total, long correct) {
        if (total == 0) return NOT_STARTED;
        return total >= MINIMUM_MASTERY_ATTEMPTS
                && (double) correct / total >= MASTERY_ACCURACY
                ? MASTERED : PRACTICING;
    }
}
