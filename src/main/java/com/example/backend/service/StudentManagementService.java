package com.example.backend.service;

import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import com.example.backend.domain.Student;
import com.example.backend.dto.AdminStudentViewDto;
import com.example.backend.dto.DashboardStatsDto;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.ExamSessionRepository;
import com.example.backend.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentManagementService {

    private final StudentRepository studentRepository;
    private final ExamSessionRepository examSessionRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalStudents = studentRepository.count();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long studentsToday = studentRepository.countByCreatedAtAfter(startOfDay);

        long examsCompleted = examSessionRepository
                .findAll()
                .stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();

        return new DashboardStatsDto(totalStudents, studentsToday, examsCompleted);
    }

    @Transactional(readOnly = true)
    public Page<AdminStudentViewDto> getStudentDirectory(Pageable pageable) {
        return studentRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public String getStudentContactInfo(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        return student.getMobileNumber();
    }

    private AdminStudentViewDto toDto(Student student) {
        // Determine exam status from sessions
        List<ExamSession> sessions = examSessionRepository
                .findByStudentIdAndStatus(student.getId(), SessionStatus.COMPLETED);

        String examStatus;
        if (!sessions.isEmpty()) {
            examStatus = "COMPLETED";
        } else {
            List<ExamSession> active = examSessionRepository
                    .findByStudentIdAndStatus(student.getId(), SessionStatus.STARTED);
            examStatus = active.isEmpty() ? "NOT_TAKEN" : "IN_PROGRESS";
        }

        return new AdminStudentViewDto(
                student.getId(),
                student.getFirstname(),
                student.getLastname(),
                student.getCreatedAt(),
                examStatus
        );
    }
}
