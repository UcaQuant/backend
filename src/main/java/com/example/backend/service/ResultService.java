package com.example.backend.service;

import com.example.backend.dto.ExamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Thin facade kept for backward compatibility.
 * Delegates to AssessmentService for actual score calculation.
 */
@Service
@RequiredArgsConstructor
public class ResultService {

    private final AssessmentService assessmentService;

    /**
     * Calculates the result for the given session and returns the report download URL.
     */
    public String calculateResult(UUID sessionId) {
        ExamResult result = assessmentService.calculateResult(sessionId);
        return sessionId + "/download";
    }
}
