package com.example.backend.service;

import com.example.backend.domain.Exam;
import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import com.example.backend.domain.Student;
import com.example.backend.dto.ExamSessionResponse;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.ExamRepository;
import com.example.backend.repository.ExamSessionRepository;
import com.example.backend.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final StudentRepository studentRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;

    @Transactional
    public ExamSessionResponse startExamSession(UUID studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        // Find active session for this student
        var activeSessions = examSessionRepository.findByStudentIdAndStatus(studentId, SessionStatus.STARTED);
        ExamSession activeSession = activeSessions.isEmpty() ? null : activeSessions.get(0);

        if (activeSession != null) {
            return toResponse(activeSession);
        }

        // For now: pick first available exam (could be improved later)
        // TODO: Replace with real exam selection logic when exam creation is implemented
        Exam exam = examRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No active exam available"));

        ExamSession session = new ExamSession();
        session.setExam(exam);
        session.setStudent(student);
        session.setStatus(SessionStatus.STARTED);
        session.setStartTime(LocalDateTime.now());

        ExamSession saved = examSessionRepository.save(session);

        return toResponse(saved);
    }

    private ExamSessionResponse toResponse(ExamSession session) {
        return new ExamSessionResponse(
                session.getId(),
                session.getExam().getTimeLimitSeconds(),
                session.getStartTime()
        );
    }
}
