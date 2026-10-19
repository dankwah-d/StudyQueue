package com.studyqueue.studyqueue.Controller;

import com.studyqueue.studyqueue.Service.Attempt;
import com.studyqueue.studyqueue.Question;
import com.studyqueue.studyqueue.Repository.AttemptRepository;
import com.studyqueue.studyqueue.Repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository repository;
    private final AttemptRepository attemptRepository;

    public QuestionController(
            QuestionRepository repository,
            AttemptRepository attemptRepository) {
        this.repository = repository;
        this.attemptRepository = attemptRepository;
    }

    public record QuestionView(
            Long id, String prompt, Map<String, String> options) {}

    public record AnswerRequest(String optionId) {}

    public record AnswerResult(
            boolean correct, String correctOptionId, String explanation) {}

    @GetMapping
    public List<QuestionView> getQuestions() {
        return repository.findAll().stream()
                .map(q -> new QuestionView(
                        q.getId(), q.getPrompt(), q.getOptions()))
                .toList();
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

        boolean correct =
                question.getCorrectOptionId().equals(request.optionId());

        attemptRepository.save(
                new Attempt(question.getId(), request.optionId(), correct));

        return new AnswerResult(
                correct,
                question.getCorrectOptionId(),
                question.getExplanation());
    }
}