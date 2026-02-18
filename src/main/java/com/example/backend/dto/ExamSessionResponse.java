package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ExamSessionResponse {
    private UUID sessionId;
    private int durationSeconds;
    private LocalDateTime startTime;
}
