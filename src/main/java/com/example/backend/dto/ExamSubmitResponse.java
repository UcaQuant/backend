package com.example.backend.dto;

public record ExamSubmitResponse(
        long answeredCount,
        int totalCount,
        int unansweredCount
) {}

