package com.studyqueue.studyqueue.Controller;

import com.studyqueue.studyqueue.Service.AnswerService;
import com.studyqueue.studyqueue.Service.AnswerOptions;
import java.util.concurrent.ThreadLocalRandom;
import com.studyqueue.studyqueue.Question;
import com.studyqueue.studyqueue.Repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository repository;
    private final AnswerService answerService;

    public QuestionController(
            QuestionRepository repository,
            AnswerService answerService) {
        this.repository = repository;
        this.answerService = answerService;
    }

public record ConceptView(Long id, String name) {}

public record QuestionView(
        Long id,
        String prompt,
        Map<String, String> options,
        ConceptView concept) {}

    public record AnswerRequest(String optionId, UUID submissionId) {}

    public record AnswerResult(
            boolean correct, String correctOptionId, String explanation) {}

@GetMapping
public List<QuestionView> getQuestions() {
    return repository.findAll().stream()
            .map(this::toView)
            .toList();
}

    @GetMapping("/eligible")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<QuestionView> getEligibleQuestions() {
        return repository.findEligibleQuestions().stream().map(this::toView).toList();
    }

    @GetMapping("/next")
    public ResponseEntity<QuestionView> getNextQuestion() {
        return repository.findNextQuestionId()
                .flatMap(repository::findById)
                .map(this::toView)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private QuestionView toView(Question q) {
        return new QuestionView(
                    q.getId(),
                    q.getPrompt(),
                    AnswerOptions.present(q.getOptions(), q.getCorrectOptionId(), q.getDisplayOptionCount(), ThreadLocalRandom.current()),
                    q.getConcept() == null
                            ? null
                            : new ConceptView(
                                    q.getConcept().getId(),
                                    q.getConcept().getName()));
    }

    @PostMapping("/{id}/answer")
    public AnswerResult answer(
            @PathVariable Long id,
            @RequestBody AnswerRequest request) {

        Question question = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Question not found"));

        if (request.optionId() == null
                || !question.getOptions().containsKey(request.optionId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose a valid option");
        }

        return answerService.submit(question, request.optionId(), request.submissionId());
    }
}
