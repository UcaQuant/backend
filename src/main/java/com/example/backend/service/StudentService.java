package com.example.backend.service;

import com.example.backend.domain.Student;
import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import com.example.backend.dto.ExamResult;
import com.example.backend.dto.StudentExamHistoryDto;
import com.example.backend.dto.StudentRegistrationRequest;
import com.example.backend.dto.StudentRegistrationResponse;
import com.example.backend.exception.DuplicateMobileException;
import com.example.backend.repository.ExamSessionRepository;
import com.example.backend.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final AssessmentService assessmentService;

    @Transactional
    public StudentRegistrationResponse registerStudent(StudentRegistrationRequest request) {
        // Duplicate mobile check
        studentRepository.findStudentByMobileNumber(request.getMobileNumber())
                .ifPresent(existing -> {
                    throw new DuplicateMobileException("Student with this mobile number already exists");
                });

        Student student = new Student();
        student.setFirstname(request.getFirstName());
        student.setLastname(request.getLastName());
        student.setMobileNumber(request.getMobileNumber());
        student.setPassword(request.getPassword()); // Store password simply

        Student saved = studentRepository.save(student);

        StudentRegistrationResponse.DataPayload payload = new StudentRegistrationResponse.DataPayload(
                saved.getId().toString(),
                "/instructions");

        return new StudentRegistrationResponse(true, payload);
    }

    public StudentRegistrationResponse login(com.example.backend.dto.StudentLoginRequest request) {
        Student student = studentRepository.findStudentByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!student.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        StudentRegistrationResponse.DataPayload payload = new StudentRegistrationResponse.DataPayload(
                student.getId().toString(),
                "/dashboard");

        return new StudentRegistrationResponse(true, payload);
    }

    public List<StudentExamHistoryDto> getExamHistory(UUID studentId) {
        return examSessionRepository.findByStudentIdAndStatus(studentId, SessionStatus.COMPLETED).stream()
                .map(session -> {
                    ExamResult result = assessmentService.calculateResult(session.getId());
                    return new StudentExamHistoryDto(
                            session.getExam().getTitle(),
                            result.getTotalCorrect(),
                            result.getTotalQuestions(),
                            session.getSubmitTime() != null ? session.getSubmitTime() : LocalDateTime.now(),
                            "/api/v1/reports/" + session.getId() + "/download");
                })
                .collect(Collectors.toList());
    }
}
