package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ExamSessionResponse {
    private Long sessionId;
    private int durationSeconds;
    private LocalDateTime startTime;
}
