package com.studyqueue.studyqueue;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;
import java.util.List;

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

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_order", columnDefinition = "jsonb")
    private List<String> correctOrder;

    public String getQuestionType() { return questionType; }
    public List<String> getCorrectOrder() { return correctOrder; }

    @Column(name = "correct_option_id")
    private String correctOptionId;

    @Column(name = "display_option_count")
    private Integer displayOptionCount;

    public Integer getDisplayOptionCount() {
        return displayOptionCount;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private Concept concept;

    public Concept getConcept() {
        return concept;
    }
    
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
