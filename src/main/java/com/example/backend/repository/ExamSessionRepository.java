package com.example.backend.repository;

import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    List<ExamSession> findByStudentIdAndStatus(UUID studentId, SessionStatus status);
}