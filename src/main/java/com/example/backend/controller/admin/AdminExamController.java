package com.example.backend.controller.admin;

import com.example.backend.domain.Exam;
import com.example.backend.domain.Question;
import com.example.backend.dto.CreateExamDto;
import com.example.backend.dto.QuestionCreationDto;
import com.example.backend.service.ExamAuthoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class AdminExamController {

    private final ExamAuthoringService examAuthoringService;

    @GetMapping("/exams")
    public ResponseEntity<List<Exam>> getAllExams() {
        return ResponseEntity.ok(examAuthoringService.getAllExams());
    }

    @GetMapping("/results")
    public ResponseEntity<List<com.example.backend.dto.TeacherStudentResultDto>> getAllResults() {
        return ResponseEntity.ok(examAuthoringService.getAllStudentResults());
    }

    @PostMapping("/exams")
    public ResponseEntity<Exam> createExam(@Valid @RequestBody CreateExamDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examAuthoringService.createExam(dto));
    }

    @PostMapping("/exams/{examId}/questions")
    public ResponseEntity<Question> addQuestion(
            @PathVariable Long examId,
            @Valid @RequestBody QuestionCreationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examAuthoringService.addQuestion(examId, dto));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<Question> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionCreationDto dto) {
        return ResponseEntity.ok(examAuthoringService.updateQuestion(questionId, dto));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId) {
        examAuthoringService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}
