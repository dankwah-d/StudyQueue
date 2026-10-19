package com.studyqueue.studyqueue.Controller;

import com.studyqueue.studyqueue.Repository.AttemptRepository;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public Stats getStats() {
        long total = repository.count();
        long correct = repository.countByCorrectTrue();
        double accuracy = total == 0
                ? 0.0
                : Math.round(1000.0 * correct / total) / 10.0;

        return new Stats(total, correct, accuracy);
    }
}