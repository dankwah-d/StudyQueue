package com.studyqueue.studyqueue;

import com.studyqueue.studyqueue.Controller.StatsController;
import com.studyqueue.studyqueue.Repository.AttemptRepository;
import com.studyqueue.studyqueue.Repository.ConceptAttemptStatsProjection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsControllerTest {

    @Test
    void reportsGlobalCountsFromOneAggregate() {
        AttemptRepository repository = mock(AttemptRepository.class);
        AttemptRepository.GlobalStats counts = mock(AttemptRepository.GlobalStats.class);
        when(counts.getTotalAttempts()).thenReturn(3L);
        when(counts.getCorrectAnswers()).thenReturn(2L);
        when(repository.findGlobalStats()).thenReturn(counts);
        assertEquals(new StatsController.Stats(3, 2, 66.7),
                new StatsController(repository).getStats());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).count();
    }

    @Test
    void reportsConceptAttemptCountsAndAccuracy() {
        AttemptRepository repository = mock(AttemptRepository.class);
        ConceptAttemptStatsProjection concept = conceptStats(7L, "Processes", 3, 2);
        when(repository.findStatsByConcept()).thenReturn(List.of(concept));

        List<StatsController.ConceptStats> stats =
                new StatsController(repository).getConceptStats();

        assertEquals(
                List.of(new StatsController.ConceptStats(7L, "Processes", 3, 2, 66.7, ConceptState.PRACTICING, true, List.of())),
                stats);
    }

    @Test
    void reportsZeroAccuracyForConceptWithoutAttempts() {
        AttemptRepository repository = mock(AttemptRepository.class);
        ConceptAttemptStatsProjection concept = conceptStats(7L, "Processes", 0, 0);
        when(repository.findStatsByConcept()).thenReturn(List.of(concept));

        List<StatsController.ConceptStats> stats =
                new StatsController(repository).getConceptStats();

        assertEquals(
                List.of(new StatsController.ConceptStats(7L, "Processes", 0, 0, 0.0, ConceptState.NOT_STARTED, true, List.of())),
                stats);
    }

    private ConceptAttemptStatsProjection conceptStats(
            Long id, String name, long total, long correct) {
        ConceptAttemptStatsProjection projection =
                mock(ConceptAttemptStatsProjection.class);
        when(projection.getConceptId()).thenReturn(id);
        when(projection.getConceptName()).thenReturn(name);
        when(projection.getTotalAttempts()).thenReturn(total);
        when(projection.getCorrectAnswers()).thenReturn(correct);
        when(projection.getEligible()).thenReturn(true);
        return projection;
    }
}
