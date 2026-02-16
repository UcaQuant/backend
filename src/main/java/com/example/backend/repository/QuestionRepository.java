package com.example.backend.repository;

import com.example.backend.domain.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {


    List<Question> findByExamIdOrderById(Long examId);


    long countByExamId(Long examId);


    Page<Question> findByExamId(Long examId, Pageable pageable);
}