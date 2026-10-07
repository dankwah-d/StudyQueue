package com.studyqueue.studyqueue;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> options;

    @Column(name = "correct_option_id", nullable = false)
    private String correctOptionId;

    private String explanation;

    protected Question() {}

    public Long getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public String getCorrectOptionId() {
        return correctOptionId;
    }

    public String getExplanation() {
        return explanation;
    }
}