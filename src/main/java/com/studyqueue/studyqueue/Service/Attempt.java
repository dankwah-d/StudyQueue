package com.studyqueue.studyqueue;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option_id", nullable = false, length = 20)
    private String selectedOptionId;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected Attempt() {}

    public Attempt(Long questionId, String selectedOptionId, boolean correct) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.correct = correct;
        this.submittedAt = Instant.now();
    }
}