package com.studyqueue.studyqueue.Repository;

import com.studyqueue.studyqueue.Service.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    long countByCorrectTrue();
}
