package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping("/start")
    public ResponseEntity<ExamSessionResponse> startExam(@RequestParam("studentId") UUID studentId) {
        return ResponseEntity.ok(examService.startExamSession(studentId));
    }

    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<QuestionPageResponse> getQuestions(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(examService.getExamQuestionsPage(sessionId, page, size));
    }

    @PutMapping("/{sessionId}/answers")
    public ResponseEntity<Void> saveAnswers(
            @PathVariable UUID sessionId,
            @Valid @RequestBody List<@Valid AnswerDto> answers) {
        examService.saveAnswers(sessionId, answers);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<ExamSubmitResponse> submitExam(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(examService.submitExam(sessionId));
    }

    @PostMapping("/{sessionId}/finish")
    public ResponseEntity<ExamFinishResponse> finishExam(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(examService.finishExam(sessionId));
    }
}
