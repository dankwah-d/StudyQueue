package com.studyqueue.studyqueue.Repository;

import com.studyqueue.studyqueue.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}