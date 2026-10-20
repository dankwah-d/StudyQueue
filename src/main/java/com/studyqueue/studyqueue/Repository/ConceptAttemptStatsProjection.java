package com.studyqueue.studyqueue.Repository;

public interface ConceptAttemptStatsProjection {
    Long getConceptId();

    String getConceptName();

    long getTotalAttempts();

    long getCorrectAnswers();

    boolean getEligible();
}
