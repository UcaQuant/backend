package com.example.backend.controller;

import com.example.backend.dto.ExamSessionResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.backend.dto.QuestionPageResponse;

import com.example.backend.dto.AnswerDto;
import jakarta.validation.Valid;


import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping("/start")
    public ResponseEntity<ExamSessionResponse> startExam(@RequestParam("studentId") UUID studentId) {
        ExamSessionResponse response = examService.startExamSession(studentId);
        // Whether new or existing session, 200 OK is fine (or 201 if you prefer only for new)
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "success", false,
                        "message", ex.getMessage()
                ));
    }

    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<QuestionPageResponse> getQuestions(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        QuestionPageResponse response =
                examService.getExamQuestionsPage(sessionId, page, size);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{sessionId}/answers")
    public ResponseEntity<Void> saveAnswers(
            @PathVariable Long sessionId,
            @Valid @RequestBody List<@Valid AnswerDto> answers) {

        examService.saveAnswers(sessionId, answers);
        return ResponseEntity.noContent().build(); // 204 No Content
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", ex.getMessage()));
    }


}
