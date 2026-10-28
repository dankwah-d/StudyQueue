package com.studyqueue.studyqueue.Controller;

import com.studyqueue.studyqueue.Repository.AttemptRepository;
import com.studyqueue.studyqueue.Repository.ConceptAttemptStatsProjection;
import com.studyqueue.studyqueue.ConceptState;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final AttemptRepository repository;

    public StatsController(AttemptRepository repository) {
        this.repository = repository;
    }

    public record Stats(
            long totalAttempts,
            long correctAnswers,
            double accuracyPercent) {}

    public record ConceptStats(
            Long conceptId,
            String conceptName,
            long totalAttempts,
            long correctAnswers,
            double accuracyPercent,
            ConceptState state,
            boolean eligible,
            List<PrerequisiteStats> prerequisites) {}

    public record PrerequisiteStats(
            Long conceptId,
            String conceptName,
            long totalAttempts,
            double accuracyPercent,
            ConceptState state) {}

    @GetMapping
    public Stats getStats() {
        var counts = repository.findGlobalStats();
        long total = counts.getTotalAttempts();
        long correct = counts.getCorrectAnswers();

        return new Stats(total, correct, accuracyPercent(correct, total));
    }

    @GetMapping("/concepts")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<ConceptStats> getConceptStats() {
        List<ConceptAttemptStatsProjection> concepts = repository.findStatsByConcept();
        Map<Long, ConceptAttemptStatsProjection> byId = concepts.stream()
                .collect(Collectors.toMap(ConceptAttemptStatsProjection::getConceptId, stats -> stats));
        Map<Long, List<PrerequisiteStats>> prerequisites = repository.findPrerequisites().stream()
                .collect(Collectors.groupingBy(edge -> edge.getConceptId(),
                        Collectors.mapping(edge -> {
                            ConceptAttemptStatsProjection stats = byId.get(edge.getPrerequisiteId());
                            return new PrerequisiteStats(stats.getConceptId(), stats.getConceptName(),
                                    stats.getTotalAttempts(),
                                    accuracyPercent(stats.getCorrectAnswers(), stats.getTotalAttempts()),
                                    ConceptState.fromAttempts(stats.getTotalAttempts(), stats.getCorrectAnswers()));
                        }, Collectors.toList())));
        return concepts.stream()
                .map(stats -> new ConceptStats(
                        stats.getConceptId(),
                        stats.getConceptName(),
                        stats.getTotalAttempts(),
                        stats.getCorrectAnswers(),
                        accuracyPercent(
                                stats.getCorrectAnswers(),
                                stats.getTotalAttempts()),
                        ConceptState.fromAttempts(stats.getTotalAttempts(), stats.getCorrectAnswers()),
                        stats.getEligible(),
                        prerequisites.getOrDefault(stats.getConceptId(), List.of())))
                .toList();
    }

    private double accuracyPercent(long correct, long total) {
        return total == 0
                ? 0.0
                : Math.round(1000.0 * correct / total) / 10.0;
    }
}
