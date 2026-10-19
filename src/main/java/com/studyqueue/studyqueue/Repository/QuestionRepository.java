package com.studyqueue.studyqueue.Repository;

import com.studyqueue.studyqueue.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Override
    @EntityGraph(attributePaths = "concept")
    List<Question> findAll();
}